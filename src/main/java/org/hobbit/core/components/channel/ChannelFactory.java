package org.hobbit.core.components.channel;

import com.rabbitmq.client.DefaultConsumer;

public class ChannelFactory {
	
    public CommonChannel getChannel(String rabbitMQEnabled) {
        if(rabbitMQEnabled.equals("true")){
            return new RabbitMQChannel();
        }
        return new DirectChannel();
    }
    
    public Object getConsumerCallback(String rabbitMQEnabled) {
    	if(rabbitMQEnabled.equals("true")) {
    		return null;
    	}
    	return null;
    }
}
