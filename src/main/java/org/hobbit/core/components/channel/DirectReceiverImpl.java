package org.hobbit.core.components.channel;

import java.io.IOException;
import java.util.List;

import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractPlatformConnectorComponent;
import org.hobbit.core.components.AbstractTaskGenerator;
import org.hobbit.core.components.commonchannel.CommonChannel;
import org.hobbit.core.components.communicationfactory.ChannelFactory;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.data.handlers.DataHandler;
import org.hobbit.core.data.handlers.DataReceiver;
import org.hobbit.utils.EnvVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectReceiverImpl implements DataReceiver {

	private static final Logger LOGGER = LoggerFactory.getLogger(DirectReceiverImpl.class);

	public DirectReceiverImpl(String queue, Object consumer) {

		CommonChannel channel = new ChannelFactory().getChannel(false, queue, null);
		try {
			channel.readBytes(consumer, this, null, queue);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getErrorCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void closeWhenFinished() {
		// TODO Auto-generated method stub

	}

	@Override
	public void increaseErrorCount() {
		// TODO Auto-generated method stub

	}

	@Override
	public DataHandler getDataHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RabbitQueue getQueue() {
		// TODO Auto-generated method stub
		return null;
	}

}
