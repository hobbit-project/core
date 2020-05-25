package org.hobbit.core.components.channel;

import java.util.List;

import org.hobbit.core.components.commonchannel.CommonChannel;

public class  DirectCallback {
	
	protected CommonChannel channel;
	protected String queue;
	
public DirectCallback() {
		
		
	}

	 public DirectCallback(CommonChannel channel, String queue) {
		
		this.channel = channel;
		this.queue = queue;
	}


	public void callback(byte[] data, List<Object> classs) {
		 
	 }

}
