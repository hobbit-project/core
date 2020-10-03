package org.hobbit.core.com;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.hobbit.core.components.AbstractCommandReceivingComponent;

import com.rabbitmq.client.AMQP.BasicProperties;
import org.hobbit.core.rabbit.RabbitMQChannel;
import org.hobbit.core.com.java.DirectChannel;

/**
 * An interface with several methods that is implemented by {@link RabbitMQChannel} and {@link DirectChannel}.
 *
 * @author Altafhusen Makandar
 * @author Sourabh Poddar
 * @author Yamini Punetha
 * @author Melissa Das
 *
 */
public interface Channel {

    /**
     * Method to accept messages from the channel.
     */
    public void readBytes(Object consumerCallback, Object classs, Boolean autoAck, String queue) throws IOException;

    /**
     * Method to publish a message of type Byte array the channel.
     */
    public void writeBytes(byte data[], String exchange, String routingKey, BasicProperties props) throws IOException;

    /**
     * Method to publish a message of type ByteBuffer to the channel.
     */
    public void writeBytes(ByteBuffer buffer, String exchange, String routingKey, BasicProperties props) throws IOException;

    /**
     * Method to close a channel.
     */
    public void close();

    /**
     * Method to create a new channel.
     */
    public void createChannel() throws Exception;

    /**
     * Method to get the queue name.
     */
    public String getQueueName(AbstractCommandReceivingComponent abstractCommandReceivingComponent)  throws Exception;

    /**
     * Method to create an exchange for the data transfer for a broker
     */
    public void exchangeDeclare(String exchange, String type, boolean durable, boolean autoDelete,
        Map<String, Object> arguments) throws IOException;
	
    /**
     * Method to bind a queue with an exchange. This allows an exchange to publish the messages 
     * to the queues that are bound to this exchange 
     */
    public void queueBind(String queue, String exchange, String routingKey) throws IOException;

    /**
     * Method to return the channel instance.
     */
    public Object getChannel();

    public String declareQueue(String queueName) throws IOException;

}
