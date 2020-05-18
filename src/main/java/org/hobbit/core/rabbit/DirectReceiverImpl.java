package org.hobbit.core.rabbit;

import java.io.IOException;
import java.util.List;

import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractPlatformConnectorComponent;
import org.hobbit.core.components.AbstractTaskGenerator;
import org.hobbit.core.components.channel.ChannelFactory;
import org.hobbit.core.components.channel.CommonChannel;
import org.hobbit.core.components.channel.DirectCallback;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.utils.EnvVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectReceiverImpl implements DataReceiver {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DirectReceiverImpl.class);
	
	public DirectReceiverImpl(String queue, Object consumer) {
		
		CommonChannel channel = new ChannelFactory().getChannel(EnvVariables.getString(Constants.IS_RABBIT_MQ_ENABLED, LOGGER), Constants.DATA_GEN_2_TASK_GEN_QUEUE_NAME);
		channel.readBytes(consumer, this, queue);
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
