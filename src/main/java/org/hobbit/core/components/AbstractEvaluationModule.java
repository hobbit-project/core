package org.hobbit.core.components;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEvaluationModule.class);

	/**
	 * Name of the queue to the evaluation storage.
	 */
	protected String evalModule2EvalStoreQueueName;
	/**
	 * Channel of the queue to the evaluation storage.
	 */
	protected Channel evalModule2EvalStore;
	/**
	 * Name of the incoming queue from the evaluation storage.
	 */
	protected String evalStore2EvalModuleQueueName;
	/**
	 * Consumer used to receive the responses from the evaluation storage.
	 */
	protected QueueingConsumer consumer;

	@Override
	public void init() throws Exception {
		super.init();

		evalModule2EvalStoreQueueName = generateSessionQueueName(Constants.EVAL_MODULE_2_EVAL_STORAGE_QUEUE_NAME);
		evalModule2EvalStore = connection.createChannel();
		evalModule2EvalStore.queueDeclare(evalModule2EvalStoreQueueName, false, false, true, null);

		evalStore2EvalModuleQueueName = generateSessionQueueName(Constants.EVAL_STORAGE_2_EVAL_MODULE_QUEUE_NAME);
		evalModule2EvalStore.queueDeclare(evalStore2EvalModuleQueueName, false, false, true, null);

		consumer = new QueueingConsumer(evalModule2EvalStore);
		evalModule2EvalStore.basicConsume(evalStore2EvalModuleQueueName, consumer);
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
		byte requestBody[] = new byte[] { AbstractEvaluationStorage.NEW_ITERATOR_ID };
		ByteBuffer buffer;

		while (true) {
			// request next response pair
			corrId = java.util.UUID.randomUUID().toString();
			props = new BasicProperties.Builder().deliveryMode(2).correlationId(corrId)
					.replyTo(evalStore2EvalModuleQueueName).build();
			evalModule2EvalStore.basicPublish("", evalModule2EvalStoreQueueName, props, requestBody);
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			if (delivery.getProperties().getCorrelationId().equals(corrId)) {
				// parse the response
				buffer = ByteBuffer.wrap(delivery.getBody());
				// if the response is empty
				if (buffer.remaining() == 0) {
					LOGGER.error("Got a completely empty response from the evaluation storage.");
					return;
				}
				requestBody[0] = buffer.get();

				// if the response is empty
				if (buffer.remaining() == 0) {
					return;
				}
				taskSentTimestamp = RabbitMQUtils.readLong(RabbitMQUtils.readByteArray(buffer));
				expectedData = RabbitMQUtils.readByteArray(buffer);

				responseReceivedTimestamp = RabbitMQUtils.readLong(RabbitMQUtils.readByteArray(buffer));
				receivedData = RabbitMQUtils.readByteArray(buffer);

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
    public void receiveCommand(byte command, byte[] data) {
        // Nothing to do
    }

	@Override
	public void close() throws IOException {
		if (evalModule2EvalStore != null) {
			try {
				evalModule2EvalStore.close();
			} catch (Exception e) {
			}
		}
		super.close();
	}
}
