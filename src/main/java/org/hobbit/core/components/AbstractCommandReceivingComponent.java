package org.hobbit.core.components;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.io.Charsets;
import org.hobbit.core.Constants;
import org.hobbit.core.data.StartCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public abstract class AbstractCommandReceivingComponent extends AbstractComponent implements CommandReceivingComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommandReceivingComponent.class);

    protected Channel cmdChannel = null;
    private String containerId;
    /**
     * Threadsafe JSON parser.
     */
    private Gson gson = new Gson();

    @Override
    public void init() throws Exception {
        super.init();
        cmdChannel = connection.createChannel();
        String queueName = cmdChannel.queueDeclare().getQueue();
        cmdChannel.exchangeDeclare(Constants.HOBBIT_COMMAND_EXCHANGE_NAME, "fanout");
        cmdChannel.queueBind(queueName, Constants.HOBBIT_COMMAND_EXCHANGE_NAME, "");

        Consumer consumer = new DefaultConsumer(cmdChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                handleCmd(body);
            }
        };
        cmdChannel.basicConsume(queueName, true, consumer);

        if (System.getenv().containsKey(Constants.HOBBIT_SESSION_ID_KEY)) {
            containerId = System.getenv().get(Constants.HOBBIT_SESSION_ID_KEY);
        }
        if (containerId == null) {
            LOGGER.info("Couldn't get the id of this Docker container. Won't be able to create containers.");
        }
    }

    protected void sendToCmdQueue(byte command) throws IOException {
        sendToCmdQueue(command, null);
    }

    protected void sendToCmdQueue(byte command, byte data[]) throws IOException {
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
        cmdChannel.basicPublish(Constants.HOBBIT_COMMAND_EXCHANGE_NAME, "", null, buffer.array());
    }

    protected void handleCmd(byte bytes[]) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int idLength = buffer.getInt();
        byte sessionIdBytes[] = new byte[idLength];
        buffer.get(sessionIdBytes);
        String sessionId = new String(sessionIdBytes, Charsets.UTF_8);
        if (sessionId.equals(getHobbitSessionId())) {
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

    protected String createContainer(String dataGeneratorImageName, String[] envVariables) {
        gson.toJson(new StartCommandData(dataGeneratorImageName, "", containerId, envVariables));
        // TODO
        return;
    }

    protected String stopContainer(String containerId, String[] envVariables) {
        // TODO Auto-generated method stub
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
