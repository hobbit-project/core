package org.hobbit.core.components.dummy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.junit.Ignore;

@Ignore
public class DummySystemReceiver extends AbstractSystemAdapter {

    /**
     * The data received from the data generator. The list needs to be
     * synchronized since {@link #receiveGeneratedData(byte[])} can be called by
     * several threads in parallel.
     */
    private final List<String> receivedData = Collections.synchronizedList(new ArrayList<String>());
    /**
     * The tasks received from the task generator. The list needs to be
     * synchronized since {@link #receiveGeneratedTask(String taskId, byte[]
     * data[])} can be called by several threads in parallel.
     */
    private final List<String> receivedTasks = Collections.synchronizedList(new ArrayList<String>());

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
