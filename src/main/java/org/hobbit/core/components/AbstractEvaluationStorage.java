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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.data.ResultPair;
import org.hobbit.core.rabbit.DataReceiver;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.rabbit.IncomingStreamHandler;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.paired.PairedDataSender;
import org.hobbit.core.utils.SteppingIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * This abstract class implements basic functions that can be used to implement
 * an evaluation storage.
 * 
 * 
 * Incoming messages should have the structure:<br>
 * {@code timestamp length id data}<br>
 * where
 * <ul>
 * <li>{@code timestamp} is a {@code long} value (only expected if the message
 * is received from a task generator)</li>
 * <li>{@code length} is an {@code int} value containing the length of the
 * following id string</li>
 * <li>{@code id} is a string with the given {@code length}</li>
 * <li>{@code data} is the remaining bytes that are received</li>
 * </ul>
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractEvaluationStorage extends AbstractPlatformConnectorComponent
        implements ResponseReceivingComponent, ExpectedResponseReceivingComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEvaluationStorage.class);

    public static final int ITERATOR_ID_STREAM_ID = 0;
    public static final int EXPECTED_RESPONSE_STREAM_ID = 1;
    public static final int RECEIVED_RESPONSE_STREAM_ID = 2;
    public static final String RECEIVE_TIMESTAMP_FOR_SYSTEM_RESULTS_KEY = "HOBBIT_RECEIVE_TIMESTAMP_FOR_SYSTEM_RESULTS";

    /**
     * If a request contains this iterator ID, a new iterator is created and its
     * first result as well as its Id are returned.
     */
    public static final byte NEW_ITERATOR_ID = -1;
    /**
     * The empty response that is sent if an error occurs.
     */
    private static final byte[] EMPTY_RESPONSE = new byte[0];
    /**
     * Default value of the {@link #maxParallelProcessedMsgs} attribute.
     */
    private static final int DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES = 50;
    /**
     * Mutex used to wait for the termination signal.
     */
    private Semaphore terminationMutex = new Semaphore(0);
    /**
     * The incoming queue from the task generator.
     */
    protected DataReceiverImpl expResponseReceiver;
    /**
     * The incoming queue from the system.
     */
    protected DataReceiverImpl systemResponseReceiver;
    /**
     * The maximum number of incoming messages of a single queue that are processed
     * in parallel. Additional messages have to wait.
     */
    private final int maxParallelProcessedMsgs;
    /**
     * Iterators that have been started.
     */
    protected List<Iterator<? extends ResultPair>> resultPairIterators = Lists.newArrayList();
    /**
     * The incoming queue from the task generator.
     */
    protected DataReceiver taskResultReceiver;
    /**
     * The incoming queue from the system.
     */
    protected DataReceiver systemResultReceiver;
    /**
     * The incoming queue from the evaluation module.
     */
    protected RabbitQueue evalModule2EvalStoreQueue;
    /**
     * Channel on which the acknowledgements are send.
     */
    protected Channel ackChannel = null;
    protected String ackExchangeName = null;
    protected Map<String, PairedDataSender> replyingSenders = new HashMap<>();
    protected boolean receiveTimeStamp = false;

    /**
     * Constructor using the {@link #DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES}=
     * {@value #DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES}.
     */
    public AbstractEvaluationStorage() {
        this(DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES);
    }

    /**
     * Constructor setting the maximum number of messages processed in parallel.
     *
     * @param maxParallelProcessedMsgs
     *            The maximum number of incoming messages of a single queue that are
     *            processed in parallel. Additional messages have to wait.
     */
    public AbstractEvaluationStorage(int maxParallelProcessedMsgs) {
        this.maxParallelProcessedMsgs = maxParallelProcessedMsgs;
        defaultContainerType = Constants.CONTAINER_TYPE_DATABASE;
    }

    @Override
    public void init() throws Exception {
        super.init();
        Map<String, String> env = System.getenv();
        String queueName;
        if (expResponseReceiver == null) {
            queueName = Constants.TASK_GEN_2_EVAL_STORAGE_DEFAULT_QUEUE_NAME;
            if (env.containsKey(Constants.TASK_GEN_2_EVAL_STORAGE_QUEUE_NAME_KEY)) {
                queueName = env.get(Constants.TASK_GEN_2_EVAL_STORAGE_QUEUE_NAME_KEY);
            }
            expResponseReceiver = DataReceiverImpl.builder().maxParallelProcessedMsgs(maxParallelProcessedMsgs)
                    .name("ES-DR-from-TG").dataHandler(new ExpectedResponseReceiver())
                    .queue(getFactoryForIncomingDataQueues(), generateSessionQueueName(queueName)).build();
        } else {
            // XXX here we could set the data handler if the data receiver would
            // offer such a method
        }

        if (systemResponseReceiver == null) {
            queueName = Constants.SYSTEM_2_EVAL_STORAGE_DEFAULT_QUEUE_NAME;
            if (env.containsKey(Constants.SYSTEM_2_EVAL_STORAGE_QUEUE_NAME_KEY)) {
                queueName = env.get(Constants.SYSTEM_2_EVAL_STORAGE_QUEUE_NAME_KEY);
            }
            systemResponseReceiver = DataReceiverImpl.builder().maxParallelProcessedMsgs(maxParallelProcessedMsgs)
                    .name("ES-DR-from-SA").dataHandler(new SystemResponseReceiver())
                    .queue(getFactoryForIncomingDataQueues(), generateSessionQueueName(queueName)).build();
        } else {
            // XXX here we could set the data handler if the data receiver would
            // offer such a method
        }
        queueName = Constants.EVAL_MODULE_2_EVAL_STORAGE_DEFAULT_QUEUE_NAME;
        if (env.containsKey(Constants.EVAL_MODULE_2_EVAL_STORAGE_QUEUE_NAME_KEY)) {
            queueName = env.get(Constants.EVAL_MODULE_2_EVAL_STORAGE_QUEUE_NAME_KEY);
        }
        evalModule2EvalStoreQueue = getFactoryForIncomingDataQueues()
                .createDefaultRabbitQueue(generateSessionQueueName(queueName));
        evalModule2EvalStoreQueue.channel.basicConsume(evalModule2EvalStoreQueue.name, true,
                new IterationRequestReceiver(evalModule2EvalStoreQueue.channel));

        boolean sendAcks = false;
        if (env.containsKey(Constants.ACKNOWLEDGEMENT_FLAG_KEY)) {
            sendAcks = Boolean.parseBoolean(env.getOrDefault(Constants.ACKNOWLEDGEMENT_FLAG_KEY, "false"));
            if (sendAcks) {
                // Create channel for acknowledgements
                ackChannel = getFactoryForOutgoingCmdQueues().getConnection().createChannel();
                ackExchangeName = generateSessionQueueName(Constants.HOBBIT_ACK_EXCHANGE_NAME);
                ackChannel.exchangeDeclare(ackExchangeName, "fanout",
                        false, true, null);
            }
        }
        if (env.containsKey(RECEIVE_TIMESTAMP_FOR_SYSTEM_RESULTS_KEY)) {
            try {
                receiveTimeStamp = Boolean.parseBoolean(env.get(RECEIVE_TIMESTAMP_FOR_SYSTEM_RESULTS_KEY));
            } catch (Exception e) {
                LOGGER.error(
                        "Couldn't read the value of the " + RECEIVE_TIMESTAMP_FOR_SYSTEM_RESULTS_KEY + " variable.", e);
            }
        }
    }

    /**
     * Creates a new iterator that iterates over the response pairs.
     *
     * @return a new iterator or null if an error occurred
     */
    protected abstract Iterator<? extends ResultPair> createIterator();

    @Override
    public void run() throws Exception {
        sendToCmdQueue(Commands.EVAL_STORAGE_READY_SIGNAL);
        terminationMutex.acquire();
        expResponseReceiver.closeWhenFinished();
        systemResponseReceiver.closeWhenFinished();
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // If this is the signal to start the data generation
        if (command == Commands.EVAL_STORAGE_TERMINATE) {
            // release the mutex
            terminationMutex.release();
        }
        super.receiveCommand(command, data);
    }

    protected void acknowledgeResponse(String taskId) {
        if (ackChannel != null) {
            try {
                ackChannel.basicPublish(ackExchangeName, "", null, RabbitMQUtils.writeString(taskId));
            } catch (IOException e) {
                LOGGER.error("Couldn't send acknowledgement for task {}.", taskId);
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (replyingSenders) {
            // Remove and close all senders
            for (String replyQueue : replyingSenders.keySet()) {
                replyingSenders.remove(replyQueue).closeWhenFinished();
            }
        }
        IOUtils.closeQuietly(expResponseReceiver);
        IOUtils.closeQuietly(systemResponseReceiver);
        IOUtils.closeQuietly(evalModule2EvalStoreQueue);
        if (ackChannel != null) {
            try {
                ackChannel.close();
            } catch (Exception e) {
                LOGGER.error("Error while trying to close the acknowledgement channel.", e);
            }
        }
        super.close();
    }

    /**
     * Receiver handling the expected responses.
     * 
     * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
     *
     */
    protected class ExpectedResponseReceiver implements IncomingStreamHandler {
        @Override
        public void handleIncomingStream(String streamId, InputStream stream) {
            long timestamp;
            String taskId;
            try {
                /*
                 * Check whether this is the old format (backwards compatibility to version
                 * 1.0.X in which the timestamp is placed behind(!) the data)
                 */
                if (streamId == null) {
                    // get taskId/streamId and timestamp
                    ByteBuffer buffer = ByteBuffer.wrap(IOUtils.toByteArray(stream));
                    taskId = RabbitMQUtils.readString(buffer);
                    byte[] data = RabbitMQUtils.readByteArray(buffer);
                    timestamp = buffer.getLong();
                    IOUtils.closeQuietly(stream);
                    // create a new stream containing only the data
                    stream = new ByteArrayInputStream(data);
                } else {
                    // get taskId and timestamp
                    timestamp = RabbitMQUtils.readLong(stream);
                    int length = RabbitMQUtils.readInt(stream);
                    taskId = RabbitMQUtils.readString(RabbitMQUtils.readByteArray(stream, length));
                }
                LOGGER.trace("Received from task generator {}.", taskId);

                receiveExpectedResponseData(taskId, timestamp, stream);
            } catch (IOException e) {
                LOGGER.error("IO Error while trying to read incoming expected response.", e);
            }
        }
    }

    /**
     * Receiver handling the responses coming from the system.
     * 
     * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
     *
     */
    protected class SystemResponseReceiver implements IncomingStreamHandler {
        @Override
        public void handleIncomingStream(String streamId, InputStream stream) {
            String taskId;
            long timestamp = System.currentTimeMillis();
            try {
                /*
                 * Check whether this is the old format (backwards compatibility to version
                 * 1.0.X in which the data is preceded by its length)
                 */
                if (streamId == null) {
                    // get taskId/streamId and timestamp
                    ByteBuffer buffer = ByteBuffer.wrap(IOUtils.toByteArray(stream));
                    taskId = RabbitMQUtils.readString(buffer);
                    byte[] data = RabbitMQUtils.readByteArray(buffer);
                    if (receiveTimeStamp) {
                        timestamp = RabbitMQUtils.readLong(stream);
                    }
                    IOUtils.closeQuietly(stream);
                    // create a new stream containing only the data
                    stream = new ByteArrayInputStream(data);
                } else {
                    // If there is a timestamp in front of the data
                    if (receiveTimeStamp) {
                        timestamp = RabbitMQUtils.readLong(stream);
                    }
                    // get data
                    int length = RabbitMQUtils.readInt(stream);
                    taskId = RabbitMQUtils.readString(RabbitMQUtils.readByteArray(stream, length));
                }
                LOGGER.trace("Received from system {}.", taskId);
                try {
                    receiveResponseData(taskId, timestamp, stream);
                } finally {
                    acknowledgeResponse(taskId);
                }
            } catch (IOException e) {
                LOGGER.error("IO Error while trying to read incoming expected response.", e);
            }
        }
    }

    protected class IterationRequestReceiver extends DefaultConsumer {

        public IterationRequestReceiver(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
                throws IOException {
            InputStream response[] = null;
            // get iterator id
            ByteBuffer buffer = ByteBuffer.wrap(body);
            if (buffer.remaining() < 1) {
                response = new InputStream[] { new ByteArrayInputStream(EMPTY_RESPONSE) };
                LOGGER.error("Got a request without a valid iterator Id. Returning emtpy response.");
            } else {
                byte iteratorId = buffer.get();

                // get the iterator
                Iterator<? extends ResultPair> iterator = null;
                if (iteratorId == NEW_ITERATOR_ID) {
                    // create and save a new iterator
                    iteratorId = (byte) resultPairIterators.size();
                    LOGGER.info("Creating new iterator #{}", iteratorId);
                    resultPairIterators.add(iterator = createIterator());
                } else if ((iteratorId < 0) || iteratorId >= resultPairIterators.size()) {
                    response = new InputStream[] { new ByteArrayInputStream(EMPTY_RESPONSE) };
                    LOGGER.error("Got a request without a valid iterator Id (" + Byte.toString(iteratorId)
                            + "). Returning emtpy response.");
                } else {
                    iterator = resultPairIterators.get(iteratorId);
                }
                if ((iterator != null) && (iterator.hasNext())) {
                    ResultPair resultPair = iterator.next();
                    // The order of the streams is defined by
                    // ITERATOR_ID_STREAM_ID,
                    // EXPECTED_RESPONSE_STREAM_ID and
                    // RECEIVED_RESPONSE_STREAM_ID
                    response = new InputStream[] { new ByteArrayInputStream(new byte[] { iteratorId }),
                            resultPair.getExpected(), resultPair.getActual() };
                } else {
                    response = new InputStream[] { new ByteArrayInputStream(new byte[] { iteratorId }) };
                }
            }
            PairedDataSender sender = null;
            synchronized (replyingSenders) {
                if (replyingSenders.containsKey(properties.getReplyTo())) {
                    sender = replyingSenders.get(properties.getReplyTo());
                } else {
                    sender = PairedDataSender.builder()
                            .queue(getFactoryForOutgoingDataQueues(), properties.getReplyTo())
                            .idGenerator(new SteppingIdGenerator(0, 1)).build();
                    replyingSenders.put(properties.getReplyTo(), sender);
                }
            }
            sender.sendData(response);
            for (int i = 0; i < response.length; ++i) {
                IOUtils.closeQuietly(response[i]);
            }
        }
    }
}
