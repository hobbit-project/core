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

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.com.CommonChannel;
import org.hobbit.core.components.communicationfactory.ChannelFactory;
import org.hobbit.core.rabbit.RabbitQueueFactory;
import org.hobbit.core.rabbit.RabbitQueueFactoryImpl;
import org.hobbit.utils.EnvVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

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
     * Time, the system waits before retrying to connect to RabbitMQ. Note that this
     * time will be multiplied with the number of already failed tries.
     */
    public static final long START_WAITING_TIME_BEFORE_RETRY = 5000;

    public static final String TRUE = "true";

    private String hobbitSessionId;
    /**
     * Factory for creating outgoing data queues.
     */
    protected CommonChannel outgoingDataQueuefactory = null;
    /**
     * Factory for creating outgoing data queues.
     */
    protected CommonChannel incomingDataQueueFactory = null;
    /**
     * The host name of the RabbitMQ broker.
     */
    protected String rabbitMQHostName;
    /**
     * The factory that can be used to create additional connections. However, in
     * most cases it is sufficient to create a new channel using the already
     * existing {@link #incomingDataQueueFactory} and
     * {@link #outgoingDataQueuefactory} objects.
     */
    protected ConnectionFactory connectionFactory;

    /**
     * Abstract reference for channel abstraction
     * @return
     */
    protected CommonChannel commonChannel = null;

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public String getRabbitMQHostName() {
        return rabbitMQHostName;
    }

    public void setRabbitMQHostName(String rabbitMQHostName) {
        this.rabbitMQHostName = rabbitMQHostName;
    }

    @Override
    public void init() throws Exception {
        hobbitSessionId = EnvVariables.getString(Constants.HOBBIT_SESSION_ID_KEY,
                Constants.HOBBIT_SESSION_ID_FOR_PLATFORM_COMPONENTS);
        setConnectionFactory();
        commonChannel = new ChannelFactory().getChannel(isRabbitMQEnabled(),
            Constants.HOBBIT_COMMAND_EXCHANGE_NAME, connectionFactory);
        incomingDataQueueFactory = new ChannelFactory().getChannel(isRabbitMQEnabled(),
            "", connectionFactory);
        outgoingDataQueuefactory = new ChannelFactory().getChannel(isRabbitMQEnabled(),
            "", connectionFactory);
        incomingDataQueueFactory.createChannel();
        outgoingDataQueuefactory.createChannel();

    }

    private void setConnectionFactory() {
        connectionFactory = new ConnectionFactory();
        if (rabbitMQHostName == null) {
            rabbitMQHostName = EnvVariables.getString(Constants.RABBIT_MQ_HOST_NAME_KEY, LOGGER);
        }
        connectionFactory = new ConnectionFactory();
        if(rabbitMQHostName.contains(":")){
            String[] splitted = rabbitMQHostName.split(":");
            connectionFactory.setHost(splitted[0]);
            connectionFactory.setPort(Integer.parseInt(splitted[1]));
        }else
        connectionFactory.setHost(rabbitMQHostName);
        connectionFactory.setAutomaticRecoveryEnabled(true);
        // attempt recovery every 10 seconds
        connectionFactory.setNetworkRecoveryInterval(10000);

    }

    public Connection createConnection() throws Exception {
        Connection connection = null;
        Exception exception = null;
        for (int i = 0; (connection == null) && (i <= NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ); ++i) {
            try {
                connection = connectionFactory.newConnection();
            } catch (Exception e) {
                if (i < NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ) {
                    long waitingTime = START_WAITING_TIME_BEFORE_RETRY * (i + 1);
                    LOGGER.warn("Couldn't connect to RabbitMQ with try #" + i + ". Next try in " + waitingTime + "ms.");
                    exception = e;
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
            LOGGER.error(msg, exception);
            throw new Exception(msg, exception);
        }
        return connection;
    }

    @Override
    public void close() throws IOException {
    	incomingDataQueueFactory.close();
    	outgoingDataQueuefactory.close();
    	commonChannel.close();
    }

    public String getHobbitSessionId() {
        return hobbitSessionId;
    }

    public String generateSessionQueueName(String queueName) {
        return queueName + "." + hobbitSessionId;
    }
    /**
     * Method gets the property value for {@link org.hobbit.core.Constants#IS_RABBIT_MQ_ENABLED}
     * from environment variables. Sets the default value to true if the property not found.
     */

    protected boolean isRabbitMQEnabled() {
        boolean isRabbitMQEnabled = false;
        try {
            if(EnvVariables.getString(Constants.IS_RABBIT_MQ_ENABLED, LOGGER).equals(TRUE)) {
                isRabbitMQEnabled = true;
            }
        } catch(Exception e) {
            LOGGER.error("Unable to fetch the property for RabbitMQ Enabled, setting the default to true");
            isRabbitMQEnabled = true;
        }
        return isRabbitMQEnabled;
    }

}
