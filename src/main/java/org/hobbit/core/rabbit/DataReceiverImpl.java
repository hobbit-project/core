package org.hobbit.core.rabbit;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.data.DataReceiveState;
import org.hobbit.core.data.RabbitQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * Implementation of the {@link DataReceiver} interface.
 * 
 * <p>
 * Use the internal {@link Builder} class for creating instances of the
 * {@link DataReceiverImpl} class. <b>Note</b> that the created
 * {@link DataReceiverImpl} will either use a given {@link RabbitQueue} or
 * create a new one. In both cases the receiver will become the owner of the
 * queue, i.e., if the {@link DataReceiverImpl} instance is closed the queue
 * will be closed as well.
 * </p>
 * <p>
 * Internally, the receiver uses a multithreaded consumer that handles incoming
 * streams, sorts the messages that belong to these streams and sends the
 * received data via an {@link InputStream} to the given
 * {@link IncomingStreamHandler} instance. <b>Note</b> that since the consumer
 * is multithreaded the
 * {@link IncomingStreamHandler#handleIncomingStream(String, InputStream)}
 * should be thread safe since it might be called in parallel. Even setting the
 * maximum number of parallel processed messages to 1 (via
 * {@link Builder#maxParallelProcessedMsgs(int)}) the given handler might be
 * called with several {@link InputStream} instances in parallel.
 * </p>
 * <p>
 * The {@link DataReceiverImpl} owns recources that need to be freed if its work
 * is done. This can be achieved by closing the receiver. In most cases, this
 * should be done using the {@link #closeWhenFinished()} method which waits
 * until all incoming messages are processed and all streams are closed. Note
 * that using the {@link #close()} method leads to a direct shutdown of the
 * queue which could lead to data loss and threads getting stuck.
 * </p>
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class DataReceiverImpl implements DataReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataReceiverImpl.class);

    private static final int MAX_MESSAGE_BUFFER_SIZE = 50;
    private static final int CHECKS_BEFORE_CLOSING = 5;

    private RabbitQueue queue;
    private Map<String, DataReceiveState> streamStats = new HashMap<>();
    private int errorCount = 0;
    private IncomingStreamHandler dataHandler;
    private MessageConsumer consumer;

    protected DataReceiverImpl(RabbitQueue queue, IncomingStreamHandler handler, int maxParallelProcessedMsgs)
            throws IOException {
        this.queue = queue;
        this.dataHandler = handler;
        consumer = new MessageConsumer(this, queue.channel, maxParallelProcessedMsgs);
        // While defining the consumer we have to make sure that the auto
        // acknowledgement is turned off to make sure that the consumer will be
        // able to reject messages
        queue.channel.basicConsume(queue.name, false, consumer);
        queue.channel.basicQos(maxParallelProcessedMsgs);
        /*
         * XXX It might be possible to adapt the receiver to an implementation
         * that is more flexible, i.e., that offers a method to set the handler
         * and another method that "starts" the receiving by defining the
         * consumer (the two lines above). Note that setting the handler after
         * the receiver has been started is not working since the consumer might
         * encounter NullPointerExceptions
         */
    }

    protected synchronized void increaseErrorCount() {
        ++errorCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    /**
     * This method waits for the data receiver to finish its work and closes the
     * incoming queue as well as the internal thread pool after that.
     */
    public void closeWhenFinished() {
        // wait until all messages have been read from the queue
        long messageCount;
        int openStreamsCount;
        try {
            messageCount = queue.messageCount();
            openStreamsCount = getOpenStreamCount();
            System.out.println(messageCount + " | " + openStreamsCount);
            int checks = 0;
            int iteration = 0;
            while (checks < CHECKS_BEFORE_CLOSING) {
                messageCount = queue.messageCount();
                openStreamsCount = getOpenStreamCount();
                if ((messageCount + openStreamsCount) > 0) {
                    checks = 0;
                    if (LOGGER.isDebugEnabled() && ((iteration % 10) == 0)) {
                        LOGGER.debug(
                                "Waiting for {} remaining messages to be processed and {} remaining streams to be closed. (check #{})",
                                messageCount, openStreamsCount, iteration);
                    }
                } else {
                    ++checks;
                }
                Thread.sleep(200);
                ++iteration;
            }
            consumer.finishProcessing();
        } catch (Exception e) {
            LOGGER.error("Exception while waiting for remaining data to be processed.", e);
        }
        close(true);
    }

    /**
     * A rude way to close the receiver. Note that this method directly closes
     * the incoming queue and only notifies the internal consumer to stop its
     * work but won't wait for the handler threads to finish their work.
     */
    public void close() {
        close(false);
    }

    protected int getOpenStreamCount() {
        int count = 0;
        synchronized (streamStats) {
            for (String name : streamStats.keySet()) {
                if (streamStats.get(name).outputStream != null) {
                    ++count;
                }
            }
        }
        return count;
    }

    protected void close(boolean waitForConsmer) {
        IOUtils.closeQuietly(consumer);
        if (waitForConsmer) {
            System.out.println("Waiting for consumer");
            consumer.waitForTermination();
        }
        IOUtils.closeQuietly(queue);
        synchronized (streamStats) {
            for (String name : streamStats.keySet()) {
                if (streamStats.get(name).outputStream != null) {
                    LOGGER.warn("Closing stream \"{}\" for which no end message has been received.", name);
                    IOUtils.closeQuietly(streamStats.get(name).outputStream);
                    increaseErrorCount();
                }
            }
        }
    }

    protected static class MessageConsumer extends DefaultConsumer implements Closeable {
        /**
         * Default value of the {@link #maxParallelProcessedMsgs} attribute.
         */
        protected static final int DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES = 1;

        /**
         * The maximum number of incoming messages that are processed in
         * parallel. Additional messages have to wait.
         */
        private final int maxParallelProcessedMsgs;
        /**
         * Semaphore used to control the number of messages that can be
         * processed in parallel.
         */
        private Semaphore currentlyProcessedMessages;
        /**
         * Semaphore used to control the number of messages that can be
         * processed in parallel.
         */
        private ExecutorService executor = Executors.newCachedThreadPool();

        private DataReceiverImpl receiver;

        private boolean oldFormatWarningPrinted = false;

        public MessageConsumer(DataReceiverImpl receiver, Channel channel, int maxParallelProcessedMsgs) {
            super(channel);
            this.receiver = receiver;
            this.maxParallelProcessedMsgs = maxParallelProcessedMsgs;
            currentlyProcessedMessages = new Semaphore(maxParallelProcessedMsgs);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
                throws IOException {
            try {
                currentlyProcessedMessages.acquire();
                try {
                    if (handleMessage(properties, body)) {
                        receiver.queue.channel.basicAck(envelope.getDeliveryTag(), false);
                    } else {
                        receiver.queue.channel.basicReject(envelope.getDeliveryTag(), true);
                    }
                } catch (Exception e) {
                    LOGGER.error("Got exception while trying to process incoming data.", e);
                } finally {
                    currentlyProcessedMessages.release();
                }
            } catch (InterruptedException e) {
                throw new IOException("Interrupted while waiting for mutex.", e);
            }
        }

        protected boolean handleMessage(BasicProperties properties, byte[] body) throws IOException {
            // check if we have to handle the deprecated v0.0.1 format
            if ((properties.getCorrelationId() == null) && (properties.getMessageId() == null)) {
                if (!oldFormatWarningPrinted) {
                    LOGGER.info("Encountered old, deprecated message format!");
                    oldFormatWarningPrinted = true;
                }
                // In this old format, every incoming message is a single
                // stream, i.e., we can simply forward it
                InputStream stream = new ByteArrayInputStream(body);
                receiver.dataHandler.handleIncomingStream(null, stream);
                IOUtils.closeQuietly(stream);
            }
            String streamId = properties.getCorrelationId();
            int messageId = -1;
            try {
                messageId = Integer.parseInt(properties.getMessageId());
            } catch (NumberFormatException e) {
                LOGGER.error("Couldn't parse message Id. Message will be ignored.", e);
                return false;
            }
            DataReceiveState state = null;
            synchronized (receiver.streamStats) {
                if (receiver.streamStats.containsKey(streamId)) {
                    state = receiver.streamStats.get(streamId);
                } else {
                    if (messageId == 0) {
                        try {
                            final PipedInputStream pis = new PipedInputStream();
                            state = new DataReceiveState(streamId, new PipedOutputStream(pis));
                            receiver.streamStats.put(streamId, state);
                            executor.submit(new Runnable() {
                                @Override
                                public void run() {
                                    receiver.dataHandler.handleIncomingStream(streamId, pis);
                                    IOUtils.closeQuietly(pis);
                                    System.out.println("data Handler done");
                                }
                            });
                        } catch (Exception e) {
                            LOGGER.error("Couldn't create stream for incoming data. Message will be ignored.", e);
                            receiver.increaseErrorCount();
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }
            synchronized (state) {
                // If this message is marked as the last message of this stream
                if (Constants.END_OF_STREAM_MESSAGE_TYPE.equals(properties.getType())) {
                    state.lastMessageId = messageId;
                }
                if (messageId == state.nextMessageId) {
                    processMessageData(body, state);
                } else {
                    if (state.messageBuffer.size() < MAX_MESSAGE_BUFFER_SIZE) {
                        state.messageBuffer.put(messageId, body);
                    } else {
                        return false;
                    }
                }
            }
            return true;
        }

        protected void processMessageData(byte[] messageData, DataReceiveState state) {
            // write the data
            try {
                state.outputStream.write(messageData);
            } catch (IOException e) {
                LOGGER.error("Couldn't write message data to file.", e);
                receiver.increaseErrorCount();
            }
            ++state.nextMessageId;
            // If there is a message in the buffer that should be written
            // now
            if (state.messageBuffer.containsKey(state.nextMessageId)) {
                messageData = state.messageBuffer.remove(state.nextMessageId);
                processMessageData(messageData, state);
            } else if (state.nextMessageId >= state.lastMessageId) {
                // if this is the last message for this stream
                IOUtils.closeQuietly(state.outputStream);
                System.out.println("stream for " + state.name + " closed");
                state.outputStream = null;
                LOGGER.debug("Received last message for stream \"{}\".", state.name);
                if (state.messageBuffer.size() > 0) {
                    LOGGER.error("Closed the stream \"{}\" while there are still {} messages in its data buffer",
                            state.name, state.messageBuffer.size());
                }
            }
        }

        public void finishProcessing() throws InterruptedException {
            LOGGER.debug("Waiting data processing to finish... ( {} / {} free permits are available)",
                    currentlyProcessedMessages.availablePermits(), maxParallelProcessedMsgs);
            currentlyProcessedMessages.acquire(maxParallelProcessedMsgs);
        }

        @Override
        public void close() throws IOException {
            executor.shutdown();
        }

        public void waitForTermination() {
            try {
                System.out.println("Waiting for executor");
                executor.awaitTermination(30, TimeUnit.MINUTES);
                System.out.println("Executor terminated");
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting for termination.", e);
            }
        }
    }

    public static final class Builder {

        private static final String QUEUE_INFO_MISSING_ERROR = "There are neither a queue nor a queue name and a queue factory provided for the DataReceiver. Either a queue or a name and a factory to create a new queue are mandatory.";
        private static final String DATA_HANDLER_MISSING_ERROR = "The necessary data handler has not been provided for the DataReceiver.";

        private IncomingStreamHandler dataHandler;
        private RabbitQueue queue;
        private String queueName;
        private int maxParallelProcessedMsgs = MessageConsumer.DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES;
        private RabbitQueueFactory factory;

        public Builder() {
        };

        /**
         * Sets the handler that is called if data is incoming.
         * 
         * @param dataHandler
         *            the handler that is called if data is incoming
         * @return this builder instance
         */
        public Builder dataHandler(IncomingStreamHandler dataHandler) {
            this.dataHandler = dataHandler;
            return this;
        }

        /**
         * Sets the queue that is used to receive data.
         * 
         * @param queue
         *            the queue that is used to receive data
         * @return this builder instance
         */
        public Builder queue(RabbitQueue queue) {
            this.queue = queue;
            return this;
        }

        /**
         * Method for providing the necessary information to create a queue if
         * it has not been provided with the {@link #queue(RabbitQueue)} method.
         * Note that this information is not used if a queue has been provided.
         * 
         * @param factory
         *            the queue factory used to create a queue
         * @param queueName
         *            the name of the newly created queue
         * @return this builder instance
         */
        public Builder queue(RabbitQueueFactory factory, String queueName) {
            this.factory = factory;
            this.queueName = queueName;
            return this;
        }

        /**
         * Sets the maximum number of incoming messages that are processed in
         * parallel. Additional messages have to wait in the queue.
         * 
         * @param maxParallelProcessedMsgs
         *            the maximum number of incoming messages that are processed
         *            in parallel
         * @return this builder instance
         */
        public Builder maxParallelProcessedMsgs(int maxParallelProcessedMsgs) {
            this.maxParallelProcessedMsgs = maxParallelProcessedMsgs;
            return this;
        }

        /**
         * Builds the {@link DataReceiverImpl} instance with the previously
         * given information.
         * 
         * @return The newly created DataReceiver instance
         * @throws IllegalStateException
         *             if the dataHandler is missing or if neither a queue nor
         *             the information needed to create a queue have been
         *             provided.
         * @throws IOException
         *             if an exception is thrown while creating a new queue or
         *             if the given queue can not be configured by the newly
         *             created DataReceiver. <b>Note</b> that in the latter case
         *             the queue will be closed.
         */
        public DataReceiverImpl build() throws IllegalStateException, IOException {
            if (dataHandler == null) {
                throw new IllegalStateException(DATA_HANDLER_MISSING_ERROR);
            }
            if (queue == null) {
                if ((queueName == null) || (factory == null)) {
                    throw new IllegalStateException(QUEUE_INFO_MISSING_ERROR);
                } else {
                    queue = factory.createDefaultRabbitQueue(queueName);
                }
            }
            try {
                return new DataReceiverImpl(queue, dataHandler, maxParallelProcessedMsgs);
            } catch (IOException e) {
                IOUtils.closeQuietly(queue);
                throw e;
            }
        }
    }

}
