package org.hobbit.core.components.channel;

public interface CommonChannel {
    byte[] readBytes();
    void writeBytes(byte data[]);
}
