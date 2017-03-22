package org.hobbit.core.components;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.rabbit.DataSender;
import org.hobbit.core.rabbit.DataSenderImpl;
import org.hobbit.core.utils.SteppingIdGenerator;

public abstract class AbstractDataGenerator extends AbstractPlatformConnectorComponent {

    /**
     * Semaphore used to wait for the start signal of the benchmark controller.
     */
    private Semaphore startDataGenMutex = new Semaphore(0);
    /**
     * The ID of this generator.
     */
    protected int generatorId = -1;
    /**
     * The number of data generators that might be running in parallel.
     */
    protected int numberOfGenerators = -1;
    /**
     * Sender for transferring data to the task generators.
     */
    protected DataSender sender2TaskGen;
    /**
     * Sender for transferring data to the benchmarked system.
     */
    protected DataSender sender2System;

    public AbstractDataGenerator() {
    }

    public AbstractDataGenerator(DataSender sender2TaskGen, DataSender sender2System) {
        this.sender2TaskGen = sender2TaskGen;
        this.sender2System = sender2System;
    }

    @Override
    public void init() throws Exception {
        super.init();
        Map<String, String> env = System.getenv();

        // If the generator ID is not predefined
        if (generatorId < 0) {
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
        }

        // If the number of generators is not predefined
        if (numberOfGenerators < 0) {
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
        }

        if (sender2TaskGen == null) {
            sender2TaskGen = DataSenderImpl.builder()
                    .idGenerator(new SteppingIdGenerator(generatorId, numberOfGenerators))
                    .queue(this, generateSessionQueueName(Constants.DATA_GEN_2_TASK_GEN_QUEUE_NAME)).build();
        }
        if (sender2System == null) {
            sender2System = DataSenderImpl.builder()
                    .idGenerator(new SteppingIdGenerator(generatorId, numberOfGenerators))
                    .queue(this, generateSessionQueueName(Constants.DATA_GEN_2_SYSTEM_QUEUE_NAME)).build();
        }
    }

    @Override
    public void run() throws Exception {
        sendToCmdQueue(Commands.DATA_GENERATOR_READY_SIGNAL);
        // Wait for the start message
        startDataGenMutex.acquire();

        generateData();

        // Unfortunately, we have to wait until all messages are consumed
        sender2System.closeWhenFinished();
        sender2TaskGen.closeWhenFinished();
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
        sender2TaskGen.sendData(data);
    }

    protected void sendDataToTaskGenerator(InputStream stream) throws IOException {
        sender2TaskGen.sendData(stream);
    }

    protected void sendDataToSystemAdapter(byte[] data) throws IOException {
        sender2System.sendData(data);
    }

    protected void sendDataToSystemAdapter(InputStream stream) throws IOException {
        sender2System.sendData(stream);
    }

    public int getGeneratorId() {
        return generatorId;
    }

    public int getNumberOfGenerators() {
        return numberOfGenerators;
    }

    @Override
    public void close() throws IOException {
        sender2TaskGen.close();
        sender2System.close();
        super.close();
    }
}
