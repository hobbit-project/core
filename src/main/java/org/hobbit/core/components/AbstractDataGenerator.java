package org.hobbit.core.components;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.data.RabbitQueue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;

public abstract class AbstractDataGenerator extends AbstractCommandReceivingComponent {

    private Semaphore startDataGenMutex = new Semaphore(0);
    private int generatorId;
    private int numberOfGenerators;
    // protected String dataGen2TaskGenQueueName;
    // protected Channel dataGen2TaskGen;
    // protected String dataGen2SystemQueueName;
    // protected Channel dataGen2System;
    protected RabbitQueue dataGen2TaskGenQueue;
    protected RabbitQueue dataGen2SystemQueue;

    @Override
    public void init() throws Exception {
        super.init();
        Map<String, String> env = System.getenv();

        if (!env.containsKey(Constants.GENERATOR_ID_KEY)) {
            throw new IllegalArgumentException(
                    "Couldn't get \"" + Constants.GENERATOR_ID_KEY + "\" from the environment. Aborting.");
        }
        try {
            generatorId = Integer.parseInt(env.get(Constants.GENERATOR_ID_KEY));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Couldn't get \"" + Constants.GENERATOR_ID_KEY + "\" from the environment. Aborting.", e);
        }

        if (!env.containsKey(Constants.GENERATOR_COUNT_KEY)) {
            throw new IllegalArgumentException(
                    "Couldn't get \"" + Constants.GENERATOR_COUNT_KEY + "\" from the environment. Aborting.");
        }
        try {
            numberOfGenerators = Integer.parseInt(env.get(Constants.GENERATOR_COUNT_KEY));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Couldn't get \"" + Constants.GENERATOR_COUNT_KEY + "\" from the environment. Aborting.", e);
        }

        // dataGen2TaskGenQueueName =
        // generateSessionQueueName(Constants.DATA_GEN_2_TASK_GEN_QUEUE_NAME);
        // dataGen2TaskGen = connection.createChannel();
        // dataGen2TaskGen.queueDeclare(dataGen2TaskGenQueueName, false, false,
        // true, null);
        //
        // dataGen2SystemQueueName =
        // generateSessionQueueName(Constants.DATA_GEN_2_SYSTEM_QUEUE_NAME);
        // dataGen2System = connection.createChannel();
        // dataGen2System.queueDeclare(dataGen2SystemQueueName, false, false,
        // true, null);
        dataGen2TaskGenQueue = createDefaultRabbitQueue(
                generateSessionQueueName(Constants.DATA_GEN_2_TASK_GEN_QUEUE_NAME));
        dataGen2SystemQueue = createDefaultRabbitQueue(
                generateSessionQueueName(Constants.DATA_GEN_2_SYSTEM_QUEUE_NAME));
    }

    @Override
    public void run() throws Exception {
        sendToCmdQueue(Commands.DATA_GENERATOR_READY_SIGNAL);
        // Wait for the start message
        startDataGenMutex.acquire();

        generateData();
    }

    protected abstract void generateData() throws Exception;

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // If this is the signal to start the data generation
        if (command == Commands.DATA_GENERATOR_START_SIGNAL) {
            // release the mutex
            startDataGenMutex.release();
        }
    }

    protected void sendDataToTaskGenerator(byte[] data) throws IOException {
        // dataGen2TaskGen.basicPublish("", dataGen2TaskGenQueueName,
        // MessageProperties.PERSISTENT_BASIC, data);
        dataGen2TaskGenQueue.channel.basicPublish("", dataGen2TaskGenQueue.name,
                MessageProperties.PERSISTENT_BASIC, data);
    }

    protected void sendDataToSystemAdapter(byte[] data) throws IOException {
//        dataGen2System.basicPublish("", dataGen2SystemQueueName, MessageProperties.PERSISTENT_BASIC, data);
        dataGen2SystemQueue.channel.basicPublish("", dataGen2SystemQueue.name,
                MessageProperties.PERSISTENT_BASIC, data);
    }

    public int getGeneratorId() {
        return generatorId;
    }

    public int getNumberOfGenerators() {
        return numberOfGenerators;
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(dataGen2TaskGenQueue);
        IOUtils.closeQuietly(dataGen2SystemQueue);
//        if (dataGen2TaskGen != null) {
//            try {
//                dataGen2TaskGen.close();
//            } catch (Exception e) {
//            }
//        }
//        if (dataGen2System != null) {
//            try {
//                dataGen2System.close();
//            } catch (Exception e) {
//            }
//        }
        super.close();
    }
}
