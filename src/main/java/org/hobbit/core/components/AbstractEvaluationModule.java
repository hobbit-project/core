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
