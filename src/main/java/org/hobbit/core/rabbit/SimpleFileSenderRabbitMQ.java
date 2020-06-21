package org.hobbit.core.rabbit;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.data.RabbitQueue;

import com.rabbitmq.client.MessageProperties;

public class SimpleFileSenderRabbitMQ implements Closeable {
	private static final int DEFAULT_MESSAGE_SIZE = 65536;

    public static SimpleFileSenderRabbitMQ create(RabbitQueueFactory factory, String queueName) throws IOException {
        return new SimpleFileSenderRabbitMQ(factory.createDefaultRabbitQueue(queueName));
    }

    private RabbitQueue queue;
    private int messageSize = DEFAULT_MESSAGE_SIZE;

    protected SimpleFileSenderRabbitMQ(RabbitQueue queue) {
        this.queue = queue;
    }

    public void streamData(InputStream is, String name) throws IOException {
        int messageId = 0;
        int length = 0;
        byte[] nameBytes = RabbitMQUtils.writeString(name);
        byte[] array = new byte[messageSize + nameBytes.length + 8];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
        int messageIdPos = buffer.position();
        int dataStartPos = messageIdPos + 4;
        do {
            buffer.position(messageIdPos);
            buffer.putInt(messageId);
            length = is.read(array, dataStartPos, array.length - dataStartPos);
            queue.channel.basicPublish("", queue.name, MessageProperties.MINIMAL_PERSISTENT_BASIC,
                    Arrays.copyOf(array, (length > 0) ? (dataStartPos + length) : dataStartPos));
            ++messageId;
        } while (length > 0);
    }

    public void setMessageSize(int messageSize) {
        this.messageSize = messageSize;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(queue);
    }
}
