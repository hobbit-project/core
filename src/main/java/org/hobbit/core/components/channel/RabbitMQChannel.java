package org.hobbit.core.components.channel;

public class RabbitMQChannel implements CommonChannel {

    @Override
    public byte[] readBytes() {
        return new byte[0];
    }

    @Override
    public void writeBytes(byte[] data) {

    }
}
