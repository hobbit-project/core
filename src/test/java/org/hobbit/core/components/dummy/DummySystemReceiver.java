package org.hobbit.core.components.dummy;

import java.util.ArrayList;
import java.util.List;

import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.junit.Ignore;

@Ignore
public class DummySystemReceiver extends AbstractSystemAdapter {

    private final List<String> receivedData = new ArrayList<String>();
    private final List<String> receivedTasks = new ArrayList<String>();

    @Override
    public void receiveGeneratedData(byte[] data) {
        receivedData.add(RabbitMQUtils.readString(data));
    }

    @Override
    public void receiveGeneratedTask(String taskId, byte[] data) {
        receivedTasks.add(taskId + RabbitMQUtils.readString(data));
    }

    /**
     * @return the received data
     */
    public List<String> getReceiveddata() {
        return receivedData;
    }

    /**
     * @return the received tasks
     */
    public List<String> getReceivedtasks() {
        return receivedTasks;
    }

}
