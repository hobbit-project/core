package org.hobbit.core.components.data;

/**
 * This simple structure comprises the timestamp at which a task has been sent
 * to the system, the expected system response, the data that has been received
 * by the system as well as the timestamp when it has been received.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ResultPair {

	public static final long UNKNOWN_TIMESTAMP_VALUE = -1;

	private long taskSentTimestamp = UNKNOWN_TIMESTAMP_VALUE;
	private byte expectedData[];
	private long responseReceivedTimestamp = UNKNOWN_TIMESTAMP_VALUE;
	private byte receivedData[];

	public ResultPair(long taskSentTimestamp, byte[] expectedData, long responseReceivedTimestamp, byte[] receivedData) {
		super();
		this.taskSentTimestamp = taskSentTimestamp;
		this.expectedData = expectedData;
		this.responseReceivedTimestamp = responseReceivedTimestamp;
		this.receivedData = receivedData;
	}

	/**
	 * @return the taskSentTimestamp
	 */
	public long getTaskSentTimestamp() {
		return taskSentTimestamp;
	}

	/**
	 * @param taskSentTimestamp
	 *            the taskSentTimestamp to set
	 */
	public void setTaskSentTimestamp(long taskSentTimestamp) {
		this.taskSentTimestamp = taskSentTimestamp;
	}

	/**
	 * @return the expectedData
	 */
	public byte[] getExpectedData() {
		return expectedData;
	}

	/**
	 * @param expectedData
	 *            the expectedData to set
	 */
	public void setExpectedData(byte[] expectedData) {
		this.expectedData = expectedData;
	}

	/**
	 * @return the responseReceivedTimestamp
	 */
	public long getResponseReceivedTimestamp() {
		return responseReceivedTimestamp;
	}

	/**
	 * @param responseReceivedTimestamp
	 *            the responseReceivedTimestamp to set
	 */
	public void setResponseReceivedTimestamp(long responseReceivedTimestamp) {
		this.responseReceivedTimestamp = responseReceivedTimestamp;
	}

	/**
	 * @return the receivedData
	 */
	public byte[] getReceivedData() {
		return receivedData;
	}

	/**
	 * @param receivedData
	 *            the receivedData to set
	 */
	public void setReceivedData(byte[] receivedData) {
		this.receivedData = receivedData;
	}

}
