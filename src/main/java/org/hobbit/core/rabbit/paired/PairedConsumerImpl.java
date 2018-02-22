package org.hobbit.core.rabbit.paired;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.data.DataReceiveState;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.DataReceiver;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.consume.AbstractMessageConsumer;
import org.hobbit.core.rabbit.consume.MessageConsumer;
import org.hobbit.core.rabbit.consume.MessageConsumerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

public class PairedConsumerImpl extends AbstractMessageConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PairedConsumerImpl.class);

    private static final int MAX_MESSAGE_BUFFER_SIZE = 50;
    /**
     * Default value of the {@link #maxParallelProcessedMsgs} attribute.
     */
    protected static final int DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES = 1;
    // public static final int NO_MAX_HANDLING = -1;

    /**
     * Semaphore used to control the number of messages that can be processed in
     * parallel.
     */
    private ExecutorService executor = Executors.newCachedThreadPool();

    protected Map<String, DataReceiveState> streamStats = new HashMap<>();

    public PairedConsumerImpl(DataReceiver receiver, Channel channel, int maxParallelProcessedMsgs) {
        this(receiver, channel, maxParallelProcessedMsgs, null);
    }

    public PairedConsumerImpl(DataReceiver receiver, Channel channel, int maxParallelProcessedMsgs, String name) {
        super(receiver, channel, maxParallelProcessedMsgs, name);
    }

    protected boolean handleMessage(BasicProperties properties, byte[] body) throws IOException {
        // check if we have to handle the deprecated v0.0.1 format
        if ((properties.getCorrelationId() == null) || (properties.getMessageId() == null)) {
            LOGGER.error("{}: Received a message without the needed correlation and message Ids. It will be ignored.", name);
            return true;
        }
        String streamId = properties.getCorrelationId();
        int messageId = -1;
        try {
            messageId = Integer.parseInt(properties.getMessageId());
        } catch (NumberFormatException e) {
            LOGGER.error("Couldn't parse message Id. Message will be ignored.", e);
            receiver.increaseErrorCount();
            return true;
        }
        DataReceiveState state = null;
        synchronized (streamStats) {
            if (streamStats.containsKey(streamId)) {
                state = streamStats.get(streamId);
            } else {
                if (messageId == 0) {
                    String clusterId = properties.getClusterId();
                    // If this is a head message
                    if (clusterId != null) {
                        String streamIds[] = null;
                        try {
                            streamIds = readStreamIds(body);
                        } catch (Exception e) {
                            LOGGER.error("Couldn't read stream Ids from the head message. Message will be ignored.", e);
                            receiver.increaseErrorCount();
                            return true;
                        }
                        try {
                            final PipedInputStream streams[] = new PipedInputStream[streamIds.length];
                            for (int i = 0; i < streams.length; ++i) {
                                streams[i] = new PipedInputStream();
                                state = new DataReceiveState(streamIds[i], new PipedOutputStream(streams[i]));
                                streamStats.put(streamIds[i], state);
                            }
                            executor.submit(new Runnable() {
                                @Override
                                public void run() {
                                    ((PairedStreamHandler) receiver.getDataHandler()).handleIncomingStreams(streamId,
                                            streams);
                                    for (int i = 0; i < streams.length; ++i) {
                                        IOUtils.closeQuietly(streams[i]);
                                    }
                                }
                            });
                        } catch (Exception e) {
                            LOGGER.error("Couldn't create stream for incoming data. Message will be ignored.", e);
                            receiver.increaseErrorCount();
                        }
                        // We have consumed the head message and can resume
                        return true;
                    } else {
                        // This message is the beginning of a stream for which
                        // we have not seen a head message before
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

    private String[] readStreamIds(byte[] body) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(body);
        List<String> streamIds = new ArrayList<String>();
        while (buffer.hasRemaining()) {
            streamIds.add(RabbitMQUtils.readString(buffer));
        }
        Collections.sort(streamIds);
        return streamIds.toArray(new String[streamIds.size()]);
    }

    protected void processMessageData(byte[] messageData, DataReceiveState state) {
        // write the data
        try {
            state.outputStream.write(messageData);
        } catch (IOException e) {
            LOGGER.error("Couldn't write message data to stream.", e);
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
            LOGGER.debug("{}: Received last message for stream \"{}\".", name, state.name);
            if (state.messageBuffer.size() > 0) {
                LOGGER.error("{}: Closed the stream \"{}\" while there are still {} messages in its data buffer",
                        name, state.name, state.messageBuffer.size());
            }
        }
    }

    public int getOpenStreamCount() {
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

    @Override
    public void close() throws IOException {
        executor.shutdownNow();
        synchronized (streamStats) {
            for (String name : streamStats.keySet()) {
                if (streamStats.get(name).outputStream != null) {
                    LOGGER.warn("{}: Closing stream \"{}\" for which no end message has been received.", this.name, name);
                    IOUtils.closeQuietly(streamStats.get(name).outputStream);
                    receiver.increaseErrorCount();
                }
            }
        }
    }

    public void waitForTermination() {
        super.waitForTermination();
        try {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for termination.", e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements MessageConsumerBuilder {

        private int maxParallelProcessedMsgs = DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES;
        private String name = null;

        @Override
        public MessageConsumerBuilder maxParallelProcessedMsgs(int maxParallelProcessedMsgs) {
            this.maxParallelProcessedMsgs = maxParallelProcessedMsgs;
            return this;
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public MessageConsumer build(DataReceiverImpl receiver, RabbitQueue queue) {
            return new PairedConsumerImpl(receiver, queue.channel, maxParallelProcessedMsgs, name);
        }

    }
}
