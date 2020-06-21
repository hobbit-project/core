package org.hobbit.core.components.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.components.commonchannel.CommonChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
/**
 * This class implements the necessary functionality for message sharing 
 * without using RabbitMQ. DirectChannel uses Java NIO Pipe for message queuing
 * implementation 
 * @author altaf, sourabh, yamini, melisa
 *
 */
public class DirectChannel implements CommonChannel {

	private static final Logger LOGGER = LoggerFactory.getLogger(DirectChannel.class);
	
	static Map<String, PipeChannel> pipes = new HashMap<>();

    PipeChannel pipeChannel;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    public DirectChannel(){}
    public DirectChannel(String queue){
        try {
        	if(pipes.get(queue) == null) {
        		pipeChannel = new PipeChannel(Pipe.open());
        		pipes.put(queue, pipeChannel);
        	}
        } catch (IOException e) {
            LOGGER.error("Error creating pipe ",e);
        }
    }


    @Override
    public void readBytes(Object callback, Object classs, Boolean autoAck, String queue) {
    	threadPool.execute(new ReadByteChannel(pipes.get(queue), callback, classs));
    }

    @Override
    public void writeBytes(byte[] data, String exchange, String routingKey, BasicProperties props) throws IOException {
    	ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.put(data);
        writeBytes(buffer, exchange, routingKey, props);
    }

    @Override
    public void writeBytes(ByteBuffer buffer, String exchange, String routingKey, BasicProperties props) throws IOException {
    	String queue = StringUtils.isEmpty(exchange) ? routingKey : exchange;
    	if(!pipes.isEmpty()) {
    		pipes.get(queue).setProps(props);
    		String replyQueue = queue;
    		if(props != null && StringUtils.isNotBlank(props.getReplyTo())) {
    			replyQueue = props.getReplyTo();
    		}
    		if(pipes.get(replyQueue).getPipe().sink().isOpen()) {
    			buffer.flip();
    			while (buffer.hasRemaining())
    				pipes.get(replyQueue).getPipe().sink().write(buffer);
    			buffer.clear();
    		}
    	}
    }

	@Override
	public void close() {
		/*if(ReadByteChannel.classes != null && ReadByteChannel.classes.size() > 0) {
			ReadByteChannel.classes.clear();
		}
		pipes.clear();*/
		
	}
	
	@Override
	public void createChannel() throws Exception {
		
	}
	@Override
	public String getQueueName(AbstractCommandReceivingComponent abstractCommandReceivingComponent) throws Exception {
		return null;
	}
	@Override
	public void exchangeDeclare(String exchange, String type, boolean durable, boolean autoDelete,
			Map<String, Object> arguments) throws IOException {
		
	}
	@Override
	public void queueBind(String queue, String exchange, String routingKey) throws IOException {
		
	}
	@Override
	public Object getChannel() {
		return null;
	}
	@Override
	public String declareQueue(String queueName) throws IOException {
		return queueName;
	}
}
