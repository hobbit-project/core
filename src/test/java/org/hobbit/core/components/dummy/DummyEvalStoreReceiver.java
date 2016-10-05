package org.hobbit.core.components.dummy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hobbit.core.components.AbstractEvaluationStorage;
import org.hobbit.core.data.ResultPair;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.junit.Ignore;

@Ignore
public class DummyEvalStoreReceiver extends AbstractEvaluationStorage {

    private final List<String> receivedResponses = new ArrayList<String>();
    private final List<String> expectedResponses = new ArrayList<String>();

    @Override
    public void receiveResponseData(String taskId, long timestamp, byte[] data) {
        StringBuilder builder = new StringBuilder();
        builder.append(taskId);
        builder.append(Long.toString(timestamp));
        builder.append(RabbitMQUtils.readString(data));
        receivedResponses.add(builder.toString());
    }

    @Override
    public void receiveExpectedResponseData(String taskId, long timestamp, byte[] data) {
        StringBuilder builder = new StringBuilder();
        builder.append(taskId);
        builder.append(Long.toString(timestamp));
        builder.append(RabbitMQUtils.readString(data));
        receivedResponses.add(builder.toString());
    }

    @Override
    protected Iterator<ResultPair> createIterator() {
        return null;
    }

    /**
     * @return the received responses
     */
    public List<String> getReceivedResponses() {
        return receivedResponses;
    }

    /**
     * @return the expected responses
     */
    public List<String> getExpectedResponses() {
        return expectedResponses;
    }

}
