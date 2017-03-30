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

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.components.stream.AbstractStreamingEvaluationModule;

/**
 * This abstract class implements basic functions that can be used to implement
 * a task generator.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractEvaluationModule extends AbstractStreamingEvaluationModule {

    protected void evaluateResponse(InputStream expectedData, InputStream receivedData, long taskSentTimestamp,
            long responseReceivedTimestamp) throws Exception {
        evaluateResponse(IOUtils.toByteArray(expectedData), IOUtils.toByteArray(receivedData), taskSentTimestamp,
                responseReceivedTimestamp);
    }

    /**
     * Evaluates the given response pair.
     * 
     * @param expectedData
     *            the data that has been expected
     * @param receivedData
     *            the data that has been received from the system
     * @param taskSentTimestamp
     *            the time at which the task has been sent to the system
     * @param responseReceivedTimestamp
     *            the time at which the response has been received from the
     *            system
     * @throws Exception
     *             if an error occurs during the evaluation
     */
    protected abstract void evaluateResponse(byte[] expectedData, byte[] receivedData, long taskSentTimestamp,
            long responseReceivedTimestamp) throws Exception;

}
