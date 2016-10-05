package org.hobbit.core.components.dummy;

import java.util.ArrayList;
import java.util.List;

import org.hobbit.core.components.AbstractTaskGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;

public class DummyTaskGenReceiver extends AbstractTaskGenerator {

    private final List<String> receivedData = new ArrayList<String>();

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
