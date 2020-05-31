package org.hobbit.core.components.channel;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.components.commonchannel.CommonChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;

public class DirectChannel implements CommonChannel {

	private static final Logger LOGGER = LoggerFactory.getLogger(DirectChannel.class);
	
	static Map<String, PipeChannel> pipes = new HashMap<>();

    //Pipe pipe;
    PipeChannel pipeChannel;
    //WritableByteChannel out;
    //ReadableByteChannel in;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    public DirectChannel(){}
    public DirectChannel(String queue){
        try {
        	if(pipes.get(queue) == null) {
        		//Pipe pipe= Pipe.open();
        		//out = pipe.sink();
        		//in = pipe.source();
        		pipeChannel = new PipeChannel(Pipe.open());
        		pipes.put(queue, pipeChannel);
        	}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public byte[] readBytes(Object callback, Object classs, String queue) {
    	threadPool.execute(new ReadByteChannel(pipes.get(queue), callback, classs));
        return null;
    }

    @Override
    public void writeBytes(byte[] data) {

        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put(data);
        try {
            System.out.println("\nINSIDE writeBytes ");
            //out.write(buffer);
            buffer.clear();
           // System.out.println(buffer.toString()+"\n\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeBytes(ByteBuffer buffer, String queue, BasicProperties props) {

        try {
        	if(!pipes.isEmpty()) {
        		pipes.get(queue).setProps(props);
        		String replyQueue = queue;
        		if(props != null && StringUtils.isNotBlank(props.getReplyTo())) {
        			replyQueue = props.getReplyTo();
        		}
        		if(pipes.get(replyQueue).getPipe().sink().isOpen()) {
        			buffer.flip();
        			System.out.println("\nINSIDE writeBytes ");
        			while (buffer.hasRemaining())
        				pipes.get(replyQueue).getPipe().sink().write(buffer);
        			buffer.clear();
        		}
        	}
            //System.out.println(buffer+"\n\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	@Override
	public void close() {
		if(ReadByteChannel.classes != null && ReadByteChannel.classes.size() > 0) {
			ReadByteChannel.classes.clear();
		}
		pipes.clear();
		/*if(threads != null && threads.size() > 0) {
			for(Thread t:threads) {
				t.stop();
			}
		}*/
		/*
		 * if(pipes != null && pipes.size() > 0) { for(Map.Entry<String, PipeChannel>
		 * entry : pipes.entrySet()) { try { entry.getValue().getPipe().sink().close();
		 * } catch (IOException e) { LOGGER.error("Error closing pipe",e); } } }
		 */
		
	}
}
