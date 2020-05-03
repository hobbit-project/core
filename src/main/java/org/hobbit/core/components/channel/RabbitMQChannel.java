package org.hobbit.core.components.channel;

import com.rabbitmq.client.ConnectionFactory;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractComponent;
import org.hobbit.core.rabbit.RabbitQueueFactoryImpl;
import org.hobbit.utils.EnvVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMQChannel extends AbstractChannel {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQChannel.class);

    public void createConnectionFactory(Object comp) {
        AbstractComponent cmp = (AbstractComponent)comp;
        cmp.setRabbitMQHostName(EnvVariables.getString(Constants.RABBIT_MQ_HOST_NAME_KEY, LOGGER));
        ConnectionFactory connectionFactory = new ConnectionFactory();
        if(cmp.getRabbitMQHostName().contains(":")){
            String[] splitted = cmp.getRabbitMQHostName().split(":");
            connectionFactory.setHost(splitted[0]);
            connectionFactory.setPort(Integer.parseInt(splitted[1]));
        }else
            connectionFactory.setHost(cmp.getRabbitMQHostName());
        connectionFactory.setAutomaticRecoveryEnabled(true);
        cmp.setConnectionFactory(connectionFactory);

    }

    public void setIncomingDataQueueFactory(AbstractComponent abstractComponent) throws Exception {
        abstractComponent.setIncomingDataQueueFactory(new RabbitQueueFactoryImpl(abstractComponent.createConnection()));
    }

    public void setOutgoingDataQueueFactory(AbstractComponent abstractComponent) throws Exception {
        abstractComponent.setOutgoingDataQueuefactory(new RabbitQueueFactoryImpl(abstractComponent.createConnection()));
    }
}
