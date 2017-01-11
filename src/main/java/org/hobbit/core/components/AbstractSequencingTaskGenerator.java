package org.hobbit.core.components;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hobbit.core.Constants;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * Extension of the {@link AbstractTaskGenerator} that offers methods to wait
 * for acknowledgements for single tasks. The workflow for waiting for a task is
 * <ul>
 * <li>generate a task and a task id (for the latter {@link #getNextTaskId()}
 * should be used)</li>
 * <li>set the task id which will be sent next using
 * {@link #setTaskIdToWaitFor(String)}</li>
 * <li>send the task to the system with
 * {@link #sendTaskToSystemAdapter(String, byte[])} and
 * {@link #sendTaskToEvalStorage(String, long, byte[])}</li>
 * <li>wait for the acknowledgement using {@link #waitForAck()}</li>
 * </ul>
 * 
 * <p>
 * <b>Note</b> that the acknowledgements only work if the evaluation storage is
 * configured to send acknowledgements for the single tasks.
 * </p>
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractSequencingTaskGenerator extends AbstractTaskGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSequencingTaskGenerator.class);
    /**
     * Default timeout for the acknowledgement.
     */
    private static final long DEFAULT_ACK_TIMEOUT = 600000;
    /**
     * Timeout for the acknowledgement.
     */
    private long ackTimeout = DEFAULT_ACK_TIMEOUT;
    /**
     * Id of the task the generator is waiting for an acknowledgement.
     */
    private String taskId = null;
    /**
     * Semaphore used to wait for the acknowledgement.
     */
    private Semaphore taskIdMutex = new Semaphore(0);
    /**
     * Channel on which the acknowledgements are received.
     */
    protected Channel ackChannel;

    @Override
    public void init() throws Exception {
        super.init();
        // Create channel for incoming acknowledgements
        ackChannel = connection.createChannel();
        String queueName = ackChannel.queueDeclare().getQueue();
        ackChannel.exchangeDeclare(generateSessionQueueName(Constants.HOBBIT_ACK_EXCHANGE_NAME), "fanout", false, true,
                null);
        ackChannel.queueBind(queueName, generateSessionQueueName(Constants.HOBBIT_ACK_EXCHANGE_NAME), "");
        Consumer consumer = new DefaultConsumer(ackChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                try {
                    handleAck(body);
                } catch (Exception e) {
                    LOGGER.error("Exception while trying to handle incoming command.", e);
                }
            }
        };
        ackChannel.basicConsume(queueName, true, consumer);
    }

    /**
     * Handles the acknowledgement messages.
     * 
     * @param body
     *            the body of the acknowledgement message
     */
    protected void handleAck(byte[] body) {
        String ackTaskId = RabbitMQUtils.readString(body);
        if ((taskId != null) && (taskId.equals(ackTaskId))) {
            taskId = null;
            taskIdMutex.release();
        }
    }

    /**
     * Method to set the task id for which the task generator will wait when
     * calling {@link #waitForAck()}.
     * 
     * @param taskId
     *            id of the task for which the acknowledgement should be
     *            received
     */
    protected void setTaskIdToWaitFor(String taskId) {
        this.taskId = taskId;
    }

    /**
     * Method to wait for the acknowledgement of a task with the given Id.
     * 
     * @return <code>true</code> if the acknowledgement has been received,
     *         <code>false</code> if the timeout has been reached or the method
     *         has been interrupted.
     */
    protected boolean waitForAck() {
        boolean ack = false;
        try {
            ack = taskIdMutex.tryAcquire(ackTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.info("Interrupted while waiting for acknowledgement.", e);
        }
        return ack;
    }

    @Override
    public void close() throws IOException {
        try {
            ackChannel.close();
        } catch (TimeoutException e) {
        }
        super.close();
    }
}
