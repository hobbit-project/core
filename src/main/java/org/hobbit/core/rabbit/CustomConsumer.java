package org.hobbit.core.rabbit;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
public class CustomConsumer extends DefaultConsumer {
    
    public LinkedBlockingQueue<Delivery> deliveryQueue = new LinkedBlockingQueue<>();
    public CustomConsumer(Channel channel) {
        super(channel);
    }
    @Override
   public void handleDelivery(String consumerTag,
                              Envelope envelope,
                              AMQP.BasicProperties properties,
                              byte[] body)
       throws IOException
   {
        deliveryQueue.add(new Delivery(envelope,properties,body));
   }
}
