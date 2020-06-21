package org.hobbit.core.components.channel;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.hobbit.core.components.commonchannel.CommonChannel;
import org.hobbit.core.components.communicationfactory.ChannelFactory;
import org.hobbit.core.data.handlers.DataSender;
/**
 * DataSender implementation for DirectChannel
 * @author altaf, sourabh, yamini, melisa
 *
 */
public class DirectSenderImpl implements DataSender {

	CommonChannel senderChannel;
	String queue;

	public DirectSenderImpl(String queue){
		this.queue = queue;
		senderChannel = new ChannelFactory().getChannel(false, queue, null);
	}


	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendData(byte[] data) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(data.length);
    	buffer.put(data);
    	sleep(100000);
		senderChannel.writeBytes(buffer, null, this.queue, null);
    	/*try {
			Thread.sleep(0, 100);
			sleep(100000);
			senderChannel.writeBytes(buffer, null, this.queue, null);
		} catch (Exception e) {
            LOGGER.error("Error waiting during send data", e);
		}*/

	}

	private void sleep(long interval) {
		long start = System.nanoTime();
		long totalTime = start + interval;
		long end = 0;
		do {
			end = System.nanoTime();
		}while(totalTime >= end);
	}


	@Override
	public void closeWhenFinished() {
		// TODO Auto-generated method stub

	}

}
