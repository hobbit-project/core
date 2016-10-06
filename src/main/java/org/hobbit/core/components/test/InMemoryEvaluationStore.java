package org.hobbit.core.components.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hobbit.core.components.AbstractEvaluationStorage;
import org.hobbit.core.data.Result;
import org.hobbit.core.data.ResultPair;

/**
 * Simple in-memory implementation of an evaluation storage that can be used for
 * testing purposes.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class InMemoryEvaluationStore extends AbstractEvaluationStorage {

    /**
     * Map containing a mapping from task Ids to result pairs.
     */
    private Map<String, ResultPair> results = new HashMap<String, ResultPair>();

    @Override
    public void receiveResponseData(String taskId, long timestamp, byte[] data) {
        putResult(false, taskId, timestamp, data);
    }

    @Override
    public void receiveExpectedResponseData(String taskId, long timestamp, byte[] data) {
        putResult(true, taskId, timestamp, data);
    }

    /**
     * Adds the given result to the map of results.
     * 
     * @param isExpectedResult
     *            true if the result has been received from a task generator,
     *            i.e., is the expected result for a task
     * @param taskId
     *            id of the task
     * @param timestamp
     *            time stamp for the task result
     * @param data
     *            the result
     */
    public synchronized void putResult(boolean isExpectedResult, String taskId, long timestamp, byte[] data) {
        ResultPairImpl pair;
        if (results.containsKey(taskId)) {
            pair = (ResultPairImpl) results.get(taskId);
        } else {
            pair = new ResultPairImpl();
            results.put(taskId, pair);
        }
        if (isExpectedResult) {
            pair.setActual(new ResultImpl(timestamp, data));
        } else {
            pair.setExpected(new ResultImpl(timestamp, data));
        }
    }

    @Override
    protected Iterator<ResultPair> createIterator() {
        return results.values().iterator();
    }

    /**
     * A simple structure implementing the {@link ResultPair} interface.
     * 
     * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
     *
     */
    public static class ResultPairImpl implements ResultPair {

        private Result actual;
        private Result expected;

        public void setActual(Result actual) {
            this.actual = actual;
        }

        public void setExpected(Result expected) {
            this.expected = expected;
        }

        @Override
        public Result getActual() {
            return actual;
        }

        @Override
        public Result getExpected() {
            return expected;
        }
    }

    /**
     * A simple structure implementing the {@link Result} interface
     * 
     * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
     *
     */
    public static class ResultImpl implements Result {

        private long sentTimestamp;
        private byte[] data;

        public ResultImpl(long sentTimestamp, byte[] data) {
            this.sentTimestamp = sentTimestamp;
            this.data = data;
        }

        public void setSentTimestamp(long sentTimestamp) {
            this.sentTimestamp = sentTimestamp;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        @Override
        public long getSentTimestamp() {
            return sentTimestamp;
        }

        @Override
        public byte[] getData() {
            return data;
        }

    }
}
