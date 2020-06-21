package org.hobbit.core.components.commonchannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.hobbit.core.components.AbstractCommandReceivingComponent;

import com.rabbitmq.client.AMQP.BasicProperties;
/**
 * Interface implemented by RabbitMQChannel and DirectChannel
 * @author altaf, sourabh, yamini, melisa
 *
 */
public interface CommonChannel {

	public void readBytes(Object consumerCallback, Object classs, Boolean autoAck, String queue) throws IOException;

	public void writeBytes(byte data[], String exchange, String routingKey, BasicProperties props) throws IOException;

	public void writeBytes(ByteBuffer buffer, String exchange, String routingKey, BasicProperties props) throws IOException;

	public void close();

	public void createChannel() throws Exception;

	public String getQueueName(AbstractCommandReceivingComponent abstractCommandReceivingComponent)  throws Exception;
	
	public void exchangeDeclare(String exchange, String type, boolean durable, boolean autoDelete,
            Map<String, Object> arguments) throws IOException;
	
	public void queueBind(String queue, String exchange, String routingKey) throws IOException;

	public Object getChannel();

	public String declareQueue(String queueName) throws IOException;
}
