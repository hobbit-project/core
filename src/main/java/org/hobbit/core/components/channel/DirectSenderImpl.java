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
		senderChannel = new ChannelFactory().getChannel(EnvVariables.getString(Constants.IS_RABBIT_MQ_ENABLED, LOGGER), queue);
	}
	

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendData(byte[] data) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(6);
    	buffer.put(data);
		senderChannel.writeBytes(buffer, this.queue);

	}

	@Override
	public void closeWhenFinished() {
		// TODO Auto-generated method stub

	}

}
