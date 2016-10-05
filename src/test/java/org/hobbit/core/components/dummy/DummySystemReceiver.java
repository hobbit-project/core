package org.hobbit.core.components.dummy;

import java.util.ArrayList;
import java.util.List;

import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;

public class DummySystemReceiver extends AbstractSystemAdapter {

    private final List<String> receivedData = new ArrayList<String>();
    private final List<String> receivedTasks = new ArrayList<String>();

    @Override
    public void receiveGeneratedData(byte[] data) {
        receivedData.add(RabbitMQUtils.readString(data));
    }

    @Override
    public void receiveGeneratedTask(String taskId, byte[] data) {
        receivedTasks.add(RabbitMQUtils.readString(data));
    }

    /**
     * @return the receiveddata
     */
    public List<String> getReceiveddata() {
        return receivedData;
    }

    /**
     * @return the receivedtasks
     */
    public List<String> getReceivedtasks() {
        return receivedTasks;
    }

}
