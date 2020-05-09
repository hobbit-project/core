package org.hobbit.core.components.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class DirectChannel implements CommonChannel {

    Pipe pipe;
    WritableByteChannel out;
    ReadableByteChannel in;
    DirectChannel(){
        try {
            pipe= Pipe.open();
            out = pipe.sink();
            in = pipe.source();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public byte[] readBytes() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        try {
            System.out.println("\n\n INSIDE readBytes ");
            in.read(byteBuffer);
            System.out.println(byteBuffer.toString()+"\n\n");
            return byteBuffer.array();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void writeBytes(byte[] data) {

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put(data);
        try {
            System.out.println("\n\n INSIDE writeBytes ");
            out.write(buffer);
            System.out.println(buffer.toString()+"\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
