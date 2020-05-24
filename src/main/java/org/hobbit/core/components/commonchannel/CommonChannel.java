package org.hobbit.core.components.commonchannel;

import java.nio.ByteBuffer;

import org.hobbit.core.components.AbstractCommandReceivingComponent;

public interface CommonChannel {

	public byte[] readBytes(Object consumerCallback, Object classs, String queue);

	public void writeBytes(byte data[]);

	public void writeBytes(ByteBuffer buffer, String queue);

	public void close();
}
