package org.hobbit.core.rabbit;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.consume.MessageConsumer;
import org.hobbit.core.rabbit.consume.MessageConsumerBuilder;
import org.hobbit.core.rabbit.consume.MessageConsumerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final int CHECKS_BEFORE_CLOSING = 5;

    protected RabbitQueue queue;
    private int errorCount = 0;
    private IncomingStreamHandler dataHandler;
    // private MessageConsumerBuilder consumerBuilder; XXX could be used to
    // create the consumer later on
    private MessageConsumer consumer;

    protected DataReceiverImpl(RabbitQueue queue, IncomingStreamHandler handler, MessageConsumerBuilder consumerBuilder,
            int maxParallelProcessedMsgs) throws IOException {
        this.queue = queue;
        this.dataHandler = handler;
        // consumer = new MessageConsumer(this, queue.channel,
        // maxParallelProcessedMsgs);
        consumer = consumerBuilder.maxParallelProcessedMsgs(maxParallelProcessedMsgs).build(this, queue.channel);
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

    public IncomingStreamHandler getDataHandler() {
        return dataHandler;
    }

    public synchronized void increaseErrorCount() {
        ++errorCount;
    }

    public int getErrorCount() {
        return errorCount;
    }
    
    public RabbitQueue getQueue() {
        return queue;
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
            openStreamsCount = consumer.getOpenStreamCount();
            int checks = 0;
            int iteration = 0;
            while (checks < CHECKS_BEFORE_CLOSING) {
                messageCount = queue.messageCount();
                openStreamsCount = consumer.getOpenStreamCount();
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
            ;
            consumer.waitForTermination();
        } catch (Exception e) {
            LOGGER.error("Exception while waiting for remaining data to be processed.", e);
        }
        close();
    }

    /**
     * A rude way to close the receiver. Note that this method directly closes
     * the incoming queue and only notifies the internal consumer to stop its
     * work but won't wait for the handler threads to finish their work.
     */
    public void close() {
        IOUtils.closeQuietly(queue);
        IOUtils.closeQuietly(consumer);
    }

    /**
     * Returns a newly created {@link Builder}.
     * 
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private static final String QUEUE_INFO_MISSING_ERROR = "There are neither a queue nor a queue name and a queue factory provided for the DataReceiver. Either a queue or a name and a factory to create a new queue are mandatory.";
        private static final String DATA_HANDLER_MISSING_ERROR = "The necessary data handler has not been provided for the DataReceiver.";

        private IncomingStreamHandler dataHandler;
        private RabbitQueue queue;
        private String queueName;
        private int maxParallelProcessedMsgs = MessageConsumer.DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES;
        private RabbitQueueFactory factory;
        private MessageConsumerBuilder consumerBuilder;

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
         * Sets the {@link MessageConsumerBuilder} which is used to create the
         * {@link MessageConsumer} for the created {@link DataReceiverImpl}.
         * 
         * @param consumerBuilder
         *            the builder used to create the consumer
         * @return this builder instance
         */
        public Builder consumerBuilder(MessageConsumerBuilder consumerBuilder) {
            this.consumerBuilder = consumerBuilder;
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
                    // create a new queue
                    queue = factory.createDefaultRabbitQueue(queueName);
                }
            }
            // If there is no consumer builder use the default implementation
            if (consumerBuilder == null) {
                consumerBuilder = MessageConsumerImpl.builder();
            }
            try {
                return new DataReceiverImpl(queue, dataHandler, consumerBuilder, maxParallelProcessedMsgs);
            } catch (IOException e) {
                IOUtils.closeQuietly(queue);
                throw e;
            }
        }
    }

}
