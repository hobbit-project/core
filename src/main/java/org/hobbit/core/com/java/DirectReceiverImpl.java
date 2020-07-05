package org.hobbit.core.com.java;

import java.io.IOException;

import org.hobbit.core.com.CommonChannel;
import org.hobbit.core.com.DataHandler;
import org.hobbit.core.com.DataReceiver;
import org.hobbit.core.components.communicationfactory.ChannelFactory;
import org.hobbit.core.data.RabbitQueue;

/**
 * This class implements the methods of DataReceiver for {@link DirectChannel}
 *
 * @author Altafhusen Makander
 * @author Sourabh Poddar
 * @author Yamini Punetha
 * @author Melissa Das
 *
 */
public class DirectReceiverImpl implements DataReceiver {

    public DirectReceiverImpl(String queue, Object consumer) {

        CommonChannel channel = new ChannelFactory().getChannel(false, queue, null);
        try {
            channel.readBytes(consumer, this, null, queue);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public int getErrorCount() {
        return 0;
    }

    @Override
    public void closeWhenFinished() {
        // TODO Auto-generated method stub

    }

    @Override
    public void increaseErrorCount() {
        // TODO Auto-generated method stub

    }

    @Override
    public DataHandler getDataHandler() {
        return null;
    }

    @Override
    public RabbitQueue getQueue() {
        return null;
    }

}
