package org.hobbit.core.components.communicationfactory;

import org.hobbit.core.Constants;
import org.hobbit.core.com.DataHandler;
import org.hobbit.core.com.DataReceiver;
import org.hobbit.core.com.DataSender;
import org.hobbit.core.com.java.DirectChannel;
import org.hobbit.core.com.java.DirectReceiverImpl;
import org.hobbit.core.com.java.DirectSenderImpl;
import org.hobbit.core.components.AbstractPlatformConnectorComponent;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.rabbit.DataSenderImpl;
import org.hobbit.core.rabbit.RabbitMQChannel;
/**
 * This factory class provides the instance of {@link DataSender} and {@link DataReceiver} 
 * for {@link RabbitMQChannel} or {@link DirectChannel} based on the environment property
 * {@link org.hobbit.core.Constants#IS_RABBIT_MQ_ENABLED}
 * @author Altafhusen Makandar
 *
 */
public class SenderReceiverFactory {
    
    /**
     * Factory method to fetch the instance of {@link DataSenderImpl} or {@link DirectSenderImpl}
     * based on the environment configuration {@link Constants#IS_RABBIT_MQ_ENABLED}
     */
    public static DataSender getSenderImpl(boolean isRabbitEnabled, String queue, AbstractPlatformConnectorComponent object) throws Exception {
        if(isRabbitEnabled) {
            return DataSenderImpl.builder().queue(((RabbitMQChannel)object.getFactoryForOutgoingDataQueues()).getCmdQueueFactory(),
                queue).build();
        }
        return new DirectSenderImpl(queue);
    }

    /**
     * Factory method to fetch the instance of {@link DataReceiverImpl} or {@link DirectReceiverImpl}
     * based on the environment configuration {@link Constants#IS_RABBIT_MQ_ENABLED}
     */
    public static DataReceiver getReceiverImpl(boolean isRabbitEnabled, String queue, Object consumer, 
    	    int maxParallelProcessedMsgs, AbstractPlatformConnectorComponent object ) throws Exception {
        if(isRabbitEnabled) {
            return DataReceiverImpl.builder().maxParallelProcessedMsgs(maxParallelProcessedMsgs).
                queue(((RabbitMQChannel)object.getFactoryForIncomingDataQueues()).getCmdQueueFactory(),
                queue).dataHandler((DataHandler) consumer).build();
        }
        return new DirectReceiverImpl(queue, consumer);
    }

}
