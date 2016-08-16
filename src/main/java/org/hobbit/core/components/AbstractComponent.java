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
			connection = factory.newConnection();
		} else {
			LOGGER.warn("Couldn't get {} from the environment. This component won't be able to connect to RabbitMQ.",
					Constants.RABBIT_MQ_HOST_NAME_KEY);
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
