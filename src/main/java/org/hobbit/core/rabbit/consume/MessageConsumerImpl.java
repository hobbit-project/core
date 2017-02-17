package org.hobbit.core.rabbit.consume;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.data.DataReceiveState;
import org.hobbit.core.rabbit.DataReceiver;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

public class MessageConsumerImpl extends MessageConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerImpl.class);

    private static final int MAX_MESSAGE_BUFFER_SIZE = 50;
    /**
     * Default value of the {@link #maxParallelProcessedMsgs} attribute.
     */
    protected static final int DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES = 1;
//    public static final int NO_MAX_HANDLING = -1;

    /**
     * Semaphore used to control the number of messages that can be processed in
     * parallel.
     */
    private ExecutorService executor = Executors.newCachedThreadPool();

    protected Map<String, DataReceiveState> streamStats = new HashMap<>();

    private boolean oldFormatWarningPrinted = false;

    public MessageConsumerImpl(DataReceiver receiver, Channel channel, int maxParallelProcessedMsgs
            /*,
            int maxParallelHandledMessages*/
            ) {
        super(receiver, channel, maxParallelProcessedMsgs);
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
            receiver.getDataHandler().handleIncomingStream(null, stream);
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
        synchronized (streamStats) {
            if (streamStats.containsKey(streamId)) {
                state = streamStats.get(streamId);
            } else {
                if (messageId == 0) {
                    try {
                        final PipedInputStream pis = new PipedInputStream();
                        state = new DataReceiveState(streamId, new PipedOutputStream(pis));
                        streamStats.put(streamId, state);
                        executor.submit(new Runnable() {
                            @Override
                            public void run() {
                                receiver.getDataHandler().handleIncomingStream(streamId, pis);
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
            state.outputStream = null;
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
}
