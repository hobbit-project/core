package org.hobbit.core.rabbit;

import java.io.Closeable;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.components.RabbitQueueFactory;
import org.hobbit.core.data.DataReceiveState;
import org.hobbit.core.data.RabbitQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class DataReceiver implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataReceiver.class);

    private static final int MAX_MESSAGE_BUFFER_SIZE = 50;

    public static DataReceiver create(RabbitQueueFactory factory, String queueName, IncomingStreamHandler handler)
            throws IOException {
        return create(factory.createDefaultRabbitQueue(queueName), handler);
    }

    public static DataReceiver create(RabbitQueue queue, IncomingStreamHandler handler) throws IOException {
        return new DataReceiver(queue, handler);
    }

    private RabbitQueue queue;
    private Map<String, DataReceiveState> streamStats = new HashMap<>();
    private int errorCount = 0;
    private IncomingStreamHandler dataHandler;
    private MessageConsumer consumer;

    protected DataReceiver(RabbitQueue queue, IncomingStreamHandler handler) throws IOException {
        this.queue = queue;
        this.dataHandler = handler;
        consumer = new MessageConsumer(this, queue.channel);
        queue.channel.basicConsume(queue.name, true, consumer);
        // FIXME
        // channel.basicQos(maxParallelProcessedMsgs);
    }

    protected synchronized void increaseErrorCount() {
        ++errorCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void closeWhenFinished() {
        // wait until all messages have been read from the queue
        long messageCount;
        try {
            messageCount = queue.channel.messageCount(queue.name);
            while (messageCount > 0) {
                LOGGER.debug("Waiting for remaining data to be processed: " + messageCount);
                Thread.sleep(1000);
                messageCount = queue.channel.messageCount(queue.name);
            }
            consumer.finishProcessing();
        } catch (Exception e) {
            LOGGER.error("Exception while waiting for remaining data to be processed.", e);
        }
        close();
    }

    public void close() {
        IOUtils.closeQuietly(queue);
        for (String name : streamStats.keySet()) {
            if (streamStats.get(name).outputStream != null) {
                LOGGER.warn("Closing stream \"{}\" for which no end message has been received.", name);
                IOUtils.closeQuietly(streamStats.get(name).outputStream);
                increaseErrorCount();
            }
        }
    }

    protected static class MessageConsumer extends DefaultConsumer implements Closeable {
        /**
         * Default value of the {@link #maxParallelProcessedMsgs} attribute.
         */
        private static final int DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES = 1;

        /**
         * The maximum number of incoming messages that are processed in
         * parallel. Additional messages have to wait.
         */
        private final int maxParallelProcessedMsgs = DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES;
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

        private DataReceiver receiver;

        public MessageConsumer(DataReceiver receiver, Channel channel) {
            super(channel);
            this.receiver = receiver;
            currentlyProcessedMessages = new Semaphore(maxParallelProcessedMsgs);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
                throws IOException {
            try {
                currentlyProcessedMessages.acquire();
                try {
                    if (!handleMessage(properties, body)) {
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
                if(Constants.END_OF_STREAM_MESSAGE_TYPE.equals(properties.getType())) {
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
            IOUtils.closeQuietly(this);
        }

        @Override
        public void close() throws IOException {
            executor.shutdown();
        }
        
    }

    public static final class Builder {

        private String contentType;
        private String contentEncoding;
        private Map<String, Object> headers;
        private Integer deliveryMode;
        private Integer priority;
        private String correlationId;
        private String replyTo;
        private String expiration;
        private String messageId;
        private Date timestamp;
        private String type;
        private String userId;
        private String appId;
        private String clusterId;

        public Builder() {
        };

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder contentEncoding(String contentEncoding) {
            this.contentEncoding = contentEncoding;
            return this;
        }

        public Builder headers(Map<String, Object> headers) {
            this.headers = headers;
            return this;
        }

        public Builder deliveryMode(Integer deliveryMode) {
            this.deliveryMode = deliveryMode;
            return this;
        }

        public Builder priority(Integer priority) {
            this.priority = priority;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder replyTo(String replyTo) {
            this.replyTo = replyTo;
            return this;
        }

        public Builder expiration(String expiration) {
            this.expiration = expiration;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder timestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder appId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder clusterId(String clusterId) {
            this.clusterId = clusterId;
            return this;
        }

        public BasicProperties build() {
            return new BasicProperties(contentType, contentEncoding, headers, deliveryMode, priority, correlationId,
                    replyTo, expiration, messageId, timestamp, type, userId, appId, clusterId);
        }
    }

}
