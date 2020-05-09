package org.hobbit.core.components.channel;

public class AbstractChannel {
    public CommonChannel getChannel(String string) {
        if(string.equals("true")){
            return new RabbitMQChannel();
        }
        return new DirectChannel();
    }
}
