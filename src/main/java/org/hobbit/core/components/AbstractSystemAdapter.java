package org.hobbit.core.components;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSystemAdapter.class);

    /**
     * Default value of the {@link #maxParallelProcessedMsgs} attribute.
     */
    private static final int DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES = 100;

    /**
     * Mutex used to wait for the terminate signal.
     */
    private Semaphore terminateMutex = new Semaphore(0);
    /**
     * Semaphore used to control the number of data messages that can be
     * processed in parallel.
     */
    private Semaphore currentlyProcessedMessages;
    /**
     * Semaphore used to control the number of tasks that can be processed in
     * parallel.
     */
    private Semaphore currentlyProcessedTasks;
    /**
     * The maximum number of incoming messages that are processed in parallel.
     * Additional messages have to wait.
     */
    private int maxParallelProcessedMsgs = DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES;
    /**
     * Queue from the data generator to this evaluation storage.
     */
    protected RabbitQueue dataGen2SystemQueue;
    /**
     * Queue from the task generator to this evaluation storage.
     */
    protected RabbitQueue taskGen2SystemQueue;
    /**
     * Queue from the benchmarked system to this evaluation storage.
     */
    protected RabbitQueue system2EvalStoreQueue;
    /**
     * The RDF model containing the system parameters.
     */
    protected Model systemParamModel;

    @Override
    public void init() throws Exception {
        super.init();

        currentlyProcessedMessages = new Semaphore(maxParallelProcessedMsgs);
        currentlyProcessedTasks = new Semaphore(maxParallelProcessedMsgs);

        Map<String, String> env = System.getenv();
        // Get the benchmark parameter model
        if (env.containsKey(Constants.SYSTEM_PARAMETERS_MODEL_KEY)) {
            try {
                systemParamModel = RabbitMQUtils.readModel(env.get(Constants.SYSTEM_PARAMETERS_MODEL_KEY));
            } catch (Exception e) {
                LOGGER.warn("Couldn't deserialize the given parameter model. The parameter model will be empty.", e);
                systemParamModel = ModelFactory.createDefaultModel();
            }
        } else {
            LOGGER.warn("Couldn't get the expected parameter model from the variable "
                    + Constants.SYSTEM_PARAMETERS_MODEL_KEY + ". The parameter model will be empty.");
            systemParamModel = ModelFactory.createDefaultModel();
        }

        @SuppressWarnings("resource")
        AbstractSystemAdapter receiver = this;

        dataGen2SystemQueue = createDefaultRabbitQueue(
                generateSessionQueueName(Constants.DATA_GEN_2_SYSTEM_QUEUE_NAME));
        dataGen2SystemQueue.channel.basicConsume(dataGen2SystemQueue.name, false,
                new DefaultConsumer(dataGen2SystemQueue.channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties,
                            byte[] body) throws IOException {
                        try {
                            currentlyProcessedMessages.acquire();
                        } catch (InterruptedException e) {
                            throw new IOException("Interrupted while waiting for mutex.", e);
                        }
                        try {
                            receiver.receiveGeneratedData(body);
                            dataGen2SystemQueue.channel.basicAck(envelope.getDeliveryTag(), false);
                        } finally {
                            currentlyProcessedMessages.release();
                        }
                    }
                });
        dataGen2SystemQueue.channel.basicQos(maxParallelProcessedMsgs);

        taskGen2SystemQueue = createDefaultRabbitQueue(
                generateSessionQueueName(Constants.TASK_GEN_2_SYSTEM_QUEUE_NAME));
        taskGen2SystemQueue.channel.basicConsume(taskGen2SystemQueue.name, false,
                new DefaultConsumer(taskGen2SystemQueue.channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties,
                            byte[] body) throws IOException {
                        ByteBuffer buffer = ByteBuffer.wrap(body);
                        String taskId = RabbitMQUtils.readString(buffer);
                        byte[] data = RabbitMQUtils.readByteArray(buffer);
                        try {
                            currentlyProcessedTasks.acquire();
                        } catch (InterruptedException e) {
                            throw new IOException("Interrupted while waiting for mutex.", e);
                        }
                        try {
                            receiver.receiveGeneratedTask(taskId, data);
                            taskGen2SystemQueue.channel.basicAck(envelope.getDeliveryTag(), false);
                        } finally {
                            currentlyProcessedTasks.release();
                        }
                    }
                });
        taskGen2SystemQueue.channel.basicQos(maxParallelProcessedMsgs);

        system2EvalStoreQueue = createDefaultRabbitQueue(
                generateSessionQueueName(Constants.SYSTEM_2_EVAL_STORAGE_QUEUE_NAME));
    }

    @Override
    public void run() throws Exception {
        sendToCmdQueue(Commands.SYSTEM_READY_SIGNAL);

        terminateMutex.acquire();
        // wait until all messages have been read from the queue
        while ((taskGen2SystemQueue.channel.messageCount(taskGen2SystemQueue.name)
                + dataGen2SystemQueue.channel.messageCount(dataGen2SystemQueue.name)) > 0) {
            Thread.sleep(1000);
        }
        // Collect all open mutex counts to make sure that there is no message
        // that is still processed
        Thread.sleep(1000);
        currentlyProcessedMessages.acquire(maxParallelProcessedMsgs);
        currentlyProcessedTasks.acquire(maxParallelProcessedMsgs);
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
        // system2EvalStore
        // .basicPublish("", system2EvalStoreQueueName,
        // MessageProperties.PERSISTENT_BASIC, buffer.array());
        system2EvalStoreQueue.channel.basicPublish("", system2EvalStoreQueue.name, MessageProperties.PERSISTENT_BASIC,
                buffer.array());
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(dataGen2SystemQueue);
        IOUtils.closeQuietly(taskGen2SystemQueue);
        IOUtils.closeQuietly(system2EvalStoreQueue);
        super.close();
    }
}
