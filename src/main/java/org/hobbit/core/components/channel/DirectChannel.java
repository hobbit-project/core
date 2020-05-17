package org.hobbit.core.components.channel;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectChannel implements CommonChannel {

	private static final Logger LOGGER = LoggerFactory.getLogger(DirectChannel.class);
	
	static Map<String, Pipe> pipes = new HashMap<>();

    Pipe pipe;
    WritableByteChannel out;
    ReadableByteChannel in;
    List<Thread> threads = new ArrayList<>();
    DirectChannel(){}
    DirectChannel(String queue){
        try {
        	if(pipes.get(queue) == null) {
        		pipe= Pipe.open();
        		out = pipe.sink();
        		in = pipe.source();
        		pipes.put(queue, pipe);
        	}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public byte[] readBytes(Object callback, Object classs, String queue) {
    	Thread t = new Thread(new ReadByteChannel(pipes.get(queue), callback, classs));
    	threads.add(t);
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
    public void writeBytes(ByteBuffer buffer, String queue) {

        try {
            buffer.flip();
            System.out.println("\nINSIDE writeBytes ");
            while (buffer.hasRemaining())
                pipes.get(queue).sink().write(buffer);
            buffer.clear();
            //System.out.println(buffer+"\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getConsumerCallback(AbstractCommandReceivingComponent component, String methodName, Class[] parameterTypes) {
    	
    	return new DirectCallback() {

			@Override
			public void callback(byte[] data, List<Object> list) {
				for(Object classs:list) {
					if(classs != null) {
						try {
							System.out.println(classs.getClass());
							AbstractCommandReceivingComponent comp = (AbstractCommandReceivingComponent) classs;
							Object[] parameters = new Object[2];
				    		parameters[0] = null;
				    		parameters[1] = data.clone();
				    		Method method = comp.getClass().getMethod(methodName, parameterTypes);
				    		method.invoke(comp, parameters);
							/*AbstractCommandReceivingComponent comp = (AbstractCommandReceivingComponent) classs;
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
							});*/
						}catch(Exception e) {
							System.out.println("CALLBACK EXCEPTION" + e);
						}
					}
				}

			}
		};
    }
	@Override
	public void close() {
		if(ReadByteChannel.classes != null && ReadByteChannel.classes.size() > 0) {
			ReadByteChannel.classes.clear();
		}
		/*if(threads != null && threads.size() > 0) {
			for(Thread t:threads) {
				t.stop();
			}
		}*/
		if(pipes != null && pipes.size() > 0) {
			for(Map.Entry<String, Pipe> entry : pipes.entrySet()) {
				try {
					entry.getValue().sink().close();
				} catch (IOException e) {
					LOGGER.error("Error closing pipe",e);
				}
			}
		}
		
	}
}
