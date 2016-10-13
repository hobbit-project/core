package org.hobbit.core.rabbit;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * This class implements a thread safe client that can process several RPC calls
 * in parallel.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class RabbitRpcClient implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitRpcClient.class);

    /**
     * Creates a StorageServiceClient using the given RabbitMQ
     * {@link Connection}.
     * 
     * @param connection
     *            RabbitMQ connection used for the communication
     * @param requestQueueName
     *            name of the queue to which the requests should be sent
     * @return a StorageServiceClient instance
     * @throws IOException
     *             if a problem occurs during the creation of the
     *             {@link #channel}, the response queue or the {@link #consumer}
     *             .
     */
    public static RabbitRpcClient create(Connection connection, String requestQueueName) throws IOException {
        RabbitRpcClient client = new RabbitRpcClient(requestQueueName);
        try {
            client.init(connection);
            return client;
        } catch (Exception e) {
            client.close();
            throw e;
        }
    }

    /**
     * Channel used for the RabbitMQ-based communication.
     */
    private Channel channel = null;
    /**
     * Name of the queue containing the requests.
     */
    private String requestQueueName = null;
    /**
     * Name of the queue containing the responses from the storage service.
     */
    private String replyQueueName = null;
    /**
     * Mutex for managing access to the {@link #currentRequests} object.
     */
    private Semaphore requestMapMutex = new Semaphore(1);
    /**
     * Mapping of correlation Ids to their {@link RabbitRpcRequest} instances.
     */
    private Map<String, RabbitRpcRequest> currentRequests = new HashMap<String, RabbitRpcRequest>();

    /**
     * Constructor.
     * 
     * @param channel
     * @param replyQueueName
     * @param consumer
     */
    protected RabbitRpcClient(String requestQueueName) {
        this.requestQueueName = requestQueueName;
    }

    /**
     * Initializes the client by
     * 
     * @param connection
     * @throws IOException
     */
    protected void init(Connection connection) throws IOException {
        channel = connection.createChannel();
        replyQueueName = channel.queueDeclare().getQueue();
        channel.basicQos(1);
        RabbitRpcClientConsumer consumer = new RabbitRpcClientConsumer(channel, this);
        channel.basicConsume(replyQueueName, true, consumer);
    }

    /**
     * Sends the request, i.e., the given data, and blocks until the response is
     * received.
     * 
     * @param data
     *            the data of the request
     * @return the response or null if an error occurs.
     */
    public byte[] request(byte[] data) {
        byte[] response = null;
        try {
            String corrId = java.util.UUID.randomUUID().toString();

            BasicProperties props = new BasicProperties.Builder().correlationId(corrId).deliveryMode(2)
                    .replyTo(replyQueueName).build();
            RabbitRpcRequest request = new RabbitRpcRequest();
            requestMapMutex.acquire();
            currentRequests.put(corrId, request);
            requestMapMutex.release();

            channel.basicPublish("", requestQueueName, props, data);

            response = request.get();
        } catch (Exception e) {
            LOGGER.error("Exception while sending query. Returning null.", e);
        }
        return response;
    }

    /**
     * Processes the response with the given correlation Id and byte array by
     * searching for a matching request and setting the response if it could be
     * found. If there is no request with the same correlation Id, nothing is
     * done.
     * 
     * @param corrId
     *            correlation Id of the response
     * @param body
     *            data of the response
     */
    protected void processResponseForRequest(String corrId, byte[] body) {
        if (currentRequests.containsKey(corrId)) {
            try {
                requestMapMutex.acquire();
                currentRequests.get(corrId).setResponse(body);
                currentRequests.remove(corrId);
                requestMapMutex.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (channel != null) {
            try {
                channel.close();
            } catch (TimeoutException e) {
            }
        }
    }

    protected static class RabbitRpcClientConsumer extends DefaultConsumer {

        private RabbitRpcClient client;

        public RabbitRpcClientConsumer(Channel channel, RabbitRpcClient client) {
            super(channel);
            this.client = client;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                throws IOException {
            try {
                String corrId = properties.getCorrelationId();
                if (corrId != null) {
                    client.processResponseForRequest(corrId, body);
                }
            } catch (Exception e) {
                LOGGER.error("Exception while processing response.", e);
            }
        }
    }

    protected static class RabbitRpcRequest extends AbstractFuture<byte[]> implements Future<byte[]> {

        public void setResponse(byte[] response) {
            set(response);
        }
    }
}
