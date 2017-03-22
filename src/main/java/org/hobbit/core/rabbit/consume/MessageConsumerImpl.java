package org.hobbit.core.rabbit.consume;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.data.DataReceiveState;
import org.hobbit.core.rabbit.DataReceiver;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.rabbit.IncomingStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

public class MessageConsumerImpl extends MessageConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerImpl.class);

    private static final int MAX_MESSAGE_BUFFER_SIZE = 50;
    private static final int MAX_CLOSED_STREAM_BUFFER_SIZE = 100;
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

    private boolean oldFormatWarningPrinted = false;

    public MessageConsumerImpl(DataReceiver receiver, Channel channel, int maxParallelProcessedMsgs
    /*
     * , int maxParallelHandledMessages
     */
    ) {
        super(receiver, channel, maxParallelProcessedMsgs);
    }

    protected boolean handleMessage(BasicProperties properties, byte[] body) throws IOException {
        // check if we have to handle the deprecated v1.0.0 format
        if ((properties.getCorrelationId() == null) && (properties.getMessageId() == null)) {
            if (!oldFormatWarningPrinted) {
                LOGGER.info("Encountered old, deprecated message format!");
                oldFormatWarningPrinted = true;
            }
            // In this old format, every incoming message is a single
            // stream, i.e., we can simply forward it
            return handleSimpleMessage(body);
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
                    try {
                        final PipedInputStream pis = new PipedInputStream();
                        state = new DataReceiveState(streamId, new PipedOutputStream(pis));
                        streamStats.put(streamId, state);
                        executor.submit(new StreamHandlerCall(streamId, pis, receiver.getDataHandler()));
                    } catch (Exception e) {
                        LOGGER.error("Couldn't create stream for incoming data. Message will be ignored.", e);
                        receiver.increaseErrorCount();
                        return true;
                    }
                } else {
                    // this message should be consumed by somebody else
                    return false;
                }
            }
        }
        synchronized (state) {
            // If this stream is already closed and this message is only a copy
            // that has been received a second time
            if (state.outputStream == null) {
                // consume it to make sure that it is not sent again
                return true;
            }
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

    /**
     * 
     * 
     * @param properties
     * @param body
     * @return
     * @throws IOException
     */
    protected boolean handleSimpleMessage(byte[] body) throws IOException {
        executor.submit(new StreamHandlerCall(null, new ByteArrayInputStream(body), receiver.getDataHandler()));
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
            // clear all remaining messages (they should have been removed
            // before. However, maybe a message has been sent a second time)
            state.messageBuffer.clear();
            LOGGER.debug("Received last message for stream \"{}\".", state.name);
            if (state.messageBuffer.size() > 0) {
                LOGGER.error("Closed the stream \"{}\" while there are still {} messages in its data buffer",
                        state.name, state.messageBuffer.size());
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
                    LOGGER.warn("Closing stream \"{}\" for which no end message has been received.", name);
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

        private int maxParallelProcessedMsgs = MessageConsumer.DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES;

        @Override
        public MessageConsumerBuilder maxParallelProcessedMsgs(int maxParallelProcessedMsgs) {
            this.maxParallelProcessedMsgs = maxParallelProcessedMsgs;
            return this;
        }

        @Override
        public MessageConsumer build(DataReceiverImpl receiver, Channel channel) {
            return new MessageConsumerImpl(receiver, channel, maxParallelProcessedMsgs);
        }

    }

    /**
     * A simple {@link Runnable} implementation that calls the given
     * {@link IncomingStreamHandler} with the given {@link InputStream} and
     * stream ID.
     * 
     * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
     *
     */
    public static class StreamHandlerCall implements Runnable {

        private String streamId;
        private InputStream stream;
        private IncomingStreamHandler handler;

        public StreamHandlerCall(String streamId, InputStream stream, IncomingStreamHandler handler) {
            this.streamId = streamId;
            this.stream = stream;
            this.handler = handler;
        }

        @Override
        public void run() {
            handler.handleIncomingStream(streamId, stream);
            IOUtils.closeQuietly(stream);
        }

    }
}
