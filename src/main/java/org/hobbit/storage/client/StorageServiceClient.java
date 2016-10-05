package org.hobbit.storage.client;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;

import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Constants;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;

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
     * Maximum time the client is waiting for a response from the triple store.
     */
    private static final long MAXIMUM_WAITING_TIME = 60000;
    /**
     * Channel used for the RabbitMQ-based communication.
     */
    private Channel channel = null;
    /**
     * Name of the queue containing the responses from the storage service.
     */
    private String replyQueueName = null;
    /**
     * Consumer used to read responses.
     */
    private QueueingConsumer consumer = null;

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
        Channel channel = connection.createChannel();
        String replyQueueName = channel.queueDeclare().getQueue();
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(replyQueueName, true, consumer);
        return new StorageServiceClient(channel, replyQueueName, consumer);
    }

    /**
     * Constructor.
     * 
     * @param channel
     * @param replyQueueName
     * @param consumer
     */
    protected StorageServiceClient(Channel channel, String replyQueueName, QueueingConsumer consumer) {
        this.channel = channel;
        this.replyQueueName = replyQueueName;
        this.consumer = consumer;
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
        byte[] response = sendQuery(query);
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
        byte[] response = sendQuery(query);
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
        byte[] response = sendQuery(query);
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
        byte[] response = sendQuery(query);
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
     * Sends the given query to the service and returns the result as byte array
     * or <code>null</code> if an error occurs.
     * 
     * @param query
     *            the query that should be sent to the service
     * @return the result or <code>null</code>
     */
    protected synchronized byte[] sendQuery(String query) {
        byte[] response = null;
        try {
            String corrId = java.util.UUID.randomUUID().toString();

            BasicProperties props = new BasicProperties.Builder().correlationId(corrId).replyTo(replyQueueName).build();

            channel.basicPublish("", Constants.STORAGE_QUEUE_NAME, props, RabbitMQUtils.writeString(query));

            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery(MAXIMUM_WAITING_TIME);
                if (delivery == null) {
                    LOGGER.error(
                            "Couldn't get a response from the triple store during the maximum waiting timg ({}ms). Returning null.",
                            MAXIMUM_WAITING_TIME);
                    return null;
                }
                if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                    response = delivery.getBody();
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception while sending query. Returning null.", e);
        }
        return response;
    }

    @Override
    public void close() throws IOException {
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception ignore) {
            }
        }
    }
}
