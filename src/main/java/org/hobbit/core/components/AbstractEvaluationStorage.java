package org.hobbit.core.components;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.rabbit.RabbitMQUtils;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * This abstract class implements basic functions that can be used to implement
 * a task generator.
 * 
 * FIXME Implement me!!!!
 * 
 * <p>
 * The following environment variables are expected:
 * <ul>
 * <li>{@link Constants#GENERATOR_ID_KEY}</li>
 * <li>{@link Constants#GENERATOR_COUNT_KEY}</li>
 * </ul>
 * </p>
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractEvaluationStorage extends AbstractCommandReceivingComponent implements
		ResponseReceivingComponent, ExpectedResponseReceivingComponent {

	public static final int NEW_ITERATOR_ID = -1;


	/**
	 * Mutex used to wait for the termination signal.
	 */
	private Semaphore terminationMutex = new Semaphore(0);
	/**
	 * Name of the queue to the evaluation storage.
	 */
	protected String taskGen2EvalStoreQueueName;
	/**
	 * Channel of the queue to the evaluation storage.
	 */
	protected Channel taskGen2EvalStore;
	/**
	 * Name of the queue to the system.
	 */
	protected String system2EvalStoreQueueName;
	/**
	 * Channel of the queue to the system.
	 */
	protected Channel system2EvalStore;
	/**
	 * Name of the queue to the evaluation storage.
	 */
	protected String EvalModule2EvalStoreQueueName;
	/**
	 * Channel of the queue to the evaluation storage.
	 */
	protected Channel EvalModule2EvalStore;

	protected List<Iterator> resultPairIterators = new ArrayList<>();

	@Override
	public void init() throws Exception {
		super.init();

		@SuppressWarnings("resource")
		ExpectedResponseReceivingComponent expReceiver = this;
		taskGen2EvalStoreQueueName = generateSessionQueueName(Constants.TASK_GEN_2_EVAL_STORAGE_QUEUE_NAME);
		taskGen2EvalStore = connection.createChannel();
		taskGen2EvalStore.queueDeclare(taskGen2EvalStoreQueueName, false, false, true, null);
		taskGen2EvalStore.basicConsume(taskGen2EvalStoreQueueName, true, new DefaultConsumer(taskGen2EvalStore) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
					throws IOException {
				ByteBuffer buffer = ByteBuffer.wrap(body);
				String taskId = RabbitMQUtils.readString(buffer);
				long timestamp = buffer.getLong();
				byte[] data = RabbitMQUtils.readByteArray(buffer);
				expReceiver.receiveExpectedResonseData(taskId, timestamp, data);
			}
		});

		@SuppressWarnings("resource")
		ResponseReceivingComponent respReceiver = this;
		system2EvalStoreQueueName = generateSessionQueueName(Constants.TASK_GEN_2_SYSTEM_QUEUE_NAME);
		system2EvalStore = connection.createChannel();
		system2EvalStore.queueDeclare(system2EvalStoreQueueName, false, false, true, null);
		system2EvalStore.basicConsume(system2EvalStoreQueueName, true, new DefaultConsumer(system2EvalStore) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
					throws IOException {
				ByteBuffer buffer = ByteBuffer.wrap(body);
				String taskId = RabbitMQUtils.readString(buffer);
				long timestamp = buffer.getLong();
				byte[] data = RabbitMQUtils.readByteArray(buffer);
				respReceiver.receiveResponseData(taskId, timestamp, data);
			}
		});

		EvalModule2EvalStoreQueueName = generateSessionQueueName(Constants.TASK_GEN_2_SYSTEM_QUEUE_NAME);
		EvalModule2EvalStore = connection.createChannel();
		EvalModule2EvalStore.queueDeclare(EvalModule2EvalStoreQueueName, false, false, true, null);
		EvalModule2EvalStore.basicConsume(EvalModule2EvalStoreQueueName, true, new DefaultConsumer(EvalModule2EvalStore) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
					throws IOException {
				// TODO get iterator id
				// TODO if NEW_ITERATOR_ID -> create a new iterator
				// TODO get next response pair
				// TODO send response pair
			}
		});
	}

	@Override
	public void run() throws Exception {
		sendToCmdQueue(Commands.EVAL_STORAGE_READY_SIGNAL);
		terminationMutex.acquire();
	}

	@Override
	public void receiveCommand(byte command, byte[] data) {
		// If this is the signal to start the data generation
		if (command == Commands.EVAL_STORAGE_TERMINATE) {
			// release the mutex
			terminationMutex.release();
		}
	}

	@Override
	public void close() throws IOException {
		if (taskGen2EvalStore != null) {
			try {
				taskGen2EvalStore.close();
			} catch (Exception e) {
			}
		}
		if (system2EvalStore != null) {
			try {
				system2EvalStore.close();
			} catch (Exception e) {
			}
		}
		if (EvalModule2EvalStore != null) {
			try {
				EvalModule2EvalStore.close();
			} catch (Exception e) {
			}
		}
		super.close();
	}
}
