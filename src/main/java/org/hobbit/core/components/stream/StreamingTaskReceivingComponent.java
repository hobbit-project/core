package org.hobbit.core.components.stream;

import java.io.InputStream;

import org.hobbit.core.components.Component;

/**
 * This interface is implemented by components that want to receive task data
 * that is sent by a task generator component.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface StreamingTaskReceivingComponent extends Component {

    /**
     * This method is called if a task is received from a task generator.
     * 
     * @param taskId
     *            the id of the received task
     * @param dataStream
     *            the data received from a data generator
     */
    public void receiveGeneratedTask(String taskId, InputStream dataStream);

}
