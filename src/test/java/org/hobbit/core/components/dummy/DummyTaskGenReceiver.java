package org.hobbit.core.components.dummy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hobbit.core.components.AbstractTaskGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.junit.Ignore;

@Ignore
public class DummyTaskGenReceiver extends AbstractTaskGenerator {

    /**
     * The data received from the data generator. The list needs to be
     * synchronized since {@link #generateTask(byte[])} can be called by several
     * threads in parallel.
     */
    private final List<String> receivedData = Collections.synchronizedList(new ArrayList<String>());

    @Override
    protected void generateTask(byte[] data) throws Exception {
        receivedData.add(RabbitMQUtils.readString(data));
    }

    /**
     * @return the receiveddata
     */
    public List<String> getReceiveddata() {
        return receivedData;
    }

}
