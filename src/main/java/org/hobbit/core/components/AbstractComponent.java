package org.hobbit.core.components;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.hobbit.core.Constants;
import org.hobbit.core.data.RabbitQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This abstract class implements basic functionalities of a hobbit component.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractComponent implements Component {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractComponent.class);

    /**
     * Maximum number of retries that are executed to connect to RabbitMQ.
     */
    public static final int NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ = 5;
    /**
     * Time, the system waits before retrying to connect to RabbitMQ. Note that
     * this time will be multiplied with the number of already failed tries.
     */
    public static final long START_WAITING_TIME_BEFORE_RETRY = 5000;

    private String hobbitSessionId;

    protected Connection connection = null;

    protected String rabbitMQHostName;

    @Override
    public void init() throws Exception {
        hobbitSessionId = null;
        if (System.getenv().containsKey(Constants.HOBBIT_SESSION_ID_KEY)) {
            hobbitSessionId = System.getenv().get(Constants.HOBBIT_SESSION_ID_KEY);
        }
        if (hobbitSessionId == null) {
            hobbitSessionId = Constants.HOBBIT_SESSION_ID_FOR_PLATFORM_COMPONENTS;
        }

        if (System.getenv().containsKey(Constants.RABBIT_MQ_HOST_NAME_KEY)) {
            ConnectionFactory factory = new ConnectionFactory();
            rabbitMQHostName = System.getenv().get(Constants.RABBIT_MQ_HOST_NAME_KEY);
            factory.setHost(rabbitMQHostName);
            for (int i = 0; (connection == null) && (i <= NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ); ++i) {
                try {
                    connection = factory.newConnection();
                } catch (Exception e) {
                    if (i < NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ) {
                        long waitingTime = START_WAITING_TIME_BEFORE_RETRY * (i + 1);
                        LOGGER.warn(
                                "Couldn't connect to RabbitMQ with try #" + i + ". Next try in " + waitingTime + "ms.",
                                e);
                        try {
                            Thread.sleep(waitingTime);
                        } catch (Exception e2) {
                            LOGGER.warn("Interrupted while waiting before retrying to connect to RabbitMQ.", e2);
                        }
                    }
                }
            }
            if (connection == null) {
                String msg = "Couldn't connect to RabbitMQ after " + NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ
                        + " retries.";
                LOGGER.error(msg);
                throw new Exception(msg);
            }
        } else {
            String msg = "Couldn't get " + Constants.RABBIT_MQ_HOST_NAME_KEY
                    + " from the environment. This component won't be able to connect to RabbitMQ.";
            LOGGER.error(msg);
            throw new Exception(msg);
        }
    }

    @Override
    public void close() throws IOException {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
            }
        }
    }

    public String getHobbitSessionId() {
        return hobbitSessionId;
    }

    public String generateSessionQueueName(String queueName) {
        return queueName + "." + hobbitSessionId;
    }

    /**
     * This method opens a channel using the established {@link #connection} to
     * RabbitMQ and creates a new queue using the given name and the following configuration:
     * <ul>
     * <li>The channel number is automatically derived from the connection.</li>
     * <li>The queue is not durable.</li>
     * <li>The queue is not exclusive.</li>
     * <li>The queue is configured to be deleted automatically.</li>
     * <li>No additional queue configuration is defined.</li>
     * </ul>
     * 
     * @param name
     *            name of the queue
     * @return {@link RabbitQueue} object comprising the {@link Channel} and the
     *         name of the created queue
     * @throws IOException
     */
    protected RabbitQueue createDefaultRabbitQueue(String name) throws IOException {
        Channel channel = connection.createChannel();
        channel.queueDeclare(name, false, false, true, null);
        return new RabbitQueue(channel, name);
    }
}
