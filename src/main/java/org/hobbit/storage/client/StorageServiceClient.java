package org.hobbit.storage.client;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;

import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Constants;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.RabbitRpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Connection;

/**
 * Simple client of the storage service implementing a synchronized
 * communication.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class StorageServiceClient extends RabbitRpcClient implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageServiceClient.class);

    /**
     * Creates a StorageServiceClient using the given RabbitMQ
     * {@link Connection}.
     * 
     * @param connection
     *            RabbitMQ connection used for the communication
     * @return a StorageServiceClient instance
     * @throws IOException
     *             if a problem occurs during the creation of the
     *             {@link #channel}, the response queue or the {@link #consumer}
     *             .
     */
    public static StorageServiceClient create(Connection connection) throws IOException {
        StorageServiceClient client = new StorageServiceClient();
        try {
            client.init(connection);
            return client;
        } catch (Exception e) {
            client.close();
            throw e;
        }
    }

    /**
     * Constructor.
     */
    protected StorageServiceClient() {
        super(Constants.STORAGE_QUEUE_NAME);
    }

    /**
     * Sends the given ASK query to the storage service and returns a boolean
     * value or throws an Exception if an error occurs, the service needs too
     * much time to respond or the response couldn't be parsed.
     * 
     * @param query
     *            ASK query
     * @return result for the query or <code>false</code> if an error occurs
     * @throws Exception
     *             if no response has been received or the response couldn't be
     *             parsed.
     */
    public boolean sendAskQuery(String query) throws Exception {
        byte[] response = request(RabbitMQUtils.writeString(query));
        if (response != null) {
            try {
                return Boolean.parseBoolean(RabbitMQUtils.readString(response));
            } catch (Exception e) {
                throw new Exception("Couldn't parse boolean value from response. Returning false.", e);
            }
        }
        throw new Exception("Couldn't get response for query.");
    }

    /**
     * Sends the given CONSTRUCT query to the storage service and returns a
     * {@link Model} value or <code>null</code> if an error occurs, the service
     * needs too much time to respond or the response couldn't be parsed.
     * 
     * @param query
     *            CONSTRUCT query
     * @return result for the query or <code>null</code>
     */
    public Model sendConstructQuery(String query) {
        byte[] response = request(RabbitMQUtils.writeString(query));
        if (response != null) {
            try {
                return RabbitMQUtils.readModel(response);
            } catch (Exception e) {
                LOGGER.error("The response couldn't be parsed. Returning null.", e);
            }
        }
        return null;
    }

    /**
     * Sends the given DESCRIBE query to the storage service and returns a
     * {@link Model} value or <code>null</code> if an error occurs, the service
     * needs too much time to respond or the response couldn't be parsed.
     * 
     * @param query
     *            DESCRIBE query
     * @return result for the query or <code>null</code>
     */
    public Model sendDescribeQuery(String query) {
        byte[] response = request(RabbitMQUtils.writeString(query));
        if (response != null) {
            try {
                return RabbitMQUtils.readModel(response);
            } catch (Exception e) {
                LOGGER.error("The response couldn't be parsed. Returning null.", e);
            }
        }
        return null;
    }

    /**
     * Sends the given SELECT query to the storage service and returns a
     * {@link ResultSet} value or <code>null</code> if an error occurs, the
     * service needs too much time to respond or the response couldn't be
     * parsed.
     * 
     * @param query
     *            SELECT query
     * @return result for the query or <code>null</code>
     */
    public ResultSet sendSelectQuery(String query) {
        byte[] response = request(RabbitMQUtils.writeString(query));
        if (response != null) {
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(response);
                ResultSet result = ResultSetFactory.fromJSON(in);
                return result;
            } catch (Exception e) {
                LOGGER.error("The response couldn't be parsed. Returning null.", e);
            }
        }
        return null;
    }

}
