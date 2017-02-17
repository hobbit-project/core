package org.hobbit.core.rabbit.consume;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.hobbit.core.rabbit.DataReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public abstract class MessageConsumer extends DefaultConsumer implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);

    /**
     * Default value of the {@link #maxParallelProcessedMsgs} attribute.
     */
    public static final int DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES = 1;

    /**
     * The maximum number of incoming messages that are processed in parallel.
     * Additional messages have to wait.
     */
    private final int maxParallelProcessedMsgs;
    /**
     * Semaphore used to control the number of messages that can be processed in
     * parallel.
     */
    private Semaphore currentlyProcessedMessages;
    /**
     * The {@link DataReceiver} which is using this consumer.
     */
    protected DataReceiver receiver;
    /**
     * The channel of the queue from which the incoming messages will be
     * consumed.
     */
    protected Channel channel;

    public MessageConsumer(DataReceiver receiver, Channel channel, int maxParallelProcessedMsgs) {
        super(channel);
        this.receiver = receiver;
        this.channel = channel;
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
                    channel.basicAck(envelope.getDeliveryTag(), false);
                } else {
                    channel.basicReject(envelope.getDeliveryTag(), true);
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

    protected abstract boolean handleMessage(BasicProperties properties, byte[] body) throws IOException;

    public void finishProcessing() throws InterruptedException {
    }

    public abstract int getOpenStreamCount();

    public void waitForTermination() {
        try {
            LOGGER.debug("Waiting data processing to finish... ( {} / {} free permits are available)",
                    currentlyProcessedMessages.availablePermits(), maxParallelProcessedMsgs);
            currentlyProcessedMessages.acquire(maxParallelProcessedMsgs);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for termination.", e);
        }
    }
}
