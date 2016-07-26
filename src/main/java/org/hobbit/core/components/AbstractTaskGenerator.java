package org.hobbit.core.components;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.Charsets;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;

public abstract class AbstractTaskGenerator extends AbstractCommandReceivingComponent implements
		GeneratedDataReceivingComponent {

	private static final int DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES = 100;

	private Semaphore startTaskGenMutex = new Semaphore(0);
	private Semaphore currentlyProcessedMessages;
	private int generatorId;
	private int numberOfGenerators;
	private long nextTaskId;
	private int maxParallelProcessedMsgs = DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES;
	protected String dataGen2TaskGenQueueName;
	protected Channel dataGen2TaskGen;
	protected String taskGen2SystemQueueName;
	protected Channel taskGen2System;
	protected String taskGen2EvalStoreQueueName;
	protected Channel taskGen2EvalStore;

	@Override
	public void init() throws Exception {
		super.init();
		Map<String, String> env = System.getenv();

		if (!env.containsKey(Constants.GENERATOR_ID_KEY)) {
			throw new IllegalArgumentException("Couldn't get \"" + Constants.GENERATOR_ID_KEY
					+ "\" from the environment. Aborting.");
		}
		try {
			generatorId = Integer.parseInt(env.get(Constants.GENERATOR_ID_KEY));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Couldn't get \"" + Constants.GENERATOR_ID_KEY
					+ "\" from the environment. Aborting.", e);
		}
		nextTaskId = generatorId;

		if (!env.containsKey(Constants.GENERATOR_COUNT_KEY)) {
			throw new IllegalArgumentException("Couldn't get \"" + Constants.GENERATOR_COUNT_KEY
					+ "\" from the environment. Aborting.");
		}
		try {
			numberOfGenerators = Integer.parseInt(env.get(Constants.GENERATOR_COUNT_KEY));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Couldn't get \"" + Constants.GENERATOR_COUNT_KEY
					+ "\" from the environment. Aborting.", e);
		}

		taskGen2SystemQueueName = generateSessionQueueName(Constants.TASK_GEN_2_SYSTEM_QUEUE_NAME);
		taskGen2System = connection.createChannel();
		taskGen2System.queueDeclare(taskGen2SystemQueueName, false, false, true, null);

		taskGen2EvalStoreQueueName = generateSessionQueueName(Constants.TASK_GEN_2_EVAL_STORAGE_QUEUE_NAME);
		taskGen2EvalStore = connection.createChannel();
		taskGen2EvalStore.queueDeclare(taskGen2EvalStoreQueueName, false, false, true, null);

		@SuppressWarnings("resource")
		GeneratedDataReceivingComponent receiver = this;
		dataGen2TaskGenQueueName = generateSessionQueueName(Constants.DATA_GEN_2_TASK_GEN_QUEUE_NAME);
		dataGen2TaskGen = connection.createChannel();
		dataGen2TaskGen.queueDeclare(dataGen2TaskGenQueueName, false, false, true, null);
		dataGen2TaskGen.basicConsume(dataGen2TaskGenQueueName, true, new DefaultConsumer(dataGen2TaskGen) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
					throws IOException {
				try {
					currentlyProcessedMessages.acquire();
				} catch (InterruptedException e) {
					throw new IOException("Interrupted while waiting for mutex.", e);
				}
				receiver.receiveGeneratedData(body);
				currentlyProcessedMessages.release();
			}
		});

		currentlyProcessedMessages = new Semaphore(maxParallelProcessedMsgs);
	}

	@Override
	public void run() throws Exception {
		boolean terminate = false;
		sendToCmdQueue(Commands.TASK_GENERATOR_READY_SIGNAL);
		// Wait for the start message
		startTaskGenMutex.acquire();

		while ((!terminate) || (dataGen2TaskGen.messageCount(dataGen2TaskGenQueueName) > 0)) {
			Thread.sleep(1000);
		}
		// Collect all open mutex counts to make sure that there is no message
		// that is still processed
		Thread.sleep(1000);
		currentlyProcessedMessages.acquire(maxParallelProcessedMsgs);
	}

	protected abstract void generateTask(byte[] data) throws Exception;

	protected synchronized String getNextTaskId() {
		String taskIdString = Long.toString(nextTaskId);
		nextTaskId += numberOfGenerators;
		return taskIdString;
	}

	@Override
	public void receiveCommand(byte command, byte[] data) {
		// If this is the signal to start the data generation
		if (command == Commands.TASK_GENERATOR_START_SIGNAL) {
			// release the mutex
			startTaskGenMutex.release();
		}
	}

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

	protected void sendTaskToSystemAdapter(String taskIdString, long timestamp, byte[] data) throws IOException {
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

	public int getGeneratorId() {
		return generatorId;
	}

	public int getNumberOfGenerators() {
		return numberOfGenerators;
	}

	public void setMaxParallelProcessedMsgs(int maxParallelProcessedMsgs) {
		this.maxParallelProcessedMsgs = maxParallelProcessedMsgs;
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
