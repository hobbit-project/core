package org.hobbit.core.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.dummy.DummyComponentExecutor;
import org.hobbit.core.components.dummy.DummySystemReceiver;
import org.hobbit.core.components.dummy.DummyTaskGenReceiver;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the workflow of the {@link AbstractDataGenerator} class and the
 * communication between the {@link AbstractDataGenerator},
 * {@link AbstractSystemAdapter} and {@link AbstractTaskGenerator} classes. Note
 * that this test needs a running RabbitMQ instance. Its host name can be set
 * using the {@link #RABBIT_HOST_NAME} parameter.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
@RunWith(Parameterized.class)
public class DataGeneratorTest extends AbstractDataGenerator {

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testConfigs = new ArrayList<Object[]>();
        // We use only one single data generator
        testConfigs.add(new Object[] { 1, 10000 });
        // We use two data generators
        testConfigs.add(new Object[] { 2, 10000 });
        // We use ten data generators
        testConfigs.add(new Object[] { 10, 10000 });
        return testConfigs;
    }

    private static final String RABBIT_HOST_NAME = "192.168.99.100";

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private List<String> sentData = new ArrayList<String>();
    private int numberOfGenerators;
    private int numberOfMessages;

    public DataGeneratorTest(int numberOfGenerators, int numberOfMessages) {
        this.numberOfGenerators = numberOfGenerators;
        this.numberOfMessages = numberOfMessages;
    }

    @Override
    protected void generateData() throws Exception {
        byte data[];
        String msg;
        for (int i = 0; i < numberOfMessages; ++i) {
            msg = Integer.toString(i);
            sentData.add(msg);
            data = RabbitMQUtils.writeString(msg);
            sendDataToSystemAdapter(data);
            sendDataToTaskGenerator(data);
        }
    }

    @Test
    public void test() throws Exception {
        environmentVariables.set(Constants.RABBIT_MQ_HOST_NAME_KEY, RABBIT_HOST_NAME);
        environmentVariables.set(Constants.GENERATOR_ID_KEY, "0");
        environmentVariables.set(Constants.GENERATOR_COUNT_KEY, "1");
        environmentVariables.set(Constants.HOBBIT_SESSION_ID_KEY, "0");

        DummySystemReceiver system = new DummySystemReceiver();
        DummyComponentExecutor systemExecutor = new DummyComponentExecutor(system);
        Thread systemThread = new Thread(systemExecutor);
        systemThread.start();

        DummyTaskGenReceiver[] taskGens = new DummyTaskGenReceiver[numberOfGenerators];
        DummyComponentExecutor[] taskGenExecutors = new DummyComponentExecutor[numberOfGenerators];
        Thread[] taskGenThreads = new Thread[numberOfGenerators];
        for (int i = 0; i < taskGenThreads.length; ++i) {
            taskGens[i] = new DummyTaskGenReceiver();
            taskGenExecutors[i] = new DummyComponentExecutor(taskGens[i]);
            taskGenThreads[i] = new Thread(taskGenExecutors[i]);
            taskGenThreads[i].start();
        }

        try {
            init();
            // start dummy
            sendToCmdQueue(Commands.TASK_GENERATOR_START_SIGNAL);
            sendToCmdQueue(Commands.DATA_GENERATOR_START_SIGNAL);
            run();
            sendToCmdQueue(Commands.DATA_GENERATION_FINISHED);
            sendToCmdQueue(Commands.TASK_GENERATION_FINISHED);

            systemThread.join();
            for (int i = 0; i < taskGenThreads.length; ++i) {
                taskGenThreads[i].join();
            }

            Assert.assertTrue(systemExecutor.isSuccess());
            for (int i = 0; i < taskGenExecutors.length; ++i) {
                Assert.assertTrue(taskGenExecutors[i].isSuccess());
            }

            List<String> receivedData = system.getReceiveddata();
            Assert.assertArrayEquals(sentData.toArray(new String[sentData.size()]),
                    receivedData.toArray(new String[receivedData.size()]));
            // collect the data from all task generators
            receivedData = new ArrayList<String>();
            for (int i = 0; i < taskGens.length; ++i) {
                receivedData.addAll(taskGens[i].getReceiveddata());
            }
            Collections.sort(receivedData);
            Collections.sort(sentData);
            Assert.assertArrayEquals(sentData.toArray(new String[sentData.size()]),
                    receivedData.toArray(new String[receivedData.size()]));
        } finally {
            close();
        }
    }

}
