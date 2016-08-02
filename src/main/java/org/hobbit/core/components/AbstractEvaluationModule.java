package org.hobbit.core.components;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.io.Charsets;
import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;

/**
 * This abstract class implements basic functions that can be used to implement
 * a task generator.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractEvaluationModule extends AbstractCommandReceivingComponent {

	/**
	 * Name of the incoming queue with which the task generator can receive data
	 * from the data generators.
	 */
	protected String dataGen2TaskGenQueueName;
	/**
	 * The Channel of the incoming queue with which the task generator can
	 * receive data from the data generators.
	 */
	protected Channel dataGen2TaskGen;
	/**
	 * Name of the queue to the system.
	 */
	protected String taskGen2SystemQueueName;
	/**
	 * Channel of the queue to the system.
	 */
	protected Channel taskGen2System;
	/**
	 * Name of the queue to the evaluation storage.
	 */
	protected String taskGen2EvalStoreQueueName;
	/**
	 * Channel of the queue to the evaluation storage.
	 */
	protected Channel taskGen2EvalStore;

	@Override
	public void init() throws Exception {
		super.init();

		taskGen2SystemQueueName = generateSessionQueueName(Constants.TASK_GEN_2_SYSTEM_QUEUE_NAME);
		taskGen2System = connection.createChannel();
		taskGen2System.queueDeclare(taskGen2SystemQueueName, false, false, true, null);

		taskGen2EvalStoreQueueName = generateSessionQueueName(Constants.TASK_GEN_2_EVAL_STORAGE_QUEUE_NAME);
		taskGen2EvalStore = connection.createChannel();
		taskGen2EvalStore.queueDeclare(taskGen2EvalStoreQueueName, false, false, true, null);
	}

	@Override
	public void run() throws Exception {
		sendToCmdQueue(Commands.EVAL_MODULE_READY_SIGNAL);

		collectResponses();

		Model model = summarizeEvaluation();
		
		sendResultModel(model);
	}

	protected void collectResponses() {
		// TODO Implement the iteration over the evaluation storage until an
		// empty response is received

	}

	protected abstract void evaluateResponse(byte[] expectedData, byte[] receivedData, long taskSentTimestamp,
			long responseReceivedTimestamp) throws Exception;

	protected abstract Model summarizeEvaluation() throws Exception;

	private void sendResultModel(Model model) {
		// TODO send the model to the benchmark controller
	}

	/**
	 * This method sends the given data and the given timestamp of the task with
	 * the given task id to the evaluation storage.
	 * 
	 * @param taskIdString
	 *            the id of the task
	 * @param timestamp
	 *            the timestamp of the moment in which the task has been sent to
	 *            the system
	 * @param data
	 *            the expected response for the task with the given id
	 * @throws IOException
	 *             if there is an error during the sending
	 */
	protected void sendTaskToEvalStorage(String taskIdString, long timestamp, byte[] data) throws IOException {
		byte[] taskIdBytes = taskIdString.getBytes(Charsets.UTF_8);
		// + 4 for taskIdBytes.length
		// + 4 for data length (time stamp + data)
		// + 8 for time stamp
		// + 4 for data.length
		int dataLength = 8 + 4 + data.length;
		int capacity = 4 + taskIdString.length() + 4 + dataLength;
		ByteBuffer buffer = ByteBuffer.allocate(capacity);
		buffer.putInt(taskIdBytes.length);
		buffer.put(taskIdBytes);
		buffer.putInt(dataLength);
		buffer.putLong(timestamp);
		buffer.putInt(data.length);
		buffer.put(data);
		taskGen2EvalStore.basicPublish("", taskGen2SystemQueueName, MessageProperties.PERSISTENT_BASIC, buffer.array());
	}

	/**
	 * Sends the given task with the given task id and data to the system.
	 * 
	 * @param taskIdString
	 *            the id of the task
	 * @param data
	 *            the data of the task
	 * @throws IOException
	 *             if there is an error during the sending
	 */
	protected void sendTaskToSystemAdapter(String taskIdString, byte[] data) throws IOException {
		byte[] taskIdBytes = taskIdString.getBytes(Charsets.UTF_8);
		// + 4 for taskIdBytes.length
		// + 4 for data.length
		int capacity = 4 + 4 + taskIdBytes.length + data.length;
		ByteBuffer buffer = ByteBuffer.allocate(capacity);
		buffer.putInt(taskIdBytes.length);
		buffer.put(taskIdBytes);
		buffer.putInt(data.length);
		buffer.put(data);
		taskGen2System.basicPublish("", taskGen2EvalStoreQueueName, MessageProperties.PERSISTENT_BASIC, buffer.array());
	}

	@Override
	public void close() throws IOException {
		if (dataGen2TaskGen != null) {
			try {
				dataGen2TaskGen.close();
			} catch (Exception e) {
			}
		}
		if (taskGen2System != null) {
			try {
				taskGen2System.close();
			} catch (Exception e) {
			}
		}
		if (taskGen2EvalStore != null) {
			try {
				taskGen2EvalStore.close();
			} catch (Exception e) {
			}
		}
		super.close();
	}
}
