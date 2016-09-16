package org.hobbit.core.components;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.Charsets;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.rabbit.RabbitMQUtils;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;

/**
 * This abstract class implements basic functions that can be used to implement
 * a system adapter.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractSystemAdapter extends AbstractCommandReceivingComponent
        implements GeneratedDataReceivingComponent, TaskReceivingComponent {

    // private static final Logger LOGGER =
    // LoggerFactory.getLogger(AbstractSystemAdapter.class);

    /**
     * Default value of the {@link #maxParallelProcessedMsgs} attribute.
     */
    private static final int DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES = 100;

    /**
     * Mutex used to wait for the terminate signal.
     */
    private Semaphore terminateMutex = new Semaphore(0);
    /**
     * Semaphore used to control the number of messages that can be processed in
     * parallel.
     */
    private Semaphore currentlyProcessedMessages;
    /**
     * The maximum number of incoming messages that are processed in parallel.
     * Additional messages have to wait.
     */
    private int maxParallelProcessedMsgs = DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES;
    /**
     * Name of the incoming queue with which the system can receive data from
     * the data generators.
     */
    protected String dataGen2SystemQueueName;
    /**
     * The Channel of the incoming queue with which the system can receive data
     * from the data generators.
     */
    protected Channel dataGen2System;
    /**
     * Name of the queue from the task generator.
     */
    protected String taskGen2SystemQueueName;
    /**
     * Channel of the queue from the task generator.
     */
    protected Channel taskGen2System;
    /**
     * Name of the queue to the evaluation storage.
     */
    protected String system2EvalStoreQueueName;
    /**
     * Channel of the queue to the evaluation storage.
     */
    protected Channel system2EvalStore;

    @Override
    public void init() throws Exception {
        super.init();

        @SuppressWarnings("resource")
        AbstractSystemAdapter receiver = this;

        dataGen2SystemQueueName = generateSessionQueueName(Constants.DATA_GEN_2_SYSTEM_QUEUE_NAME);
        dataGen2System = connection.createChannel();
        dataGen2System.queueDeclare(dataGen2SystemQueueName, false, false, true, null);
        dataGen2System.basicConsume(dataGen2SystemQueueName, true, new DefaultConsumer(dataGen2System) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
                    throws IOException {
                receiver.receiveGeneratedData(body);
            }
        });

        taskGen2SystemQueueName = generateSessionQueueName(Constants.TASK_GEN_2_SYSTEM_QUEUE_NAME);
        taskGen2System = connection.createChannel();
        taskGen2System.queueDeclare(taskGen2SystemQueueName, false, false, true, null);
        taskGen2System.basicConsume(taskGen2SystemQueueName, true, new DefaultConsumer(taskGen2System) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
                    throws IOException {
                ByteBuffer buffer = ByteBuffer.wrap(body);
                String taskId = RabbitMQUtils.readString(buffer);
                byte[] data = RabbitMQUtils.readByteArray(buffer);
                try {
                    currentlyProcessedMessages.acquire();
                } catch (InterruptedException e) {
                    throw new IOException("Interrupted while waiting for mutex.", e);
                }
                receiver.receiveGeneratedTask(taskId, data);
                currentlyProcessedMessages.release();
            }
        });

        system2EvalStoreQueueName = generateSessionQueueName(Constants.SYSTEM_2_EVAL_STORAGE_QUEUE_NAME);
        system2EvalStore = connection.createChannel();
        system2EvalStore.queueDeclare(system2EvalStoreQueueName, false, false, true, null);

        currentlyProcessedMessages = new Semaphore(maxParallelProcessedMsgs);
    }

    @Override
    public void run() throws Exception {
        sendToCmdQueue(Commands.SYSTEM_READY_SIGNAL);

        terminateMutex.acquire();
        // wait until all messages have been read from the queue
        while (taskGen2System.messageCount(taskGen2SystemQueueName) > 0) {
            Thread.sleep(1000);
        }
        // Collect all open mutex counts to make sure that there is no message
        // that is still processed
        Thread.sleep(1000);
        currentlyProcessedMessages.acquire(maxParallelProcessedMsgs);
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // If this is the signal to start the data generation
        if (command == Commands.TASK_GENERATION_FINISHED) {
            // release the mutex
            terminateMutex.release();
        }
    }

    /**
     * This method sends the given result data for the task with the given task
     * id to the evaluation storage.
     * 
     * @param taskIdString
     *            the id of the task
     * @param data
     *            the data of the task
     * @throws IOException
     *             if there is an error during the sending
     */
    protected void sendResultToEvalStorage(String taskIdString, byte[] data) throws IOException {
        byte[] taskIdBytes = taskIdString.getBytes(Charsets.UTF_8);
        // + 4 for taskIdBytes.length
        // + 4 for data.length
        int capacity = 4 + 4 + taskIdBytes.length + data.length;
        ByteBuffer buffer = ByteBuffer.allocate(capacity);
        buffer.putInt(taskIdBytes.length);
        buffer.put(taskIdBytes);
        buffer.putInt(data.length);
        buffer.put(data);
        system2EvalStore.basicPublish("", system2EvalStoreQueueName, MessageProperties.PERSISTENT_BASIC,
                buffer.array());
    }

    @Override
    public void close() throws IOException {
        if (dataGen2System != null) {
            try {
                dataGen2System.close();
            } catch (Exception e) {
            }
        }
        if (taskGen2System != null) {
            try {
                taskGen2System.close();
            } catch (Exception e) {
            }
        }
        if (system2EvalStore != null) {
            try {
                system2EvalStore.close();
            } catch (Exception e) {
            }
        }
        super.close();
    }
}
