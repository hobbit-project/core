package org.hobbit.core.components.dummy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.components.AbstractEvaluationStorage;
import org.hobbit.core.data.ResultPair;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.junit.Ignore;

@Ignore
public class DummyEvalStoreReceiver extends AbstractEvaluationStorage {

    private final List<String> receivedResponses = new ArrayList<String>();
    private final List<String> expectedResponses = new ArrayList<String>();

    @Override
    public void receiveResponseData(String taskId, long timestamp, InputStream stream) {
        StringBuilder builder = new StringBuilder();
        builder.append(taskId);
        builder.append(Long.toString(timestamp));
        try {
            builder.append(IOUtils.toString(stream, RabbitMQUtils.STRING_ENCODING));
        } catch (IOException e) {
            e.printStackTrace();
        }
        receivedResponses.add(builder.toString());
    }

    @Override
    public void receiveExpectedResponseData(String taskId, long timestamp, InputStream stream) {
        StringBuilder builder = new StringBuilder();
        builder.append(taskId);
        builder.append(Long.toString(timestamp));
        try {
            builder.append(IOUtils.toString(stream, RabbitMQUtils.STRING_ENCODING));
        } catch (IOException e) {
            e.printStackTrace();
        }
        expectedResponses.add(builder.toString());
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
