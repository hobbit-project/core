package org.hobbit.core.components;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.hobbit.core.Constants;
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

    public static final int NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ = 5;
    public static final long START_WAITING_TIME_BEFORE_RETRY = 5000;

    private String hobbitSessionId;

    protected Connection connection = null;

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
            factory.setHost(System.getenv().get(Constants.RABBIT_MQ_HOST_NAME_KEY));
            for (int i = 0; (connection == null) && (i <= NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ); ++i) {
                try {
                    connection = factory.newConnection();
                } catch (Exception e) {
                    LOGGER.warn("Couldn't connect to RabbitMQ with try #" + i, e);
                    try {
                        Thread.sleep(START_WAITING_TIME_BEFORE_RETRY * i);
                    } catch (Exception e2) {
                        LOGGER.warn("Interrupted while waiting before retrying to connect to RabbitMQ.");
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
        return queueName + "-" + hobbitSessionId;
    }
}
