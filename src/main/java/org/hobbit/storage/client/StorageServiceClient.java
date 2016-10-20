package org.hobbit.storage.client;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
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
public class StorageServiceClient implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageServiceClient.class);

    /**
     * Creates a StorageServiceClient using the given RabbitMQ
     * {@link Connection}.
     * 
     * @param connection
     *            RabbitMQ connection used for the communication
     * @return a StorageServiceClient instance
     * @throws IOException
     *             if a problem occurs during the creation of the queues or the
     *             consumer.
     */
    public static StorageServiceClient create(Connection connection) throws IOException {
        RabbitRpcClient rpcClient = RabbitRpcClient.create(connection, Constants.STORAGE_QUEUE_NAME);
        return new StorageServiceClient(rpcClient);
    }

    /**
     * RPC client that is used for the communication.
     */
    private RabbitRpcClient rpcClient;

    /**
     * Constructor creating a StorageServiceClient using the given
     * {@link RabbitRpcClient}.
     * 
     * @param rpcClient
     *            RPC client that is used for the communication
     */
    public StorageServiceClient(RabbitRpcClient rpcClient) {
        this.rpcClient = rpcClient;
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
        byte[] response = rpcClient.request(RabbitMQUtils.writeString(query));
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
        byte[] response = rpcClient.request(RabbitMQUtils.writeString(query));
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
        byte[] response = rpcClient.request(RabbitMQUtils.writeString(query));
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
        byte[] response = rpcClient.request(RabbitMQUtils.writeString(query));
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

    /**
     * Sends the given UPDATE query to the storage service and returns
     * <code>true</code> if the query is successful or <code>false</code> if an
     * error occurs or the service needs too much time to respond.
     * 
     * @param query
     *            UPDATE query
     * @return flag indicating whether the query has been executed successfully
     *         or not
     */
    public boolean sendUpdateQuery(String query) {
        byte[] response = rpcClient.request(RabbitMQUtils.writeString(query));
        return (response != null) && (response.length > 0);
    }

    /**
     * Inserts the given model into the storage and returns <code>true</code> if
     * the query is successful or <code>false</code> if an error occurs or the
     * service needs too much time to respond.
     * 
     * @param model
     *            RDF model containing triples that should be inserted
     * @param graphURI
     *            URI of the graph in which the model should be inserted
     * @return flag indicating whether the query has been executed successfully
     *         or not
     */
    public boolean sendInsertQuery(Model model, String graphURI) {
        StringWriter writer = new StringWriter();
        model.write(writer, "TTL");
        String modelString = writer.toString();
        StringBuilder builder = new StringBuilder();
        int pos = 0;
        // find the free line between the prefixes and the triples
        if (modelString.contains("\n\n")) {
            pos = modelString.indexOf("\n\n");
            pos += 2;
            builder.append(modelString.substring(0, pos).replace("@prefix", "prefix"));
        }
        builder.append("INSERT DATA\n{\nGRAPH <");
        builder.append(graphURI);
        builder.append("> {\n");
        builder.append(modelString.substring(pos));
        builder.append("\n}}");

        String query = builder.toString();
        LOGGER.info("Generated query: ", query.replace('\n', ' '));
        return sendUpdateQuery(query);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Closes the internal {@link #rpcClient} instance.
     * </p>
     */
    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(rpcClient);
    }

}
