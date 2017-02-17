package org.hobbit.core.rabbit.consume;

import org.hobbit.core.rabbit.DataReceiver;
import org.hobbit.core.rabbit.DataReceiverImpl;

import com.rabbitmq.client.Channel;

/**
 * Interface of a builder class that can create a {@link MessageConsumer} instance.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface MessageConsumerBuilder {

    /**
     * Builds a consumer for the given {@link DataReceiver}
     * 
     * @param receiver
     * @param channel
     * @return
     */
    public MessageConsumer build(DataReceiverImpl receiver, Channel channel);
    
    public MessageConsumerBuilder maxParallelProcessedMsgs(int maxParallelProcessedMsgs);
}
