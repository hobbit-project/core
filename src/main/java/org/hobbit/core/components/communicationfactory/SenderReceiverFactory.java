package org.hobbit.core.components.communicationfactory;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.hobbit.core.components.AbstractPlatformConnectorComponent;
import org.hobbit.core.components.AbstractTaskGenerator;
import org.hobbit.core.components.channel.DirectReceiverImpl;
import org.hobbit.core.components.channel.DirectSenderImpl;
import org.hobbit.core.data.handlers.DataReceiver;
import org.hobbit.core.data.handlers.DataSender;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.rabbit.DataSenderImpl;
import org.hobbit.core.rabbit.DataReceiverImpl.Builder;

public class SenderReceiverFactory {
	
	public static DataSender getSenderImpl(String rabbitEnabled, String queue) {
		if(!StringUtils.isEmpty(rabbitEnabled) && rabbitEnabled.equals("true")) {
			try {
				return new DataSenderImpl.Builder().build();
			} catch (IllegalStateException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new DirectSenderImpl(queue);
	}
	
	public static DataReceiver getReceiverImpl(String rabbitEnabled, String queue, Object consumer) {
		if(!StringUtils.isEmpty(rabbitEnabled) && rabbitEnabled.equals("true")) {
			try {
				return new DataReceiverImpl.Builder().build();
			} catch (IllegalStateException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new DirectReceiverImpl( queue, consumer);
		
	}

}
