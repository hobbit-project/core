package org.hobbit.core.components.channel;

import java.nio.ByteBuffer;

import org.hobbit.core.components.AbstractCommandReceivingComponent;

public class RabbitMQChannel implements CommonChannel {

    @Override
    public byte[] readBytes(Object callback, Object classs, String queue) {
        return new byte[0];
    }

    @Override
    public void writeBytes(byte[] data) {

    }

    @Override
    public void writeBytes(ByteBuffer buffer, String queue) {

    }

	@Override
	public Object getConsumerCallback(AbstractCommandReceivingComponent component, String methodName, Class[] parameterTypes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
}
