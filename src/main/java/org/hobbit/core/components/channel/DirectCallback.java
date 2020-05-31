package org.hobbit.core.components.channel;

import java.util.List;

import org.hobbit.core.components.commonchannel.CommonChannel;

import com.rabbitmq.client.AMQP.BasicProperties;

public class  DirectCallback {
	
	protected CommonChannel channel;
	protected String queue;
	protected BasicProperties props;
	
public DirectCallback() {
		
		
	}

	 public DirectCallback(CommonChannel channel, String queue, BasicProperties props) {
		
		this.channel = channel;
		this.queue = queue;
		this.props = props;
	}


	public void callback(byte[] data, List<Object> classs,BasicProperties props) {
		 
	 }

}
