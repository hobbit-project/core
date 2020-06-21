package org.hobbit.core.components.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.rabbitmq.client.AMQP.BasicProperties;
/**
 * For each readByte call for DirectChannel implementation,
 * a thread of ReadByteChannel is created. This makes sure that
 * the execution takes place on a separate thread by waiting for 
 * messages to be received by the pipe.
 * @author altaf, sourabh, yamini, melisa
 *
 */
public class ReadByteChannel extends DirectChannel implements Runnable{

	PipeChannel pipeChannel;
	DirectCallback callback;
	public static ArrayList<Object> classes = new ArrayList<>();
	private ExecutorService threadPool = Executors.newCachedThreadPool();

	public ReadByteChannel(PipeChannel pipeChannel, Object callback, Object classs) {
		this.pipeChannel = pipeChannel;
		this.callback = (DirectCallback) callback;
        classes.add(classs);
	}

	@Override
	public void run() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
        	if(pipeChannel != null && pipeChannel.getPipe() !=null) {
        		while(pipeChannel.getPipe().source().isOpen() && 
        				pipeChannel.getPipe().source().read(buffer) > 0){
        			threadPool.execute(new ProcessCallback(callback, clone(buffer), pipeChannel.getProps()));
        			buffer.clear();
        		}
        	}
        } catch (IOException e) {
            e.printStackTrace();
        }

	}
	
	
	
	public ByteBuffer clone(ByteBuffer original) {
	       ByteBuffer clone = ByteBuffer.allocate(original.capacity());
	       original.rewind();
	       clone.put(original);
	       original.rewind();
	       clone.flip();
	       return clone;
	}
	
	protected class ProcessCallback implements Runnable {
		DirectCallback callbackObj;
		ByteBuffer byteBuffer;
		BasicProperties props;
		
		ProcessCallback(DirectCallback callback, ByteBuffer byteBuffer, BasicProperties props){
			this.callbackObj = callback;
			this.byteBuffer = byteBuffer;
			this.props = props;
		}

		@Override
		public void run() {
			callbackObj.callback(getNonEmptyArray(byteBuffer), classes, props);
		}
		
		public byte[] getNonEmptyArray(ByteBuffer buffer) {
			byte[] inputArray = buffer.array().clone();
			int i = inputArray.length - 1;
		    while (i >= 0 && inputArray[i] == 0) {
		        --i;
		    }
		    return Arrays.copyOf(inputArray, i + 1);
		}
	}

}
