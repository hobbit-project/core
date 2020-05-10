package org.hobbit.core.components.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;

public class ReadByteChannel extends DirectChannel implements Runnable{
	
	ReadableByteChannel in;
	DirectCallback callback;

	public ReadByteChannel(Pipe pipe, Object callback) {
		in = pipe.source();
		this.callback = (DirectCallback) callback;
	}

	@Override
	public void run() {
		ByteBuffer buffer = ByteBuffer.allocate(6);
        try {
        	System.out.println("\nINSIDE readBytes ");
        	while(in.read(buffer) > 0){
                //limit is set to current position and position is set to zero
                //buffer.flip();
                while(buffer.hasRemaining()){
                   //char ch = (char) buffer.get();
                   System.out.print("R : ");
                   System.out.print(buffer.get());
                }
                buffer.flip();
                callback.callback(buffer.array());
                buffer.clear();
             } 	
        	
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}

}
