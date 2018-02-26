/**
 * This file is part of core.
 *
 * core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with core.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.core.components.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.components.AbstractEvaluationStorage;
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
    private Map<String, ResultPair> results = Collections.synchronizedMap(new HashMap<String, ResultPair>());

    @Override
    public void receiveResponseData(String taskId, long timestamp, InputStream stream) {
        try {
            putResult(false, taskId, timestamp, IOUtils.toByteArray(stream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receiveExpectedResponseData(String taskId, long timestamp, InputStream stream) {
        try {
            putResult(true, taskId, timestamp, IOUtils.toByteArray(stream));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            pair.setExpected(putTimestampInFront(timestamp, data));
        } else {
            pair.setActual(putTimestampInFront(timestamp, data));
        }
    }

    @Override
    protected Iterator<ResultPair> createIterator() {
        return results.values().iterator();
    }

    /**
     * Copies timestamp and data in one single byte array starting with the
     * timestamp.
     * 
     * @param timestamp
     *            a timestamp that belongs to the given data
     * @param data
     * @return a single array containing both
     */
    public static byte[] putTimestampInFront(long timestamp, byte[] data) {
        byte[] storedData = new byte[data.length + Long.BYTES];
        ByteBuffer.wrap(storedData).putLong(timestamp);
        System.arraycopy(data, 0, storedData, Long.BYTES, data.length);
        return storedData;
    }

    /**
     * A simple structure implementing the {@link ResultPair} interface.
     * 
     * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
     *
     */
    public static class ResultPairImpl implements ResultPair {

        private byte[] actual;
        private byte[] expected;

        public void setActual(byte[] actual) {
            this.actual = actual;
        }

        public void setExpected(byte[] expected) {
            this.expected = expected;
        }

        @Override
        public InputStream getActual() {
            return new ByteArrayInputStream(actual != null ? actual : new byte[Long.BYTES]);
        }

        @Override
        public InputStream getExpected() {
            return new ByteArrayInputStream(expected != null ? expected : new byte[Long.BYTES]);
        }
        
        public byte[] getActualAsArray() {
            return actual;
        }
        
        public byte[] getExpectedAsArray() {
            return expected;
        }
    }
}
