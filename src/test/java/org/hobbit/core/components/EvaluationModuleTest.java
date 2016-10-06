package org.hobbit.core.components;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.dummy.DummyComponentExecutor;
import org.hobbit.core.components.test.InMemoryEvaluationStore;
import org.hobbit.core.components.test.InMemoryEvaluationStore.ResultImpl;
import org.hobbit.core.components.test.InMemoryEvaluationStore.ResultPairImpl;
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
public class EvaluationModuleTest extends AbstractEvaluationModule {

    private static final String RABBIT_HOST_NAME = "192.168.99.100";

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private Map<String, ResultPairImpl> expectedResults = new HashMap<>();
    private int numberOfMessages = 10000;
    private Set<String> receivedResults = new HashSet<>();

    @Test
    public void test() throws Exception {
        environmentVariables.set(Constants.RABBIT_MQ_HOST_NAME_KEY, RABBIT_HOST_NAME);
        environmentVariables.set(Constants.HOBBIT_SESSION_ID_KEY, "0");

        // Create the eval store and add some data
        InMemoryEvaluationStore evalStore = new InMemoryEvaluationStore();
        Random rand = new Random();

        String taskId;
        ResultImpl expResult, sysResult;
        ResultPairImpl pair;
        for (int i = 0; i < numberOfMessages; ++i) {
            taskId = Integer.toString(i);
            pair = new ResultPairImpl();

            expResult = new ResultImpl(rand.nextLong(), RabbitMQUtils.writeString(taskId));
            evalStore.putResult(true, taskId, expResult.getSentTimestamp(), expResult.getData());
            pair.setExpected(expResult);

            sysResult = new ResultImpl(rand.nextLong(), RabbitMQUtils.writeString(Integer.toString(rand.nextInt())));
            evalStore.putResult(true, taskId, sysResult.getSentTimestamp(), sysResult.getData());
            pair.setActual(sysResult);

            expectedResults.put(taskId, pair);
        }
        DummyComponentExecutor evalStoreExecutor = new DummyComponentExecutor(evalStore);
        Thread evalStoreThread = new Thread(evalStoreExecutor);
        evalStoreThread.start();

        try {
            init();

            run();
            sendToCmdQueue(Commands.EVAL_STORAGE_TERMINATE);

            evalStoreThread.join();

            Assert.assertTrue(evalStoreExecutor.isSuccess());

            String expectedTaskIds[] = expectedResults.keySet().toArray(new String[expectedResults.size()]);
            Arrays.sort(expectedTaskIds);
            String receivedTaskIds[] = receivedResults.toArray(new String[receivedResults.size()]);
            Arrays.sort(receivedTaskIds);
            Assert.assertArrayEquals(expectedTaskIds, receivedTaskIds);
        } finally {
            close();
        }
    }

    @Override
    protected void evaluateResponse(byte[] expectedData, byte[] receivedData, long taskSentTimestamp,
            long responseReceivedTimestamp) throws Exception {
        String taskId = RabbitMQUtils.readString(expectedData);
        Assert.assertTrue(taskId + " is not known.", expectedResults.containsKey(taskId));
        ResultPairImpl pair = expectedResults.get(taskId);
        Assert.assertArrayEquals(pair.getExpected().getData(), expectedData);
        Assert.assertArrayEquals(pair.getActual().getData(), receivedData);
        Assert.assertEquals(pair.getExpected().getSentTimestamp(), taskSentTimestamp);
        Assert.assertEquals(pair.getActual().getSentTimestamp(), responseReceivedTimestamp);
        receivedResults.add(taskId);
    }

    @Override
    protected Model summarizeEvaluation() throws Exception {
        return ModelFactory.createDefaultModel();
    }

}
