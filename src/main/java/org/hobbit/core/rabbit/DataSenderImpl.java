package org.hobbit.core.rabbit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.components.RabbitQueueFactory;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.utils.IdGenerator;
import org.hobbit.core.utils.RandomIdGenerator;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;

public class DataSenderImpl implements DataSender {

    private static final int DEFAULT_MESSAGE_SIZE = 65536;

    public static DataSenderImpl create(RabbitQueueFactory factory, String queueName) throws IOException {
        return new DataSenderImpl(factory.createDefaultRabbitQueue(queueName));
    }

    private IdGenerator idGenerator = new RandomIdGenerator();
    private RabbitQueue queue;
    private int messageSize = DEFAULT_MESSAGE_SIZE;
    private int maxMessageSize = 2 * messageSize;
    private int deliveryMode = 2;

    protected DataSenderImpl(RabbitQueue queue) {
        this.queue = queue;
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
        BasicProperties.Builder probBuilder = new Builder();
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
    public void close() {
        IOUtils.closeQuietly(queue);
    }

}
