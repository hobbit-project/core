package org.hobbit.core.rabbit;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.hobbit.core.components.AbstractTaskGenerator;

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
	
	public static DataReceiver getReceiverImpl(String rabbitEnabled, AbstractTaskGenerator abstractTaskGenerator, String queue) {
		if(!StringUtils.isEmpty(rabbitEnabled) && rabbitEnabled.equals("true")) {
			try {
				return new DataReceiverImpl.Builder().build();
			} catch (IllegalStateException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new DirectReceiverImpl(abstractTaskGenerator, queue);
		
	}

}
