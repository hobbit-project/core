package org.hobbit.core.components;

import java.util.ArrayList;
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
public class DataGeneratorTest extends AbstractDataGenerator {

    private static final String RABBIT_HOST_NAME = "192.168.99.100";

    private static final int NUMBER_OF_MESSAGES = 10000;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private List<String> sentData = new ArrayList<String>();

    @Override
    protected void generateData() throws Exception {
        byte data[];
        String msg;
        for (int i = 0; i < NUMBER_OF_MESSAGES; ++i) {
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

        DummySystemReceiver system = new DummySystemReceiver();
        DummyComponentExecutor systemExecutor = new DummyComponentExecutor(system);
        Thread systemThread = new Thread(systemExecutor);
        systemThread.start();

        DummyTaskGenReceiver taskGen = new DummyTaskGenReceiver();
        DummyComponentExecutor taskGenExecutor = new DummyComponentExecutor(taskGen);
        Thread taskGenThread = new Thread(taskGenExecutor);
        taskGenThread.start();

        try {
            init();
            // start dummy
            sendToCmdQueue(Commands.TASK_GENERATOR_START_SIGNAL);
            sendToCmdQueue(Commands.DATA_GENERATOR_START_SIGNAL);
            run();
            sendToCmdQueue(Commands.DATA_GENERATION_FINISHED);
            sendToCmdQueue(Commands.TASK_GENERATION_FINISHED);

            systemThread.join();
            taskGenThread.join();

            Assert.assertTrue(systemExecutor.isSuccess());
            Assert.assertTrue(taskGenExecutor.isSuccess());

            List<String> receivedData = system.getReceiveddata();
            Assert.assertArrayEquals(sentData.toArray(new String[sentData.size()]),
                    receivedData.toArray(new String[receivedData.size()]));
            receivedData = taskGen.getReceiveddata();
            Assert.assertArrayEquals(sentData.toArray(new String[sentData.size()]),
                    receivedData.toArray(new String[receivedData.size()]));
        } finally {
            close();
        }
    }

}
