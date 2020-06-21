package org.hobbit.core.components.channel;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.hobbit.core.Constants;
import org.hobbit.core.components.commonchannel.CommonChannel;
import org.hobbit.core.components.communicationfactory.ChannelFactory;
import org.hobbit.core.data.handlers.DataSender;
import org.hobbit.utils.EnvVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectSenderImpl implements DataSender {

	private static final Logger LOGGER = LoggerFactory.getLogger(DirectSenderImpl.class);

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
    	try {
			Thread.sleep(0, 100);
			senderChannel.writeBytes(buffer, null, this.queue, null);
		} catch (InterruptedException e) {
            LOGGER.error("Error waiting during send data", e);
		}

	}

	@Override
	public void closeWhenFinished() {
		// TODO Auto-generated method stub

	}

}
