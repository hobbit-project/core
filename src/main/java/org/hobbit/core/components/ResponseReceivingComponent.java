package org.hobbit.core.components;

import java.io.InputStream;

/**
 * This interface is implemented by components that want to receive the
 * responses from the system.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface ResponseReceivingComponent extends Component {

	/**
	 * This method is called if a response is received from the system.
	 * 
	 * @param taskId
	 *            the id of the task
	 * @param timestamp
	 *            the time at which the response has been received from the
	 *            system
	 * @param stream
	 *            the data received from a data generator
	 */
	public void receiveResponseData(String taskId, long timestamp, InputStream stream);

}
