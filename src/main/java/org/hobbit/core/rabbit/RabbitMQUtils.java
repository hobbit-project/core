package org.hobbit.core.rabbit;

import java.nio.ByteBuffer;

import org.apache.commons.io.Charsets;

public class RabbitMQUtils {

	public static String readString(ByteBuffer buffer) {
		return new String(readByteArray(buffer), Charsets.UTF_8);
	}

	public static byte[] readByteArray(ByteBuffer buffer) {
		int length = buffer.getInt();
		byte[] data = new byte[length];
		buffer.get(data, 0, data.length);
		return data;
	}

}
