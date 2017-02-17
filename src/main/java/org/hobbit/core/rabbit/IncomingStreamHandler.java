package org.hobbit.core.rabbit;

import java.io.InputStream;

/**
 * This interface defines a method for handling incoming streaming data.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface IncomingStreamHandler {

    /**
     * Method handling the given InputStream. Note that it is assumed that the
     * method blocks until the input stream is read. After the program leaves
     * the method, the stream is closed.
     * 
     * @param streamId
     *            the id of the stream
     * @param stream
     *            the input stream containing the incoming data
     */
    public void handleIncomingStream(String streamId, InputStream stream);
}
