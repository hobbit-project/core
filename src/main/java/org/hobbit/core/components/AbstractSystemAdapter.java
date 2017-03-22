package org.hobbit.core.components;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.components.stream.AbstractStreamingSystemAdapter;

/**
 * This abstract class implements basic functions that can be used to implement
 * a system adapter.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractSystemAdapter extends AbstractStreamingSystemAdapter
        implements GeneratedDataReceivingComponent, TaskReceivingComponent {

    public AbstractSystemAdapter() {
        super();
    }

    public AbstractSystemAdapter(int maxParallelProcessedMsgs) {
        super(maxParallelProcessedMsgs);
    }

    /**
     * This method sends the given result data for the task with the given task
     * id to the evaluation storage.
     * 
     * @param taskIdString
     *            the id of the task
     * @param data
     *            the data of the task
     * @throws IOException
     *             if there is an error during the sending
     */
    protected void sendResultToEvalStorage(String taskIdString, byte[] data) throws IOException {
        sendResultToEvalStorage(taskIdString, new ByteArrayInputStream(data));
    }

    @Override
    public void receiveGeneratedData(InputStream dataStream) {
        try {
            receiveGeneratedData(IOUtils.toByteArray(dataStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receiveGeneratedTask(String taskId, InputStream dataStream) {
        try {
            receiveGeneratedTask(taskId, IOUtils.toByteArray(dataStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
