package org.hobbit.core.rabbit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.hobbit.core.TestConstants;
import org.hobbit.core.data.RabbitQueue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@RunWith(Parameterized.class)
public class DataStreamingTest implements RabbitQueueFactory, IncomingStreamHandler {

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testConfigs = new ArrayList<Object[]>();
        testConfigs.add(new Object[] { new byte[0] });
        testConfigs.add(new Object[] {
                "Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquid ex ea commodi consequat. Quis aute iure reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint obcaecat cupiditat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
                        .getBytes(Charsets.UTF_8) });

        ByteBuffer buffer = ByteBuffer.allocate(4000);
        IntBuffer intBuffer = buffer.asIntBuffer();
        int number = 0;
        for (int j = 0; j < 1000; ++j) {
            intBuffer.put(number);
            ++number;
        }
        testConfigs.add(new Object[] { buffer.array() });

        return testConfigs;
    }

    private Connection connection = null;
    private byte data[];
    private byte receivedData[];
    private List<Exception> exceptions = new ArrayList<>();

    public DataStreamingTest(byte data[]) {
        this.data = data;
    }

    @Before
    public void before() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(TestConstants.RABBIT_HOST);
        connection = factory.newConnection();
    }

    @After
    public void close() {
        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testStream() {
        try {
            String queueName = UUID.randomUUID().toString().replace("-", "");

            System.out.println("Starting receiver...");
            DataReceiverImpl receiver = (new DataReceiverImpl.Builder()).dataHandler(this).maxParallelProcessedMsgs(1)
                    .queue(this, queueName).build();

            System.out.println("Starting sender...");
            DataStreamingTest testInstance = this;
            Thread senderThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    DataSender sender = null;
                    try {
                        sender = (new DataSenderImpl.Builder()).queue(testInstance, queueName).build();
                        sender.sendData(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                        exceptions.add(e);
                    } finally {
                        IOUtils.closeQuietly(sender);
                    }
                    // We have to tell the receiver that we have finished the
                    // sending
                    // receiver.terminate();
                }
            }, "sender");
            senderThread.start();

            System.out.println("Waiting for sender...");
            senderThread.join();

            receiver.closeWhenFinished();

            // make sure that there are no exceptions
            Assert.assertEquals("Exceptions occured.", 0, exceptions.size());

            Assert.assertArrayEquals(data, receivedData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public RabbitQueue createDefaultRabbitQueue(String name) throws IOException {
        return createDefaultRabbitQueue(name, createChannel());
    }

    @Override
    public RabbitQueue createDefaultRabbitQueue(String name, Channel channel) throws IOException {
        channel.queueDeclare(name, false, false, true, null);
        return new RabbitQueue(channel, name);
    }

    @Override
    public Channel createChannel() throws IOException {
        return connection.createChannel();
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void handleIncomingStream(String streamId, InputStream stream) {
        try {
            receivedData = IOUtils.toByteArray(stream);
        } catch (IOException e) {
            e.printStackTrace();
            exceptions.add(e);
        }
    }
}
