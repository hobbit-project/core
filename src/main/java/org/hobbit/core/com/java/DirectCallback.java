package org.hobbit.core.com.java;

import java.util.List;

import org.hobbit.core.com.CommonChannel;

import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * This class is used by the DirectChannel implementation 
 * for a callback function as a consumer callback
 * @author altaf, sourabh, yamini, melisa
 *
 */
public abstract class  DirectCallback {
	
    protected CommonChannel channel;
    protected String queue;
    protected BasicProperties props;
	
    public DirectCallback() {}

    public DirectCallback(CommonChannel channel, String queue, BasicProperties props) {
        this.channel = channel;
        this.queue = queue;
        this.props = props;
    }

    public abstract void callback(byte[] data, List<Object> classs,BasicProperties props);

}
