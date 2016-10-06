package org.hobbit.core.data;

import java.io.Closeable;
import java.io.IOException;

import com.rabbitmq.client.Channel;

/**
 * Simple structure representing the data of a RabbitMQ queue, i.e., the
 * {@link Channel} and the name.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class RabbitQueue implements Closeable {

    /**
     * Channel of this queue.
     */
    public final Channel channel;
    /**
     * Name of this queue.
     */
    public final String name;

    /**
     * Constructor.
     * 
     * @param channel
     *            Channel of this queue.
     * @param name
     *            Name of this queue.
     */
    public RabbitQueue(Channel channel, String name) {
        this.channel = channel;
        this.name = name;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns number of available messages or 0 if an error occurred.
     * 
     * @return the number of available messages.
     */
    public long messageCount() {
        try {
            return channel.messageCount(name);
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RabbitQueue [channel=");
        builder.append(channel);
        builder.append(", name=");
        builder.append(name);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void close() throws IOException {
        try {
            channel.close();
        } catch (Exception e) {
        }
    }
}
