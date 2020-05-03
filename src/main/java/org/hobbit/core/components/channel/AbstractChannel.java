package org.hobbit.core.components.channel;

import org.hobbit.core.components.AbstractComponent;

public abstract class AbstractChannel {
    public static AbstractChannel getChannel(String string) {
        if(string.equals("true")){
            return new RabbitMQChannel();
        }
        return new DirectChannel();
    }

    public void createConnectionFactory(Object comp){}

    public void setIncomingDataQueueFactory(AbstractComponent abstractComponent) throws Exception {}

    public void setOutgoingDataQueueFactory(AbstractComponent abstractComponent) throws Exception{}
}
