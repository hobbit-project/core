package org.hobbit.core.rabbit;

import java.io.IOException;

import org.hobbit.core.data.RabbitQueue;

import com.rabbitmq.client.Channel;

public interface RabbitQueueFactory {

    /**
     * This method opens a channel using the established connection to RabbitMQ
     * and creates a new queue using the given name and the following
     * configuration:
     * <ul>
     * <li>The channel number is automatically derived from the connection.</li>
     * <li>The queue is not durable.</li>
     * <li>The queue is not exclusive.</li>
     * <li>The queue is configured to be deleted automatically.</li>
     * <li>No additional queue configuration is defined.</li>
     * </ul>
     * 
     * @param name
     *            name of the queue
     * @return {@link RabbitQueue} object comprising the {@link Channel} and the
     *         name of the created queue
     * @throws IOException
     *             if a communication problem during the creation of the channel
     *             or the queue occurs
     */
    public RabbitQueue createDefaultRabbitQueue(String name) throws IOException;
}
