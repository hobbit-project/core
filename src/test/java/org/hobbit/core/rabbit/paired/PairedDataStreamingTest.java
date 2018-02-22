package org.hobbit.core.rabbit.paired;

import java.io.ByteArrayInputStream;
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
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.rabbit.RabbitQueueFactory;
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
public class PairedDataStreamingTest implements RabbitQueueFactory, PairedStreamHandler {

    @Parameters
    public static Collection<Object[]> data() {
        byte loremIpsum[] = "Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquid ex ea commodi consequat. Quis aute iure reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint obcaecat cupiditat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
                .getBytes(Charsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4000);
        IntBuffer intBuffer = buffer.asIntBuffer();
        int number = 0;
        for (int j = 0; j < 1000; ++j) {
            intBuffer.put(number);
            ++number;
        }
        byte numbers[] = buffer.array();

        List<Object[]> testConfigs = new ArrayList<Object[]>();
        testConfigs.add(new Object[] { new byte[][][] { { new byte[0] } } });
        testConfigs.add(new Object[] { new byte[][][] { { new byte[0], new byte[0] } } });
        testConfigs.add(new Object[] { new byte[][][] { { { 0x0, 0x1, 0x2 }, { 0x2, 0x1, 0x0 } } } });
        testConfigs.add(new Object[] { new byte[][][] { { loremIpsum, loremIpsum } } });
        testConfigs.add(new Object[] { new byte[][][] { { loremIpsum, numbers } } });
        testConfigs.add(
                new Object[] { new byte[][][] { { { 0x0, 0x1, 0x2 }, { 0x2, 0x1, 0x0 } }, { loremIpsum, numbers } } });
        testConfigs.add(new Object[] { new byte[][][] { { loremIpsum, numbers }, { numbers, loremIpsum },
                { numbers, numbers }, { loremIpsum, loremIpsum } } });

        return testConfigs;
    }

    private Connection connection = null;
    private byte data[][][];
    private byte receivedData[][][];
    private List<Exception> exceptions = new ArrayList<>();

    public PairedDataStreamingTest(byte data[][][]) {
        this.data = data;
        this.receivedData = new byte[data.length][][];
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
            DataReceiverImpl receiver = DataReceiverImpl.builder().consumerBuilder(PairedConsumerImpl.builder())
                    .dataHandler(this).maxParallelProcessedMsgs(1).queue(this, queueName).build();

            System.out.println("Starting sender...");
            PairedDataStreamingTest testInstance = this;
            Thread senderThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    PairedDataSender sender = null;
                    try {
                        sender = PairedDataSender.builder().queue(testInstance, queueName).build();
                        for (int i = 0; i < data.length; ++i) {
                            InputStream[] streams = new InputStream[data[i].length];
                            for (int j = 0; j < streams.length; ++j) {
                                streams[j] = new ByteArrayInputStream(data[i][j]);
                            }
                            sender.sendData(streams, Integer.toString(i));
                            for (int j = 0; j < streams.length; ++j) {
                                IOUtils.closeQuietly(streams[j]);
                            }
                        }
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
    public void handleIncomingStream(String streamId, InputStream stream) {
        try {
            throw new IllegalStateException(
                    "handleIncomingStream has been called which should not happen in this test case");
        } catch (Exception e) {
            e.printStackTrace();
            exceptions.add(e);
        }
    }

    @Override
    public void handleIncomingStreams(String streamId, InputStream[] streams) {
        try {
            int id = Integer.parseInt(streamId);
            receivedData[id] = new byte[streams.length][];
            for (int i = 0; i < streams.length; ++i) {
                receivedData[id][i] = IOUtils.toByteArray(streams[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            exceptions.add(e);
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

}
