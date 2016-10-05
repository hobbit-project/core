package org.hobbit.core.components;

import java.io.IOException;
import java.util.ArrayList;
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
public class TaskGeneratorTest extends AbstractTaskGenerator {

    private static final String RABBIT_HOST_NAME = "192.168.99.100";

    private static final int NUMBER_OF_MESSAGES = 10000;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private List<String> sentTasks = new ArrayList<String>();
    private List<String> expectedResponses = new ArrayList<String>();

    @Test
    public void test() throws Exception {
        environmentVariables.set(Constants.RABBIT_MQ_HOST_NAME_KEY, RABBIT_HOST_NAME);
        environmentVariables.set(Constants.GENERATOR_ID_KEY, "0");
        environmentVariables.set(Constants.GENERATOR_COUNT_KEY, "1");

        DummyDataCreator dataGenerator = new DummyDataCreator(NUMBER_OF_MESSAGES);
        DummyComponentExecutor dataGenExecutor = new DummyComponentExecutor(dataGenerator);
        Thread dataGenThread = new Thread(dataGenExecutor);
        dataGenThread.start();

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

            // Start Thread that sends terminated signal
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    try {
                        sendToCmdQueue(Commands.DATA_GENERATION_FINISHED);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            })).start();

            run();
            sendToCmdQueue(Commands.TASK_GENERATION_FINISHED);
            sendToCmdQueue(Commands.EVAL_STORAGE_TERMINATE);

            dataGenThread.join();
            systemThread.join();
            evalStoreThread.join();

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
        builder.append(dataString);
        expectedResponses.add(builder.toString());
    }

}
