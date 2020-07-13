package org.hobbit.core.com.java;

import java.util.List;

import org.hobbit.core.com.Channel;

import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * This class is used by the DirectChannel implementation 
 * for a callback function as a consumer callback
 * @author altaf, sourabh, yamini, melisa
 *
 */
public abstract class  DirectCallback {
	
    protected Channel channel;
    protected String queue;
    protected BasicProperties props;
	
    public DirectCallback() {}

    public DirectCallback(Channel channel, String queue, BasicProperties props) {
        this.channel = channel;
        this.queue = queue;
        this.props = props;
    }

    public abstract void callback(byte[] data, List<Object> classs,BasicProperties props);

}
