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
public abstract class AbstractComponent implements Component, RabbitQueueFactory {

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
    /**
     * The connection to the RabbitMQ broker. In most cases it will be used for
     * handling data.
     */
    protected Connection dataConnection = null;
    /**
     * The host name of the RabbitMQ broker.
     */
    protected String rabbitMQHostName;
    /**
     * The factory that can be used to create additional connections. However,
     * in most cases it is sufficient to create a new channel using the already
     * established {@link #dataConnection}.
     */
    protected ConnectionFactory connectionFactory;

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
            connectionFactory = new ConnectionFactory();
            rabbitMQHostName = System.getenv().get(Constants.RABBIT_MQ_HOST_NAME_KEY);
            connectionFactory.setHost(rabbitMQHostName);
            connectionFactory.setAutomaticRecoveryEnabled(true);
            // attempt recovery every 10 seconds
            connectionFactory.setNetworkRecoveryInterval(10000);
            for (int i = 0; (dataConnection == null) && (i <= NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ); ++i) {
                try {
                    dataConnection = connectionFactory.newConnection();
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
            if (dataConnection == null) {
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
        if (dataConnection != null) {
            try {
                dataConnection.close();
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
     * This method opens a channel using the established {@link #dataConnection}
     * to RabbitMQ and creates a new queue using the given name and the
     * following configuration:
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
     *             if a communication error occurs
     */
    @Override
    public RabbitQueue createDefaultRabbitQueue(String name) throws IOException {
        Channel channel = dataConnection.createChannel();
        channel.queueDeclare(name, false, false, true, null);
        return new RabbitQueue(channel, name);
    }
}
