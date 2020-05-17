package org.hobbit.core.components.channel;

import com.rabbitmq.client.DefaultConsumer;

public class ChannelFactory {
	
    public CommonChannel getChannel(String rabbitMQEnabled, String queue) {
        if(rabbitMQEnabled.equals("true")){
            return new RabbitMQChannel();
        }
        return new DirectChannel(queue);
    }
    
    public Object getConsumerCallback(String rabbitMQEnabled) {
    	if(rabbitMQEnabled.equals("true")) {
    		return null;
    	}
    	return null;
    }
}
