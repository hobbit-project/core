package org.hobbit.core.components.communicationfactory;

import org.hobbit.core.Constants;
import org.hobbit.core.com.CommonChannel;
import org.hobbit.core.com.java.DirectChannel;
import org.hobbit.core.rabbit.RabbitMQChannel;

import com.rabbitmq.client.ConnectionFactory;
/**
 * This factory class provides an instance of RabbitMQChannel or DirectChannel
 * based on the environment property {@link org.hobbit.core.Constants#IS_RABBIT_MQ_ENABLED}
 * @author altaf, sourabh, yamini, melisa
 *
 */
public class ChannelFactory {

    /**
     * Factory method to get the instance of {@link RabbitMQChannel} or {@link DirectChannel}
     * based on the environment configuration {@link Constants#IS_RABBIT_MQ_ENABLED}
     */
    public CommonChannel getChannel(boolean rabbitMQEnabled, String queue, ConnectionFactory connectionFactory) {
        if(rabbitMQEnabled){
            return new RabbitMQChannel(connectionFactory);
        }
        return new DirectChannel(queue);
    }
    
}
