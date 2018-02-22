package org.hobbit.core.rabbit.paired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.rabbit.DataSenderImpl;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.RabbitQueueFactory;
import org.hobbit.core.utils.IdGenerator;

import com.rabbitmq.client.AMQP.BasicProperties;

public class PairedDataSender extends DataSenderImpl {

    protected PairedDataSender(RabbitQueue queue, IdGenerator idGenerator, int messageSize, int deliveryMode,
            int messageConfirmBuffer, String name) {
        super(queue, idGenerator, messageSize, deliveryMode, messageConfirmBuffer, name);
    }

    public void sendData(InputStream[] is) throws IOException {
        sendData(is, idGenerator.getNextId());
    }

    public void sendData(InputStream[] is, String dataId) throws IOException {
        String streamIds[] = new String[is.length];
        byte streamIdBytes[][] = new byte[is.length][];
        for (int i = 0; i < is.length; ++i) {
            streamIds[i] = idGenerator.getNextId();
            streamIdBytes[i] = RabbitMQUtils.writeString(streamIds[i]);
        }
        Arrays.sort(streamIds);
        // create head message
        BasicProperties.Builder probBuilder = new BasicProperties.Builder();
        probBuilder.clusterId(dataId);
        InputStream headStream = new ByteArrayInputStream(RabbitMQUtils.writeByteArrays(streamIdBytes));
        sendData(headStream, dataId, probBuilder);
        IOUtils.closeQuietly(headStream);
        // send other streams
        for (int i = 0; i < is.length; ++i) {
            sendData(is[i], streamIds[i]);
        }
    }

    /**
     * Returns a newly created {@link Builder}.
     * 
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends DataSenderImpl.Builder {

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
         * <p>
         * Sets the number of messages that are buffered while waiting for a
         * confirmation that they have been received by the broker. Note that if
         * the message buffer has reached is maximum size, the sender will block
         * until confirmations are received.
         * </p>
         * <p>
         * If the given message buffer size is {@code <1} the usage of
         * confirmation messages is turned off.
         * </p>
         * 
         * @param messageConfirmBuffer
         *            the size of the messages buffer
         * @return this builder instance
         */
        public Builder messageBuffer(int messageConfirmBuffer) {
            this.messageConfirmBuffer = messageConfirmBuffer;
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

        public Builder name(String name) {
            this.name = name;
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
        public PairedDataSender build() throws IllegalStateException, IOException {
            if (queue == null) {
                if ((queueName == null) || (factory == null)) {
                    throw new IllegalStateException(QUEUE_INFO_MISSING_ERROR);
                } else {
                    queue = factory.createDefaultRabbitQueue(queueName);
                }
            }
            return new PairedDataSender(queue, idGenerator, messageSize, deliveryMode, messageConfirmBuffer, name);
        }
    }

}
