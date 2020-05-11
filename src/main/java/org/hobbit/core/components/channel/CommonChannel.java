package org.hobbit.core.components.channel;

import java.nio.ByteBuffer;

import org.hobbit.core.components.AbstractCommandReceivingComponent;

public interface CommonChannel {

	public byte[] readBytes(Object consumerCallback, Object classs);

	public void writeBytes(byte data[]);

	public void writeBytes(ByteBuffer buffer);

    public Object getConsumerCallback(AbstractCommandReceivingComponent component);
}
