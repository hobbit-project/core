package org.hobbit.core.rabbit;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.lf5.util.StreamUtils;
import org.hobbit.core.TestConstants;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.consume.MessageConsumerBuilder;
import org.hobbit.core.rabbit.consume.MessageConsumerImpl;
import org.hobbit.core.rabbit.consume.QueueingConsumerBasedImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer.Delivery;

@SuppressWarnings("deprecation")
@RunWith(Parameterized.class)
public class SenderReceiverTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SenderReceiverTest.class);

    private static final String QUEUE_NAME = "sender-receiver-test";

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testConfigs = new ArrayList<Object[]>();
        testConfigs.add(new Object[] { 1, 1, 20, 1, 0, QueueingConsumerBasedImpl.builder() });
        testConfigs.add(new Object[] { 1, 1, 10000, 1, 0, QueueingConsumerBasedImpl.builder() });
        testConfigs.add(new Object[] { 1, 1, 10000, 100, 0, QueueingConsumerBasedImpl.builder() });
        testConfigs.add(new Object[] { 2, 1, 10000, 1, 0, QueueingConsumerBasedImpl.builder() });
        testConfigs.add(new Object[] { 2, 1, 10000, 100, 0, QueueingConsumerBasedImpl.builder() });
        testConfigs.add(new Object[] { 1, 2, 10000, 1, 0, QueueingConsumerBasedImpl.builder() });
        testConfigs.add(new Object[] { 1, 2, 10000, 100, 0, QueueingConsumerBasedImpl.builder() });
        testConfigs.add(new Object[] { 1, 1, 1000, 100, 1000, QueueingConsumerBasedImpl.builder() });
        testConfigs.add(new Object[] { 2, 1, 1000, 100, 1000, QueueingConsumerBasedImpl.builder() });
        testConfigs.add(new Object[] { 1, 2, 1000, 100, 1000, QueueingConsumerBasedImpl.builder() });
        testConfigs.add(new Object[] { 1, 1, 50, 1, 500, QueueingConsumerBasedImpl.builder() });
        testConfigs.add(new Object[] { 2, 1, 50, 1, 500, QueueingConsumerBasedImpl.builder() });
        testConfigs.add(new Object[] { 1, 2, 50, 1, 500, QueueingConsumerBasedImpl.builder() });

        testConfigs.add(new Object[] { 1, 1, 20, 1, 0, MessageConsumerImpl.builder() });
        testConfigs.add(new Object[] { 1, 1, 10000, 1, 0, MessageConsumerImpl.builder() });
        testConfigs.add(new Object[] { 1, 1, 10000, 100, 0, MessageConsumerImpl.builder() });
        testConfigs.add(new Object[] { 2, 1, 10000, 1, 0, MessageConsumerImpl.builder() });
        testConfigs.add(new Object[] { 2, 1, 10000, 100, 0, MessageConsumerImpl.builder() });
        testConfigs.add(new Object[] { 1, 2, 10000, 1, 0, MessageConsumerImpl.builder() });
        testConfigs.add(new Object[] { 1, 2, 10000, 100, 0, MessageConsumerImpl.builder() });
        testConfigs.add(new Object[] { 1, 1, 1000, 100, 1000, MessageConsumerImpl.builder() });
        testConfigs.add(new Object[] { 2, 1, 1000, 100, 1000, MessageConsumerImpl.builder() });
        testConfigs.add(new Object[] { 1, 2, 1000, 100, 1000, MessageConsumerImpl.builder() });
        testConfigs.add(new Object[] { 1, 1, 50, 1, 500, MessageConsumerImpl.builder() });
        testConfigs.add(new Object[] { 2, 1, 50, 1, 500, MessageConsumerImpl.builder() });
        testConfigs.add(new Object[] { 1, 2, 50, 1, 500, MessageConsumerImpl.builder() });
        
        return testConfigs;
    }

    private int numberOfSenders;
    private int numberOfReceivers;
    private int numberOfMessages;
    private int numberOfMessagesProcessedInParallel;
    private long messageProcessingDelay;
    private MessageConsumerBuilder builder;

    public SenderReceiverTest(int numberOfSenders, int numberOfReceivers, int numberOfMessages,
            int numberOfMessagesProcessedInParallel, long messageProcessingDelay, MessageConsumerBuilder builder) {
        this.numberOfSenders = numberOfSenders;
        this.numberOfReceivers = numberOfReceivers;
        this.numberOfMessages = numberOfMessages;
        this.numberOfMessagesProcessedInParallel = numberOfMessagesProcessedInParallel;
        this.messageProcessingDelay = messageProcessingDelay;
        this.messageProcessingDelay = messageProcessingDelay;
    }

    @Test(timeout=60000)
    public void test() throws Exception {
        int overallMessages = numberOfSenders * numberOfMessages;
        RabbitQueueFactoryImpl sendQueueFactory = null;
        RabbitQueueFactoryImpl receiveQueueFactory = null;
        String queueName = QUEUE_NAME + new Random().nextLong();
        LOGGER.info("Queue \"{}\" will be used for this test.", queueName);

        try {
            ConnectionFactory cFactory = new ConnectionFactory();
            cFactory.setHost(TestConstants.RABBIT_HOST);
            cFactory.setAutomaticRecoveryEnabled(true);
            sendQueueFactory = new RabbitQueueFactoryImpl(cFactory.newConnection());

            Receiver receivers[] = new Receiver[numberOfReceivers];
            Thread receiverThreads[] = new Thread[numberOfReceivers];
            for (int i = 0; i < receiverThreads.length; ++i) {
                receivers[i] = new Receiver(cFactory, overallMessages, numberOfMessagesProcessedInParallel,
                        messageProcessingDelay, queueName, builder);
                receiverThreads[i] = new Thread(receivers[i]);
                receiverThreads[i].start();
            }

            Sender senders[] = new Sender[numberOfSenders];
            Thread senderThreads[] = new Thread[numberOfSenders];
            for (int i = 0; i < senderThreads.length; ++i) {
                senders[i] = new Sender(i, numberOfMessages, sendQueueFactory,queueName);
                senderThreads[i] = new Thread(senders[i]);
                senderThreads[i].start();
            }
            for (int i = 0; i < senderThreads.length; ++i) {
                senderThreads[i].join();
            }
            for (int i = 0; i < receivers.length; ++i) {
                receivers[i].terminate();
            }
            for (int i = 0; i < receiverThreads.length; ++i) {
                receiverThreads[i].join();
            }
            // Make sure no errors occurred
            for (int i = 0; i < senders.length; ++i) {
                Assert.assertNull(senders[i].getError());
            }
            for (int i = 0; i < receivers.length; ++i) {
                Assert.assertNull(receivers[i].getError());
            }
            // check received messages
            BitSet receivedMsgIds = new BitSet(overallMessages);
            for (int i = 0; i < receivers.length; ++i) {
                Assert.assertFalse(receivedMsgIds.intersects(receivers[i].getReceivedMsgIds()));
                receivedMsgIds.or(receivers[i].getReceivedMsgIds());
            }
            Assert.assertEquals(overallMessages, receivedMsgIds.cardinality());
        } finally {
            IOUtils.closeQuietly(sendQueueFactory);
            IOUtils.closeQuietly(receiveQueueFactory);
        }
    }

    protected static class Sender implements Runnable {

        private static final Logger LOGGER = LoggerFactory.getLogger(Sender.class);

        private int senderId;
        private int numberOfMessages;
        private String queueName;
        private RabbitQueueFactory factory;
        private Throwable error;

        public Sender(int senderId, int numberOfMessages, RabbitQueueFactory factory, String queueName) {
            this.senderId = senderId;
            this.numberOfMessages = numberOfMessages;
            this.factory = factory;
            this.queueName = queueName;
        }

        @Override
        public void run() {
            DataSender sender = null;
            try {
                sender = DataSenderImpl.builder().queue(factory, queueName).build();
                int firstMsgId = senderId * numberOfMessages;
                for (int i = 0; i < numberOfMessages; ++i) {
                    sender.sendData(RabbitMQUtils.writeString(Integer.toString(firstMsgId + i)));
                }
                sender.closeWhenFinished();
            } catch (Exception e) {
                LOGGER.error("Sender crashed with Exception.", e);
                error = e;
            } finally {
                IOUtils.closeQuietly(sender);
            }
        }

        public Throwable getError() {
            return error;
        }

    }

    protected static class Receiver implements Runnable, IncomingStreamHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(Receiver.class);

        private ConnectionFactory cFactory;
        private BitSet receivedMsgIds;
        private int maxParallelProcessedMsgs;
        private long messageProcessingDelay;
        private Semaphore terminationMutex = new Semaphore(0);
        private Throwable error;
        private String queueName;
        private MessageConsumerBuilder builder;

        public Receiver(ConnectionFactory cFactory, int overallMessages, int maxParallelProcessedMsgs,
                long messageProcessingDelay, String queueName, MessageConsumerBuilder builder) {
            super();
            receivedMsgIds = new BitSet(overallMessages);
            this.cFactory = cFactory;
            this.maxParallelProcessedMsgs = maxParallelProcessedMsgs;
            this.messageProcessingDelay = messageProcessingDelay;
            this.queueName = queueName;
            this.builder = builder;
        }

        @Override
        public void run() {
            RabbitQueueFactory factory = null;
            RabbitQueue queue = null;
            try {
                factory = new RabbitQueueFactoryImpl(cFactory.newConnection());
                DataReceiver receiver = DataReceiverImpl.builder().dataHandler(this).consumerBuilder(builder)
                        .maxParallelProcessedMsgs(maxParallelProcessedMsgs).queue(factory, queueName).build();
                terminationMutex.acquire();
                receiver.closeWhenFinished();
            } catch (Exception e) {
                LOGGER.error("Receiver crashed with Exception.", e);
                error = e;
            } finally {
                IOUtils.closeQuietly(queue);
                IOUtils.closeQuietly(factory);
            }
        }

        protected class MsgProcessingTask implements Runnable {

            private Delivery delivery;

            public MsgProcessingTask(Delivery delivery) {
                this.delivery = delivery;
            }

            @Override
            public void run() {
                processMsg(RabbitMQUtils.readString(delivery.getBody()));
            }

        }

        private void processMsg(String msg) {
            int id = Integer.parseInt(msg);
            synchronized (receivedMsgIds) {
                if (receivedMsgIds.get(id)) {
                    LOGGER.error("Received id {} a second time.", id);
                    if (error != null) {
                        error = new IllegalStateException("Received at least one message twice");
                    }
                } else {
                    receivedMsgIds.set(id);
                }
            }
            try {
                Thread.sleep(messageProcessingDelay);
            } catch (InterruptedException e) {
            }
        }

        public BitSet getReceivedMsgIds() {
            return receivedMsgIds;
        }

        public void terminate() {
            terminationMutex.release();
        }

        public Throwable getError() {
            return error;
        }

        // @Override
        // public void handleData(byte[] data) {
        // processMsg(RabbitMQUtils.readString(data));
        // }

        @Override
        public void handleIncomingStream(String streamId, InputStream stream) {
            try {
                processMsg(RabbitMQUtils.readString(StreamUtils.getBytes(stream)));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            int length = 1;
            byte data[] = new byte[1024];
            while(length >= 0) {
                try {
                    length = stream.read(data);
                } catch (IOException e) {
                    LOGGER.error("Got an exception while reading incoming data. Aborting.", e);
                }
            }
        }
    }
}
