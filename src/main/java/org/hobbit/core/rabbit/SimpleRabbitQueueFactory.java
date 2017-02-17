package org.hobbit.core.rabbit;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.hobbit.core.data.RabbitQueue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class SimpleRabbitQueueFactory implements RabbitQueueFactory, Closeable {

    public static SimpleRabbitQueueFactory create(String rabbitHost) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitHost);
        return new SimpleRabbitQueueFactory(factory.newConnection());
    }

    private Connection connection;

    public SimpleRabbitQueueFactory(Connection connection) {
        this.connection = connection;
    }

    @Override
    public RabbitQueue createDefaultRabbitQueue(String name) throws IOException {
        Channel channel = connection.createChannel();
        channel.queueDeclare(name, false, false, true, null);
        return new RabbitQueue(channel, name);
    }

    @Override
    public void close() throws IOException {
        if (connection != null) {
            connection.close();
        }
    }

}
