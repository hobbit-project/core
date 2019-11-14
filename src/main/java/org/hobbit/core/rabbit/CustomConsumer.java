package org.hobbit.core.rabbit;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;

public class CustomConsumer extends DefaultConsumer {
    
    private LinkedBlockingQueue<Delivery> deliveryQueue;
    
    
    public CustomConsumer(Channel channel) {
    	this(channel, new LinkedBlockingQueue<Delivery>());
    }
    
    public CustomConsumer(Channel channel, LinkedBlockingQueue<Delivery> deliveryQueue) {
        super(channel);
        this.deliveryQueue = deliveryQueue;
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

	public LinkedBlockingQueue<Delivery> getDeliveryQueue() {
		return deliveryQueue;
	}
    
}
