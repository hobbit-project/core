package org.hobbit.core.components.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectChannel implements CommonChannel {

	private static final Logger LOGGER = LoggerFactory.getLogger(DirectChannel.class);

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
    public byte[] readBytes(Object callback, Object classs) {
    	Thread t = new Thread(new ReadByteChannel(pipe, callback, classs));
    	t.start();
        return null;
    }

    @Override
    public void writeBytes(byte[] data) {

        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put(data);
        try {
            System.out.println("\nINSIDE writeBytes ");
            out.write(buffer);
            buffer.clear();
           // System.out.println(buffer.toString()+"\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeBytes(ByteBuffer buffer) {

        try {
            buffer.flip();
            System.out.println("\nINSIDE writeBytes ");
            while (buffer.hasRemaining())
                out.write(buffer);
            buffer.clear();
            //System.out.println(buffer+"\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getConsumerCallback(AbstractCommandReceivingComponent component) {
    	return new DirectCallback() {

			@Override
			public void callback(byte[] data, Object classs) {
				System.out.println(classs.getClass());
                AbstractCommandReceivingComponent comp = (AbstractCommandReceivingComponent) classs;
				LOGGER.debug("INSIDE CALLBACK");
                comp.getCmdThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                        	LOGGER.debug("INSIDE CALLBACK RUN");
                            comp.handleCmd(data, "");
                            //byte[] bytes = commonChannel.readBytes();
                            //handleCmd(bytes, "");
                        } catch (Exception e) {
                            LOGGER.error("Exception while trying to handle incoming command.", e);
                        }
                    }
                });

			}
		};
    }
}
