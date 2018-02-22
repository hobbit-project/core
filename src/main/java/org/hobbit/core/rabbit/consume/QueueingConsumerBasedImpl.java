package org.hobbit.core.rabbit.consume;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.DataReceiver;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.utils.TerminatableRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.QueueingConsumer;

public class QueueingConsumerBasedImpl extends QueueingConsumer implements MessageConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueingConsumerBasedImpl.class);

    private static final int MAX_MESSAGE_BUFFER_SIZE = 50;
    /**
     * The {@link DataReceiver} which is using this consumer.
     */
    protected DataReceiver receiver;
    
    private ExecutorService executor;

    protected Map<String, DataReceiveState> streamStats = new HashMap<>();

    protected RabbitQueue queue;
    private int errorCount = 0;
    private TerminatableRunnable receiverTask;
    private Thread receiverThread;
    private boolean oldFormatWarningPrinted = false;

    public QueueingConsumerBasedImpl(DataReceiver receiver, RabbitQueue queue, int maxParallelProcessedMsgs) {
        super(queue.channel);
        this.receiver = receiver;
        this.queue = queue;
        receiverTask = buildMsgReceivingTask(this);
        receiverThread = new Thread(receiverTask);
        receiverThread.start();
        executor = Executors.newFixedThreadPool(maxParallelProcessedMsgs);
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
            LOGGER.trace("Received last message for stream \"{}\".", state.name);
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
        receiverTask.terminate();
        // Try to wait for the receiver task to finish
        try {
            receiverThread.join();
        } catch (Exception e) {
            LOGGER.error("Exception while waiting for termination of receiver task. Closing receiver.", e);
        }
        // After the receiver task finished, no new tasks are added to the
        // executor. Now we can ask the executor to shut down.
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            LOGGER.error("Exception while waiting for termination. Closing receiver.", e);
        }
    }

    public void finishProcessing() throws InterruptedException {
    }

    public synchronized void increaseErrorCount() {
        ++errorCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    /**
     * This factory method creates a runnable task that uses the given consumer to
     * receive incoming messages.
     * 
     * @param consumer
     *            the consumer that can be used to receive messages
     * @return a Runnable instance that will handle incoming messages as soon as it
     *         will be executed
     */
    protected TerminatableRunnable buildMsgReceivingTask(QueueingConsumer consumer) {
        return new MsgReceivingTask(consumer);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements MessageConsumerBuilder {

        private int maxParallelProcessedMsgs = AbstractMessageConsumer.DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES;

        @Override
        public MessageConsumerBuilder maxParallelProcessedMsgs(int maxParallelProcessedMsgs) {
            this.maxParallelProcessedMsgs = maxParallelProcessedMsgs;
            return this;
        }

        @Override
        public MessageConsumer build(DataReceiverImpl receiver, RabbitQueue queue) {
            return new QueueingConsumerBasedImpl(receiver, queue, maxParallelProcessedMsgs);
        }

    }

    protected class MsgReceivingTask implements TerminatableRunnable {

        private QueueingConsumer consumer;
        private boolean runFlag = true;

        public MsgReceivingTask(QueueingConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public void run() {
            int count = 0;
            Delivery delivery = null;
            while (runFlag || (queue.messageCount() > 0) || (delivery != null)) {
                try {
                    delivery = consumer.nextDelivery(3000);
                } catch (Exception e) {
                    LOGGER.error("Exception while waiting for delivery.", e);
                    increaseErrorCount();
                }
                if (delivery != null) {
                    try {
                        if (handleMessage(delivery.getProperties(), delivery.getBody())) {
                            queue.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                            ++count;
                        } else {
                            queue.channel.basicReject(delivery.getEnvelope().getDeliveryTag(), true);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Got exception while trying to process incoming data.", e);
                    }
                }
            }
            LOGGER.debug("Receiver task terminates after receiving {} messages.", count);
        }

        @Override
        public void terminate() {
            runFlag = false;
        }

        @Override
        public boolean isTerminated() {
            return !runFlag;
        }
    }

}
