package org.hobbit.core.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.dummy.DummyComponentExecutor;
import org.hobbit.core.components.dummy.DummyDataCreator;
import org.hobbit.core.components.dummy.DummyEvalStoreReceiver;
import org.hobbit.core.components.dummy.DummySystemReceiver;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the workflow of the {@link AbstractTaskGenerator} class and the
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
public class TaskGeneratorTest extends AbstractTaskGenerator {

    private static final String RABBIT_HOST_NAME = "192.168.99.100";

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testConfigs = new ArrayList<Object[]>();
        // We use only one single data generator without parallel message
        // processing
        testConfigs.add(new Object[] { 1, 10000, 1 });
        // We use only one single data generator with parallel message
        // processing (max 100)
        testConfigs.add(new Object[] { 1, 10000, 100 });
        // We use two data generators without parallel message processing
        testConfigs.add(new Object[] { 2, 10000, 1 });
        // We use two data generators with parallel message processing (max 100)
        testConfigs.add(new Object[] { 2, 10000, 100 });
        // We use ten data generators without parallel message processing
        testConfigs.add(new Object[] { 10, 1000, 1 });
        // We use ten data generators with parallel message processing (max 100)
        testConfigs.add(new Object[] { 10, 1000, 100 });
        return testConfigs;
    }

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private List<String> sentTasks = new ArrayList<String>();
    private List<String> expectedResponses = new ArrayList<String>();
    private int terminationCount = 0;
    private int numberOfGenerators;
    private int numberOfMessages;

    public TaskGeneratorTest(int numberOfGenerators, int numberOfMessages, int numberOfMessagesInParallel) {
        super(numberOfMessagesInParallel);
        this.numberOfGenerators = numberOfGenerators;
        this.numberOfMessages = numberOfMessages;
    }

    @Test
    public void test() throws Exception {
        environmentVariables.set(Constants.RABBIT_MQ_HOST_NAME_KEY, RABBIT_HOST_NAME);
        environmentVariables.set(Constants.GENERATOR_ID_KEY, "0");
        environmentVariables.set(Constants.GENERATOR_COUNT_KEY, "1");
        environmentVariables.set(Constants.HOBBIT_SESSION_ID_KEY, "0");

        Thread[] dataGenThreads = new Thread[numberOfGenerators];
        DummyComponentExecutor[] dataGenExecutors = new DummyComponentExecutor[numberOfGenerators];
        for (int i = 0; i < dataGenThreads.length; ++i) {
            DummyDataCreator dataGenerator = new DummyDataCreator(numberOfMessages);
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

        DummySystemReceiver system = new DummySystemReceiver();
        DummyComponentExecutor systemExecutor = new DummyComponentExecutor(system);
        Thread systemThread = new Thread(systemExecutor);
        systemThread.start();

        DummyEvalStoreReceiver evalStore = new DummyEvalStoreReceiver();
        DummyComponentExecutor evalStoreExecutor = new DummyComponentExecutor(evalStore);
        Thread evalStoreThread = new Thread(evalStoreExecutor);
        evalStoreThread.start();

        try {
            init();
            // start dummy
            sendToCmdQueue(Commands.TASK_GENERATOR_START_SIGNAL);
            sendToCmdQueue(Commands.DATA_GENERATOR_START_SIGNAL);

            run();
            sendToCmdQueue(Commands.TASK_GENERATION_FINISHED);
            sendToCmdQueue(Commands.EVAL_STORAGE_TERMINATE);

            for (int i = 0; i < dataGenThreads.length; ++i) {
                dataGenThreads[i].join();
            }
            systemThread.join();
            evalStoreThread.join();

            for (int i = 0; i < dataGenExecutors.length; ++i) {
                Assert.assertTrue(dataGenExecutors[i].isSuccess());
            }
            Assert.assertTrue(systemExecutor.isSuccess());
            Assert.assertTrue(evalStoreExecutor.isSuccess());

            List<String> receivedData = system.getReceivedtasks();
            Assert.assertArrayEquals(sentTasks.toArray(new String[sentTasks.size()]),
                    receivedData.toArray(new String[receivedData.size()]));
            receivedData = evalStore.getExpectedResponses();
            Assert.assertArrayEquals(expectedResponses.toArray(new String[expectedResponses.size()]),
                    receivedData.toArray(new String[receivedData.size()]));
        } finally {
            close();
        }
    }

    @Override
    protected void generateTask(byte[] data) throws Exception {
        String taskIdString = getNextTaskId();
        long timestamp = System.currentTimeMillis();
        sendTaskToSystemAdapter(taskIdString, data);
        String dataString = RabbitMQUtils.readString(data);
        StringBuilder builder = new StringBuilder();
        builder.append(taskIdString);
        builder.append(dataString);
        sentTasks.add(builder.toString());

        sendTaskToEvalStorage(taskIdString, timestamp, data);
        builder.delete(0, builder.length());
        builder.append(taskIdString);
        builder.append(Long.toString(timestamp));
        builder.append(dataString);
        expectedResponses.add(builder.toString());
    }

    protected synchronized void dataGeneratorTerminated() {
        ++terminationCount;
        if (terminationCount == numberOfGenerators) {
            try {
                sendToCmdQueue(Commands.DATA_GENERATION_FINISHED);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
