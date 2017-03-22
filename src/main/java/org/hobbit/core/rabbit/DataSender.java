package org.hobbit.core.rabbit;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * An interface of a class sending (large) data to a queue using data
 * identifiers to mark single packages as part of a larger set of data.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface DataSender extends Closeable {

    /**
     * Send the given data to the queue.
     * 
     * @param data
     *            the data that should be sent
     * @throws IOException
     *             if an exception occurs during the communication with the
     *             queue
     */
    public void sendData(byte[] data) throws IOException;

    /**
     * Send the given data to the queue using the given Id as identifier.
     * 
     * @param data
     *            the data that should be sent
     * @param dataId
     *            the Id that is used as identifier
     * @throws IOException
     *             if an exception occurs during the communication with the
     *             queue
     */
    public void sendData(byte[] data, String dataId) throws IOException;

    /**
     * Send the given data to the queue.
     * 
     * @param is
     *            an InputStream from which the data is read
     * @throws IOException
     *             if an exception occurs during the communication with the
     *             queue
     */
    public void sendData(InputStream is) throws IOException;

    /**
     * Send the given data to the queue using the given Id as identifier.
     * 
     * @param is
     *            an InputStream from which the data is read
     * @param dataId
     *            the Id that is used as identifier
     * @throws IOException
     *             if an exception occurs during the communication with the
     *             queue
     */
    public void sendData(InputStream is, String dataId) throws IOException;

    /**
     * A blocking method that closes the sender when its work is done, i.e., all
     * messages have been consumed by receivers from the queue.
     */
    public void closeWhenFinished();

}
