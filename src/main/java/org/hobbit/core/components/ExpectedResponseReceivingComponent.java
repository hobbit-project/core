package org.hobbit.core.components;

/**
 * This interface is implemented by components that want to receive the expected
 * responses from the task generator component.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface ExpectedResponseReceivingComponent extends Component {

	/**
	 * This method is called if an expected response is received from a task
	 * generator component.
	 *
	 * @param taskId
	 *            the id of the task
	 * @param timestamp
	 *            the time at which the task has been sent to the system
	 * @param data
	 *            the data received from a task generator
	 */
	public void receiveExpectedResonseData(String taskId, long timestamp, byte[] data);

}
