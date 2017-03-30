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
package org.hobbit.core.components;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.components.stream.AbstractStreamingSystemAdapter;
import org.hobbit.core.rabbit.DataReceiver;
import org.hobbit.core.rabbit.DataSender;

/**
 * This abstract class implements basic functions that can be used to implement
 * a system adapter.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractSystemAdapter extends AbstractStreamingSystemAdapter
        implements GeneratedDataReceivingComponent, TaskReceivingComponent {

    public AbstractSystemAdapter() {
        super();
    }

    public AbstractSystemAdapter(int maxParallelProcessedMsgs) {
        super(maxParallelProcessedMsgs);
    }

    public AbstractSystemAdapter(DataSender sender2EvalStore, DataReceiver dataReceiver, DataReceiver taskReceiver) {
        super(sender2EvalStore, dataReceiver, taskReceiver);
    }

    /**
     * This method sends the given result data for the task with the given task
     * id to the evaluation storage.
     * 
     * @param taskIdString
     *            the id of the task
     * @param data
     *            the data of the task
     * @throws IOException
     *             if there is an error during the sending
     */
    protected void sendResultToEvalStorage(String taskIdString, byte[] data) throws IOException {
        sendResultToEvalStorage(taskIdString, new ByteArrayInputStream(data));
    }

    @Override
    public void receiveGeneratedData(InputStream dataStream) {
        try {
            receiveGeneratedData(IOUtils.toByteArray(dataStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receiveGeneratedTask(String taskId, InputStream dataStream) {
        try {
            receiveGeneratedTask(taskId, IOUtils.toByteArray(dataStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
