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
package org.hobbit.core.components.dummy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.components.AbstractEvaluationStorage;
import org.hobbit.core.data.ResultPair;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.junit.Ignore;

@Ignore
public class DummyEvalStoreReceiver extends AbstractEvaluationStorage {

    protected final boolean addTaskIds;
    protected final boolean addTimeStamps;
    protected final List<String> receivedResponses = Collections.synchronizedList(new ArrayList<String>());
    protected final List<String> expectedResponses = Collections.synchronizedList(new ArrayList<String>());
    
    public DummyEvalStoreReceiver() {
        this(false, false);
    }
    
    public DummyEvalStoreReceiver(boolean addTaskIds, boolean addTimeStamps) {
        this.addTaskIds = addTaskIds;
        this.addTimeStamps = addTimeStamps;
    }

    @Override
    public void receiveResponseData(String taskId, long timestamp, InputStream stream) {
        receivedResponses.add(createStoredString(taskId, timestamp, stream, addTimeStamps, addTaskIds));
    }

    @Override
    public void receiveExpectedResponseData(String taskId, long timestamp, InputStream stream) {
        expectedResponses.add(createStoredString(taskId, timestamp, stream, addTimeStamps, addTaskIds));
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

    public static final String createStoredString(String taskId, long timestamp, byte[] data, boolean addTimeStamp,
            boolean addTaskId) {
        return createStoredString(taskId, timestamp, new ByteArrayInputStream(data), addTimeStamp, addTaskId);
    }

    public static final String createStoredString(String taskId, long timestamp, InputStream stream,
            boolean addTimeStamp, boolean addTaskId) {
        StringBuilder builder = new StringBuilder();
        if (addTaskId) {
            builder.append(taskId);
        }
        if (addTimeStamp) {
            builder.append(Long.toString(timestamp));
        }
        try {
            builder.append(IOUtils.toString(stream, RabbitMQUtils.STRING_ENCODING));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

}
