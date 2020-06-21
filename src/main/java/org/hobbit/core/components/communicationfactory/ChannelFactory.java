package org.hobbit.core.components.communicationfactory;

import org.hobbit.core.components.channel.DirectChannel;
import org.hobbit.core.components.commonchannel.CommonChannel;
import org.hobbit.core.rabbit.RabbitMQChannel;

import com.rabbitmq.client.ConnectionFactory;
/**
 * This factory class provides an instance of RabbitMQChannel or DirectChannel
 * based on the environment property {@link org.hobbit.core.Constants#IS_RABBIT_MQ_ENABLED}
 * @author altaf, sourabh, yamini, melisa
 *
 */
public class ChannelFactory {
	
    public CommonChannel getChannel(boolean rabbitMQEnabled, String queue, ConnectionFactory connectionFactory) {
        if(rabbitMQEnabled){
            return new RabbitMQChannel(connectionFactory);
        }
        return new DirectChannel(queue);
    }
    
}
