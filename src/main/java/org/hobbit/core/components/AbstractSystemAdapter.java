/**
 * This file is part of core.
 *
 * core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with core.  If not, see <http://www.gnu.org/licenses/>.
 */
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
import org.hobbit.core.rabbit.DataSender;
import org.hobbit.core.rabbit.DataSenderImpl;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * This abstract class implements basic functions that can be used to implement
 * a system adapter.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractSystemAdapter extends AbstractPlatformConnectorComponent
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
     * The cause for an unusual termination.
     */
    private Exception cause;
    /**
     * Mutex used to manage access to the {@link #cause} object.
     */
    private Semaphore causeMutex = new Semaphore(1);
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
     * The maximum number of incoming messages of a single queue that are
     * processed in parallel. Additional messages have to wait.
     */
    private final int maxParallelProcessedMsgs;
    /**
     * Queue from the data generator to this evaluation storage.
     */
    protected RabbitQueue dataGen2SystemQueue;
    /**
     * Queue from the task generator to this evaluation storage.
     */
    protected RabbitQueue taskGen2SystemQueue;
    /**
     * Sender for sending messages from the benchmarked system to the evaluation
     * storage.
     */
    protected DataSender sender2EvalStore;
    /**
     * The RDF model containing the system parameters.
     */
    protected Model systemParamModel;

    /**
     * Constructor using the {@link #DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES}=
     * {@value #DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES}.
     */
    public AbstractSystemAdapter() {
        this(DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES);
    }

    /**
     * Constructor setting the maximum number of messages processed in parallel.
     *
     * @param maxParallelProcessedMsgs
     *            The maximum number of incoming messages of a single queue that
     *            are processed in parallel. Additional messages have to wait.
     */
    public AbstractSystemAdapter(int maxParallelProcessedMsgs) {
        this.maxParallelProcessedMsgs = maxParallelProcessedMsgs;
        defaultContainerType = Constants.CONTAINER_TYPE_SYSTEM;
    }

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

        dataGen2SystemQueue = getFactoryForIncomingDataQueues()
                .createDefaultRabbitQueue(generateSessionQueueName(Constants.DATA_GEN_2_SYSTEM_QUEUE_NAME));
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

        taskGen2SystemQueue = getFactoryForIncomingDataQueues()
                .createDefaultRabbitQueue(generateSessionQueueName(Constants.TASK_GEN_2_SYSTEM_QUEUE_NAME));
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

        sender2EvalStore = DataSenderImpl.builder().queue(getFactoryForOutgoingDataQueues(),
                generateSessionQueueName(Constants.SYSTEM_2_EVAL_STORAGE_QUEUE_NAME)).build();

    }

    @Override
    public void run() throws Exception {
        sendToCmdQueue(Commands.SYSTEM_READY_SIGNAL);

        terminateMutex.acquire();
        // Check whether the system should abort
        try {
            causeMutex.acquire();
            if (cause != null) {
                throw cause;
            }
            causeMutex.release();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting to set the termination cause.");
        }
        // wait until all messages have been read from the queue and all sent
        // messages have been consumed
        while ((taskGen2SystemQueue.messageCount() + dataGen2SystemQueue.messageCount()) > 0) {
            Thread.sleep(1000);
            // Check whether the system should abort
            try {
                causeMutex.acquire();
                if (cause != null) {
                    throw cause;
                }
                causeMutex.release();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting to set the termination cause.");
            }
        }
        // Collect all open mutex counts to make sure that there is no message
        // that is still processed
        Thread.sleep(1000);
        currentlyProcessedMessages.acquire(maxParallelProcessedMsgs);
        currentlyProcessedTasks.acquire(maxParallelProcessedMsgs);
        sender2EvalStore.closeWhenFinished();
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // If this is the signal to start the data generation
        if (command == Commands.TASK_GENERATION_FINISHED) {
            terminate(null);
        }
        super.receiveCommand(command, data);
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
        sender2EvalStore.sendData(buffer.array());
    }

    /**
     * Starts termination of the main thread of this system adapter. If a cause
     * is given, it will be thrown causing an abortion from the main thread
     * instead of a normal termination.
     * 
     * @param cause
     *            the cause for an abortion of the process or {code null} if the
     *            component should terminate in a normal way.
     */
    protected synchronized void terminate(Exception cause) {
        if (cause != null) {
            try {
                causeMutex.acquire();
                this.cause = cause;
                causeMutex.release();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting to set the termination cause.");
            }
        }
        terminateMutex.release();
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(dataGen2SystemQueue);
        IOUtils.closeQuietly(taskGen2SystemQueue);
        IOUtils.closeQuietly(sender2EvalStore);
        super.close();
    }
}
