package org.hobbit.core.components;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.Charsets;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.data.StartCommandData;
import org.hobbit.core.data.StopCommandData;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;

public abstract class AbstractCommandReceivingComponent extends AbstractComponent implements CommandReceivingComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommandReceivingComponent.class);

    private String containerName;
    private String responseQueueName = null;
    private QueueingConsumer responseConsumer = null;
    protected Channel cmdChannel = null;
    protected String containerType = "";
    private Set<String> acceptedCmdHeaderIds = new HashSet<String>(5);
    /**
     * Threadsafe JSON parser.
     */
    private Gson gson = new Gson();

    @Override
    public void init() throws Exception {
        super.init();
        addCommandHeaderId(getHobbitSessionId());
        cmdChannel = connection.createChannel();
        String queueName = cmdChannel.queueDeclare().getQueue();
        cmdChannel.exchangeDeclare(Constants.HOBBIT_COMMAND_EXCHANGE_NAME, "fanout");
        cmdChannel.queueBind(queueName, Constants.HOBBIT_COMMAND_EXCHANGE_NAME, "");

        Consumer consumer = new DefaultConsumer(cmdChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                handleCmd(body, properties.getReplyTo());
            }
        };
        cmdChannel.basicConsume(queueName, true, consumer);

        if (System.getenv().containsKey(Constants.HOBBIT_SESSION_ID_KEY)) {
            containerName = System.getenv().get(Constants.HOBBIT_SESSION_ID_KEY);
        }
        if (containerName == null) {
            LOGGER.info("Couldn't get the id of this Docker container. Won't be able to create containers.");
        }
    }

    /**
     * Sends the given command to the command queue.
     * 
     * @param command
     *            the command that should be sent
     * @throws IOException
     */
    protected void sendToCmdQueue(byte command) throws IOException {
        sendToCmdQueue(command, null);
    }

    /**
     * Sends the given command to the command queue with the given data
     * appended.
     * 
     * @param command
     *            the command that should be sent
     * @param data
     *            data that should be appended to the command
     * @throws IOException
     */
    protected void sendToCmdQueue(byte command, byte data[]) throws IOException {
        sendToCmdQueue(command, data, null);
    }

    /**
     * Sends the given command to the command queue with the given data appended
     * and using the given properties.
     * 
     * @param command
     *            the command that should be sent
     * @param data
     *            data that should be appended to the command
     * @param props
     *            properties that should be used for the message
     * @throws IOException
     */
    protected void sendToCmdQueue(byte command, byte data[], BasicProperties props) throws IOException {
        byte sessionIdBytes[] = getHobbitSessionId().getBytes(Charsets.UTF_8);
        // + 5 because 4 bytes for the session ID length and 1 byte for the
        // command
        int dataLength = sessionIdBytes.length + 5;
        boolean attachData = (data != null) && (data.length > 0);
        if (attachData) {
            dataLength += data.length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(dataLength);
        buffer.putInt(sessionIdBytes.length);
        buffer.put(sessionIdBytes);
        buffer.put(command);
        if (attachData) {
            buffer.put(data);
        }
        cmdChannel.basicPublish(Constants.HOBBIT_COMMAND_EXCHANGE_NAME, "", props, buffer.array());
    }

    /**
     * Adds the given session id to the set of ids this component is reacting
     * to.
     * 
     * @param sessionId
     *            session id that should be added to the set of accepted ids.
     */
    protected void addCommandHeaderId(String sessionId) {
        acceptedCmdHeaderIds.add(sessionId);
    }

    protected void handleCmd(byte bytes[], String replyTo) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        String sessionId = RabbitMQUtils.readString(buffer);
        if (acceptedCmdHeaderIds.contains(sessionId)) {
            byte command = buffer.get();
            byte remainingData[];
            if (buffer.remaining() > 0) {
                remainingData = new byte[buffer.remaining()];
                buffer.get(remainingData);
            } else {
                remainingData = new byte[0];
            }
            receiveCommand(command, remainingData);
        }
    }

    /**
     * This method sends a {@link Commands#DOCKER_CONTAINER_START} command to
     * create and start an instance of the given image using the given
     * environment variables.
     * 
     * @param imageName
     *            the name of the image of the docker container
     * @param envVariables
     *            environment variables that should be added to the created
     *            container
     * @return the name of the container instance or null if an error occurred
     */
    protected String createContainer(String imageName, String[] envVariables) {
        try {
            envVariables = envVariables != null ? Arrays.copyOf(envVariables, envVariables.length + 2) : new String[2];
            envVariables[envVariables.length - 2] = Constants.RABBIT_MQ_HOST_NAME_KEY + "=" + rabbitMQHostName;
            envVariables[envVariables.length - 1] = Constants.HOBBIT_SESSION_ID_KEY + "=" + getHobbitSessionId();
            initResponseQueue();
            byte data[] = RabbitMQUtils.writeString(gson.toJson(new StartCommandData(imageName, containerType,
                    containerName, envVariables)));
            BasicProperties props = new BasicProperties.Builder().deliveryMode(2).replyTo(responseQueueName).build();
            sendToCmdQueue(Commands.DOCKER_CONTAINER_START, data, props);
            QueueingConsumer.Delivery delivery = responseConsumer.nextDelivery();
            if (delivery.getBody().length > 0) {
                return RabbitMQUtils.readString(delivery.getBody());
            }
        } catch (Exception e) {
            LOGGER.error("Got exception while trying to request the creation of an instance of the \"" + imageName
                    + "\" image.", e);
        }
        return null;
    }

    /**
     * This method sends a {@link Commands#DOCKER_CONTAINER_STOP} command to
     * stop the container with the given id.
     * 
     * @param containerName
     *            the name of the container instance that should be stopped
     */
    protected void stopContainer(String containerName) {
        byte data[] = RabbitMQUtils.writeString(gson.toJson(new StopCommandData(containerName)));
        try {
            sendToCmdQueue(Commands.DOCKER_CONTAINER_STOP, data);
        } catch (IOException e) {
            LOGGER.error("Got exception while trying to stop the container with the id\"" + containerName + "\".", e);
        }
    }

    /**
     * Internal method for initializing the {@link #responseQueueName} and the
     * {@link #responseConsumer} if they haven't been initialized before.
     * 
     * @throws IOException
     */
    private void initResponseQueue() throws IOException {
        if (responseQueueName == null) {
            responseQueueName = cmdChannel.queueDeclare().getQueue();
        }
        if (responseConsumer == null) {
            responseConsumer = new QueueingConsumer(cmdChannel);
            cmdChannel.basicConsume(responseQueueName, responseConsumer);
        }
    }

    @Override
    public void close() throws IOException {
        if (cmdChannel != null) {
            try {
                cmdChannel.close();
            } catch (Exception e) {
            }
        }
        super.close();
    }

}
