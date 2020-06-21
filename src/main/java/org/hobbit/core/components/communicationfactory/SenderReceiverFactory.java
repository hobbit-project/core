package org.hobbit.core.components.communicationfactory;

import java.io.IOException;
import org.hobbit.core.components.AbstractPlatformConnectorComponent;
import org.hobbit.core.components.channel.DirectChannel;
import org.hobbit.core.components.channel.DirectReceiverImpl;
import org.hobbit.core.components.channel.DirectSenderImpl;
import org.hobbit.core.data.handlers.DataHandler;
import org.hobbit.core.data.handlers.DataReceiver;
import org.hobbit.core.data.handlers.DataSender;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.rabbit.DataSenderImpl;
import org.hobbit.core.rabbit.RabbitMQChannel;
/**
 * This factory class provides the instance of {@link DataSender} and {@link DataReceiver} 
 * for {@link RabbitMQChannel} or {@link DirectChannel} based on the environment property
 * {@link org.hobbit.core.Constants#IS_RABBIT_MQ_ENABLED}
 * @author altaf
 *
 */
public class SenderReceiverFactory {
	
	public static DataSender getSenderImpl(boolean isRabbitEnabled, String queue, AbstractPlatformConnectorComponent object) {
		if(isRabbitEnabled) {
			try {
				return DataSenderImpl.builder().queue(((RabbitMQChannel)object.getFactoryForOutgoingDataQueues()).getCmdQueueFactory(),
		                queue).build();
			} catch (IllegalStateException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new DirectSenderImpl(queue);
	}
	
	public static DataReceiver getReceiverImpl(boolean isRabbitEnabled, String queue, Object consumer, 
			int maxParallelProcessedMsgs, AbstractPlatformConnectorComponent object ) {
		if(isRabbitEnabled) {
			try {
				return DataReceiverImpl.builder().maxParallelProcessedMsgs(maxParallelProcessedMsgs).
						queue(((RabbitMQChannel)object.getFactoryForIncomingDataQueues()).getCmdQueueFactory(),
		                queue).dataHandler((DataHandler) consumer).build();
			} catch (IllegalStateException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new DirectReceiverImpl(queue, consumer);
		
	}

}
