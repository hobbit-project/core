package org.hobbit.core.components;

/**
 * This interface is implemented by components that want to receive task data
 * that is sent by a task generator component.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface TaskReceivingComponent extends Component {

    /**
     * This method is called if a task is received from a task generator.
     * 
     * @param taskId
     *            the id of the received task
     * @param data
     *            the data received from a data generator
     */
    public void receiveGeneratedTask(String taskId, byte[] data);

}
