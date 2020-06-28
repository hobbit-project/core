package org.hobbit.core.rabbit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.hobbit.core.com.CommonChannel;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

public class RabbitMQChannel implements CommonChannel {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQChannel.class);
	
    protected ConnectionFactory connectionFactory;
	
    protected String rabbitMQHostName;
	
    protected RabbitQueueFactory cmdQueueFactory;
	
    protected Channel cmdChannel = null;
	
    private String queueName;
    /**
     * Maximum number of retries that are executed to connect to RabbitMQ.
     */
    public static final int NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ = 5;
    /**
     * Time, the system waits before retrying to connect to RabbitMQ. Note that this
     * time will be multiplied with the number of already failed tries.
     */
    public static final long START_WAITING_TIME_BEFORE_RETRY = 5000;
	
    public RabbitMQChannel(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
	}

    @Override
    public void readBytes(Object callback, Object classs, Boolean autoAck, String queue) throws IOException {
        if(autoAck == null) {
            cmdChannel.basicConsume(queue, (Consumer) callback);
        }else {
            cmdChannel.basicConsume(queue, autoAck, (Consumer) callback);
        }
    }

    @Override
    public void writeBytes(byte[] data, String exchange, String routingKey, BasicProperties props) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.put(data);
        writeBytes(buffer, exchange, routingKey, props);
    }

    @Override
    public void writeBytes(ByteBuffer buffer, String exchange, String routingKey, BasicProperties props) throws IOException {
        cmdChannel.basicPublish(exchange, routingKey, props, buffer.array());
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(cmdQueueFactory);
    }
	
    protected Connection createConnection() throws Exception {
        Connection connection = null;
        Exception exception = null;
        for (int i = 0; (connection == null) && (i <= NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ); ++i) {
            try {
                connection = connectionFactory.newConnection();
            } catch (Exception e) {
                if (i < NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ) {
                    long waitingTime = START_WAITING_TIME_BEFORE_RETRY * (i + 1);
                    LOGGER.warn("Couldn't connect to RabbitMQ with try #" + i + ". Next try in " + waitingTime + "ms.");
                    exception = e;
                    try {
                        Thread.sleep(waitingTime);
                    } catch (Exception e2) {
                        LOGGER.warn("Interrupted while waiting before retrying to connect to RabbitMQ.", e2);
                    }
                }
            }
        }
        if (connection == null) {
            String msg = "Couldn't connect to RabbitMQ after " + NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ
                + " retries.";
            LOGGER.error(msg, exception);
            throw new Exception(msg, exception);
        }
        return connection;
    }

    @Override
    public void createChannel() throws Exception {
        cmdQueueFactory = new RabbitQueueFactoryImpl(createConnection());
        cmdChannel = cmdQueueFactory.getConnection().createChannel();
    }

    @Override
    public String getQueueName(AbstractCommandReceivingComponent abstractCommandReceivingComponent) throws Exception {
        return this.queueName;
    }

    @Override
    public void exchangeDeclare(String exchange, String type, boolean durable, boolean autoDelete,
    	    Map<String, Object> arguments) throws IOException {
        cmdChannel.exchangeDeclare(exchange, type, durable, autoDelete, arguments);
    }

    @Override
    public void queueBind(String queue, String exchange, String routingKey) throws IOException {
        cmdChannel.queueBind(queue, exchange, routingKey);
    }

    @Override
    public Object getChannel() {
        return cmdChannel;
    }
	
    public RabbitQueueFactory getCmdQueueFactory() {
        return cmdQueueFactory;
    }

    @Override
    public String declareQueue(String queueName) throws IOException {
        this.queueName = queueName;
        if(queueName != null) {
            cmdChannel.queueDeclare(queueName, false, false, true, null);
        } else {
            this.queueName = cmdChannel.queueDeclare().getQueue();
        }
        return this.queueName;
    }
}