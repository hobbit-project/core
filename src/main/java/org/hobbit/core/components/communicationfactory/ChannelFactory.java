package org.hobbit.core.components.communicationfactory;

import org.hobbit.core.components.channel.DirectChannel;
import org.hobbit.core.components.commonchannel.CommonChannel;
import org.hobbit.core.rabbit.RabbitMQChannel;

import com.rabbitmq.client.ConnectionFactory;

public class ChannelFactory {
	
    public CommonChannel getChannel(boolean rabbitMQEnabled, String queue, ConnectionFactory connectionFactory) {
        if(rabbitMQEnabled){
            return new RabbitMQChannel(connectionFactory);
        }
        return new DirectChannel(queue);
    }
    
}
