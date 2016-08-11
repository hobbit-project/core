package org.hobbit.core.components;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;

/**
 * This abstract class implements basic functions that can be used to implement
 * a task generator.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractEvaluationModule extends AbstractCommandReceivingComponent {

	/**
	 * Name of the queue to the evaluation storage.
	 */
	protected String EvalModule2EvalStoreQueueName;
	/**
	 * Channel of the queue to the evaluation storage.
	 */
	protected Channel EvalModule2EvalStore;
	/**
	 * Name of the incoming queue from the evaluation storage.
	 */
	protected String EvalStore2EvalModuleQueueName;
	/**
	 * Consumer used to receive the responses from the evaluation storage.
	 */
	protected QueueingConsumer consumer;

	@Override
	public void init() throws Exception {
		super.init();

		EvalModule2EvalStoreQueueName = generateSessionQueueName(Constants.EVAL_MODULE_2_EVAL_STORAGE_QUEUE_NAME);
		EvalModule2EvalStore = connection.createChannel();
		EvalModule2EvalStore.queueDeclare(EvalModule2EvalStoreQueueName, false, false, true, null);

		EvalStore2EvalModuleQueueName = generateSessionQueueName(Constants.EVAL_STORAGE_2_EVAL_MODULE_QUEUE_NAME);
		EvalModule2EvalStore.queueDeclare(EvalStore2EvalModuleQueueName, false, false, true, null);

		consumer = new QueueingConsumer(EvalModule2EvalStore);
		EvalModule2EvalStore.basicConsume(EvalStore2EvalModuleQueueName, consumer);
	}

	@Override
	public void run() throws Exception {
		sendToCmdQueue(Commands.EVAL_MODULE_READY_SIGNAL);
		collectResponses();
		Model model = summarizeEvaluation();
		sendResultModel(model);
	}

	/**
	 * This method communicates with the evaluation storage to collect all
	 * response pairs. For every pair the
	 * {@link #evaluateResponse(byte[], byte[], long, long)} method is called.
	 * 
	 * @throws Exception
	 *             if a communication error occurs.
	 */
	protected void collectResponses() throws Exception {
		byte[] expectedData;
		byte[] receivedData;
		long taskSentTimestamp;
		long responseReceivedTimestamp;

		String corrId;
		BasicProperties props;
		byte[] iteratorId = new byte[4];
		ByteBuffer buffer = ByteBuffer.wrap(iteratorId);
		buffer.putInt(AbstractEvaluationStorage.NEW_ITERATOR_ID);

		int length;
		while (true) {
			// request next response pair
			corrId = java.util.UUID.randomUUID().toString();
			props = new BasicProperties.Builder().deliveryMode(2).correlationId(corrId)
					.replyTo(EvalStore2EvalModuleQueueName).build();
			EvalModule2EvalStore.basicPublish("", EvalModule2EvalStoreQueueName, props, iteratorId);
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			if (delivery.getProperties().getCorrelationId().equals(corrId)) {
				// parse the response
				buffer = ByteBuffer.wrap(delivery.getBody());
				taskSentTimestamp = buffer.getLong();
				length = buffer.getInt();
				expectedData = new byte[length];
				buffer.get(expectedData);

				responseReceivedTimestamp = buffer.getLong();
				length = buffer.getInt();
				receivedData = new byte[length];
				buffer.get(receivedData);

				if (((expectedData != null) || (expectedData.length > 0))
						&& ((receivedData != null) || (receivedData.length > 0))) {
					return;
				}
				evaluateResponse(expectedData, receivedData, taskSentTimestamp, responseReceivedTimestamp);
			}
		}
	}

	/**
	 * Evaluates the given response pair.
	 * 
	 * @param expectedData
	 *            the data that has been expected
	 * @param receivedData
	 *            the data that has been received from the system
	 * @param taskSentTimestamp
	 *            the time at which the task has been sent to the system
	 * @param responseReceivedTimestamp
	 *            the time at which the response has been received from the
	 *            system
	 * @throws Exception
	 */
	protected abstract void evaluateResponse(byte[] expectedData, byte[] receivedData, long taskSentTimestamp,
			long responseReceivedTimestamp) throws Exception;

	/**
	 * Summarizes the evaluation and generates an RDF model containing the
	 * evaluation results.
	 * 
	 * @return an RDF model containing the evaluation results
	 * @throws Exception
	 *             if a sever error occurs
	 */
	protected abstract Model summarizeEvaluation() throws Exception;

	/**
	 * Sends the model to the benchmark controller.
	 * 
	 * @param model
	 *            the model that should be sent
	 * @throws IOException
	 *             if an error occurs during the commmunication
	 */
	private void sendResultModel(Model model) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		model.write(outputStream);
		sendToCmdQueue(Commands.EVAL_MODULE_FINISHED_SIGNAL, outputStream.toByteArray());
	}

	@Override
	public void close() throws IOException {
		if (EvalModule2EvalStore != null) {
			try {
				EvalModule2EvalStore.close();
			} catch (Exception e) {
			}
		}
		super.close();
	}
}
