package org.hobbit.core.components;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.dummy.DummyComponentExecutor;
import org.hobbit.core.components.dummy.DummyDataCreator;
import org.hobbit.core.components.dummy.DummyEvalStoreReceiver;
import org.hobbit.core.components.dummy.DummyTaskGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the workflow of the {@link AbstractSystemAdapter} class and the
 * communication between the {@link AbstractDataGenerator},
 * {@link AbstractSystemAdapter}, {@link AbstractEvaluationStorage} and
 * {@link AbstractTaskGenerator} classes. Note that this test needs a running
 * RabbitMQ instance. Its host name can be set using the
 * {@link #RABBIT_HOST_NAME} parameter.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
@RunWith(Parameterized.class)
public class SystemAdapterTest extends AbstractSystemAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemAdapterTest.class);

    private static final String RABBIT_HOST_NAME = "192.168.99.100";

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testConfigs = new ArrayList<Object[]>();
        // We use only one single data generator without parallel message
        // processing
        testConfigs.add(new Object[] { 1, 1, 10000, 1 });
        // We use only one single data generator with parallel message
        // processing (max 100)
        testConfigs.add(new Object[] { 1, 1, 10000, 100 });
        // We use only one single data generator with parallel message
        // processing (max 100)
        testConfigs.add(new Object[] { 1, 2, 10000, 1 });
        // We use only one single data generator with parallel message
        // processing (max 100)
        testConfigs.add(new Object[] { 1, 2, 10000, 100 });
        // We use two data generators without parallel message processing
        testConfigs.add(new Object[] { 2, 1, 10000, 1 });
        // We use two data generators with parallel message processing (max 100)
        testConfigs.add(new Object[] { 2, 1, 10000, 100 });
        // We use two data generators without parallel message processing
        testConfigs.add(new Object[] { 2, 2, 10000, 1 });
        // We use two data generators with parallel message processing (max 100)
        testConfigs.add(new Object[] { 2, 2, 10000, 100 });
        return testConfigs;
    }

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private List<String> receivedData = Collections.synchronizedList(new ArrayList<String>());
    private int dataTerminationCount = 0;
    private int taskTerminationCount = 0;
    private int numberOfDataGenerators;
    private int numberOfTaskGenerators;
    private int numberOfMessages;
    private Semaphore dataGensReady = new Semaphore(0);
    private Semaphore taskGensReady = new Semaphore(0);
    private Semaphore evalStoreReady = new Semaphore(0);

    public SystemAdapterTest(int numberOfDataGenerators, int numberOfTaskGenerators, int numberOfMessages,
            int numberOfMessagesInParallel) {
        super(numberOfMessagesInParallel);
        this.numberOfDataGenerators = numberOfDataGenerators;
        this.numberOfTaskGenerators = numberOfTaskGenerators;
        this.numberOfMessages = numberOfMessages;
    }

    @Test(timeout = 30000)
    public void test() throws Exception {
        environmentVariables.set(Constants.RABBIT_MQ_HOST_NAME_KEY, RABBIT_HOST_NAME);
        environmentVariables.set(Constants.HOBBIT_SESSION_ID_KEY, "0");

        init();

        Thread[] dataGenThreads = new Thread[numberOfDataGenerators];
        DummyComponentExecutor[] dataGenExecutors = new DummyComponentExecutor[numberOfDataGenerators];
        for (int i = 0; i < dataGenThreads.length; ++i) {
            DummyDataCreator dataGenerator = new DummyDataCreator(i, numberOfDataGenerators, numberOfMessages);
            dataGenExecutors[i] = new DummyComponentExecutor(dataGenerator) {
                @Override
                public void run() {
                    super.run();
                    dataGeneratorTerminated();
                }
            };
            dataGenThreads[i] = new Thread(dataGenExecutors[i]);
            dataGenThreads[i].start();
        }
        Thread[] taskGenThreads = new Thread[numberOfTaskGenerators];
        DummyComponentExecutor[] taskGenExecutors = new DummyComponentExecutor[numberOfTaskGenerators];
        for (int i = 0; i < taskGenThreads.length; ++i) {
            DummyTaskGenerator taskGenerator = new DummyTaskGenerator(i, numberOfTaskGenerators);
            taskGenExecutors[i] = new DummyComponentExecutor(taskGenerator) {
                @Override
                public void run() {
                    super.run();
                    taskGeneratorTerminated();
                }
            };
            taskGenThreads[i] = new Thread(taskGenExecutors[i]);
            taskGenThreads[i].start();
        }

        DummyEvalStoreReceiver evalStore = new DummyEvalStoreReceiver() {
            @Override
            public void receiveResponseData(String taskId, long timestamp, InputStream stream) {
                try {
                    receivedResponses.add(IOUtils.toString(stream, RabbitMQUtils.STRING_ENCODING));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        DummyComponentExecutor evalStoreExecutor = new DummyComponentExecutor(evalStore);
        Thread evalStoreThread = new Thread(evalStoreExecutor);
        evalStoreThread.start();

        dataGensReady.acquire(numberOfDataGenerators);
        taskGensReady.acquire(numberOfTaskGenerators);
        evalStoreReady.acquire();

        try {
            // start dummy
            sendToCmdQueue(Commands.TASK_GENERATOR_START_SIGNAL);
            sendToCmdQueue(Commands.DATA_GENERATOR_START_SIGNAL);

            run();
            sendToCmdQueue(Commands.TASK_GENERATION_FINISHED);
            sendToCmdQueue(Commands.EVAL_STORAGE_TERMINATE);

            for (int i = 0; i < dataGenThreads.length; ++i) {
                dataGenThreads[i].join();
            }
            for (int i = 0; i < taskGenThreads.length; ++i) {
                taskGenThreads[i].join();
            }
            evalStoreThread.join();

            for (int i = 0; i < dataGenExecutors.length; ++i) {
                Assert.assertTrue(dataGenExecutors[i].isSuccess());
            }
            for (int i = 0; i < taskGenExecutors.length; ++i) {
                Assert.assertTrue(taskGenExecutors[i].isSuccess());
            }
            Assert.assertTrue(evalStoreExecutor.isSuccess());

            Assert.assertEquals(numberOfDataGenerators * numberOfMessages, receivedData.size());
            List<String> receivedTaskData = evalStore.getReceivedResponses();
            Assert.assertEquals(numberOfDataGenerators * numberOfMessages, receivedTaskData.size());
            Collections.sort(receivedData);
            Collections.sort(receivedTaskData);
            Assert.assertArrayEquals(receivedData.toArray(new String[receivedData.size()]),
                    receivedTaskData.toArray(new String[receivedTaskData.size()]));
        } finally {
            close();
        }
    }

    protected synchronized void dataGeneratorTerminated() {
        ++dataTerminationCount;
        if (dataTerminationCount == numberOfDataGenerators) {
            try {
                sendToCmdQueue(Commands.DATA_GENERATION_FINISHED);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected synchronized void taskGeneratorTerminated() {
        ++taskTerminationCount;
        if (taskTerminationCount == numberOfTaskGenerators) {
            try {
                sendToCmdQueue(Commands.TASK_GENERATION_FINISHED);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        LOGGER.info("received command {}", Commands.toString(command));
        if (command == Commands.DATA_GENERATOR_READY_SIGNAL) {
            dataGensReady.release();
        }
        if (command == Commands.TASK_GENERATOR_READY_SIGNAL) {
            taskGensReady.release();
        }
        if (command == Commands.EVAL_STORAGE_READY_SIGNAL) {
            evalStoreReady.release();
        }
        super.receiveCommand(command, data);
    }

    @Override
    public void receiveGeneratedData(byte[] data) {
        receivedData.add(RabbitMQUtils.readString(data));
    }

    @Override
    public void receiveGeneratedTask(String taskId, byte[] data) {
        try {
            sendResultToEvalStorage(taskId, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
