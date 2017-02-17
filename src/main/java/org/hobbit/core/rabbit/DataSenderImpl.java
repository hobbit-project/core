package org.hobbit.core.rabbit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.utils.IdGenerator;
import org.hobbit.core.utils.RandomIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.AMQP.BasicProperties;

public class DataSenderImpl implements DataSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSenderImpl.class);

    private static final int DEFAULT_MESSAGE_SIZE = 65536;
    private static final int DEFAULT_DELIVERY_MODE = 2;

    private IdGenerator idGenerator = new RandomIdGenerator();
    private RabbitQueue queue;
    private final int messageSize;
    private final int maxMessageSize;
    private final int deliveryMode;

    protected DataSenderImpl(RabbitQueue queue, IdGenerator idGenerator, int messageSize, int deliveryMode) {
        this.queue = queue;
        this.idGenerator = idGenerator;
        this.messageSize = messageSize;
        this.maxMessageSize = 2 * messageSize;
        this.deliveryMode = deliveryMode;
    }

    @Override
    public void sendData(byte[] data) throws IOException {
        sendData(data, idGenerator.getNextId());
    }

    @Override
    public void sendData(byte[] data, String dataId) throws IOException {
        sendData(new ByteArrayInputStream(data), dataId);
    }

    @Override
    public void sendData(InputStream is) throws IOException {
        sendData(is, idGenerator.getNextId());
    }

    @Override
    public void sendData(InputStream is, String dataId) throws IOException {
        int messageId = 0;
        int length = 0;
        int dataPos = 0;
        byte[] buffer = new byte[maxMessageSize];
        BasicProperties.Builder probBuilder = new BasicProperties.Builder();
        probBuilder.correlationId(dataId);
        probBuilder.deliveryMode(deliveryMode);
        while (true) {
            length = is.read(buffer, dataPos, buffer.length - dataPos);
            // if the stream is at its end
            if (length < 0) {
                // send last message
                probBuilder.messageId(Integer.toString(messageId));
                probBuilder.type(Constants.END_OF_STREAM_MESSAGE_TYPE);
                queue.channel.basicPublish("", queue.name, probBuilder.build(), Arrays.copyOf(buffer, dataPos));
                return;
            } else {
                dataPos += length;
                if (dataPos >= messageSize) {
                    probBuilder.messageId(Integer.toString(messageId));
                    queue.channel.basicPublish("", queue.name, probBuilder.build(), Arrays.copyOf(buffer, dataPos));
                    ++messageId;
                    dataPos = 0;
                }
            }
        }
    }

    @Override
    public void closeWhenFinished() {
        // If we want to make sure that all messages are delivered we have to
        // wait until all messages are consumed
        try {
            // Simply check whether the queue is empty. If the check is true 5
            // times, we can assume that it is empty
            int check = 0;
            while (check < 5) {
                if (queue.messageCount() > 0) {
                    check = 0;
                } else {
                    ++check;
                }
                Thread.sleep(200);
            }
        } catch (AlreadyClosedException e) {
            LOGGER.info("The queue is already closed. Assuming that all messages have been consumed.");
        } catch (Exception e) {
            LOGGER.warn("Exception while trying to check whether all messages have been consumed. It will be ignored.",
                    e);
        }
        close();
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(queue);
    }

    public static final class Builder {

        private static final String QUEUE_INFO_MISSING_ERROR = "There are neither a queue nor a queue name and a queue factory provided for the DataSender. Either a queue or a name and a factory to create a new queue are mandatory.";

        private IdGenerator idGenerator = new RandomIdGenerator();
        private RabbitQueue queue;
        private String queueName;
        private RabbitQueueFactory factory;
        private int messageSize = DEFAULT_MESSAGE_SIZE;
        private int deliveryMode = DEFAULT_DELIVERY_MODE;

        public Builder() {
        };

        /**
         * Sets the Id generator used to create unique stream Ids.
         * 
         * @param dataHandler
         *            the Id generator used to create unique stream Ids
         * @return this builder instance
         */
        public Builder idGenerator(IdGenerator idGenerator) {
            this.idGenerator = idGenerator;
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
         * Sets the size of the messages that will be send. Note that the
         * maximum size of a message can be 2 * the given value.
         * 
         * @param messageSize
         *            the size of the messages that will be send
         * @return this builder instance
         */
        public Builder messageSize(int messageSize) {
            this.messageSize = messageSize;
            return this;
        }

        /**
         * Sets the delivery mode used for the RabbitMQ messages. Please have a
         * look into the RabbitMQ documentation to see the different meanings of
         * the values. By default, the sender uses
         * {@link DataSenderImpl#DEFAULT_DELIVERY_MODE}.
         * 
         * @param deliveryMode
         *            the delivery mode used for the RabbitMQ messages
         * @return this builder instance
         */
        public Builder deliveryMode(int deliveryMode) {
            this.deliveryMode = deliveryMode;
            return this;
        }

        /**
         * Builds the {@link DataReceiverImpl} instance with the previously
         * given information.
         * 
         * @return The newly created DataReceiver instance
         * @throws IllegalStateException
         *             if neither a queue nor the information needed to create a
         *             queue have been provided.
         * @throws IOException
         *             if an exception is thrown while creating a new queue.
         */
        public DataSenderImpl build() throws IllegalStateException, IOException {
            if (queue == null) {
                if ((queueName == null) || (factory == null)) {
                    throw new IllegalStateException(QUEUE_INFO_MISSING_ERROR);
                } else {
                    queue = factory.createDefaultRabbitQueue(queueName);
                }
            }
            return new DataSenderImpl(queue, idGenerator, messageSize, deliveryMode);
        }
    }

}
