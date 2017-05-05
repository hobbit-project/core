package org.hobbit.core.rabbit;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.components.AbstractComponent;
import org.hobbit.core.components.dummy.DummyComponentExecutor;
import org.hobbit.core.data.RabbitQueue;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;

/**
 * A simple performance test with a sender - receiver scenario. Note that this
 * test needs the environmental variable HOBBIT_RABBIT_HOST.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
@Ignore
public class PerformanceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTest.class);

    private static final String QUEUE_NAME = "perfQ";
    private static final byte[] MESSAGE_CONTENT = RabbitMQUtils.writeString("This is a simple Test message");
    private static final int NUMBER_OF_MESSAGES = 1000000;
    private static final int NUMBER_OF_SENDERS = 1;

    public static void main(String[] args) throws InterruptedException {
        Receiver receiver = new Receiver();
        Thread receiverThread = new Thread(new DummyComponentExecutor(receiver));
        receiverThread.start();

        Thread[] dataGenThreads = new Thread[NUMBER_OF_SENDERS];
        DummyComponentExecutor[] dataGenExecutors = new DummyComponentExecutor[NUMBER_OF_SENDERS];
        for (int i = 0; i < dataGenThreads.length; ++i) {
            Sender dataGenerator = new Sender();
            dataGenExecutors[i] = new DummyComponentExecutor(dataGenerator);
            dataGenThreads[i] = new Thread(dataGenExecutors[i]);
            dataGenThreads[i].start();
        }

        for (int i = 0; i < dataGenThreads.length; ++i) {
            dataGenThreads[i].join();
        }
        System.out.println("terminating");
        receiver.terminate();
        receiverThread.join();
    }

    protected static class Sender extends AbstractComponent {

        private RabbitQueue queue;
        private int numberOfMessages = NUMBER_OF_MESSAGES;

        @Override
        public void init() throws Exception {
            super.init();
            queue = createDefaultRabbitQueue(QUEUE_NAME);
        }

        @Override
        public void run() throws Exception {
            long start = System.currentTimeMillis();
            for (int i = 0; i < numberOfMessages; ++i) {
                queue.channel.basicPublish("", queue.name, MessageProperties.PERSISTENT_BASIC, MESSAGE_CONTENT);
            }
            long time = System.currentTimeMillis() - start;
            LOGGER.info("Sender has finished sending. Sender took {}ms ({} messages per second)", time,
                    (numberOfMessages * 1000.0 / (double) time));
            // make sure all messages have been received
            long messageCount = queue.messageCount();
            while (messageCount > 0) {
                Thread.sleep(100);
                messageCount = queue.messageCount();
            }
            time = System.currentTimeMillis() - start;
            LOGGER.info("Queue is empty and it took {}ms ({} messages per second)", time,
                    (numberOfMessages * 1000.0 / (double) time));
        }

        @Override
        public void close() throws IOException {
            IOUtils.closeQuietly(queue);
            super.close();
        }
    }

    protected static class Receiver extends AbstractComponent {

        private RabbitQueue queue;
        private Semaphore terminationMutex = new Semaphore(0);
        private int maxParallelProcessedMsgs = 1000;
        private Semaphore currentlyProcessedMessages;
        private Semaphore sawMessages = new Semaphore(0);

        @Override
        public void init() throws Exception {
            super.init();
            queue = createDefaultRabbitQueue(QUEUE_NAME);
            currentlyProcessedMessages = new Semaphore(maxParallelProcessedMsgs);
            queue.channel.basicConsume(queue.name, true, new DefaultConsumer(queue.channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties,
                        byte[] body) throws IOException {
                    try {
                        currentlyProcessedMessages.acquire();
                        // process message ...
                        sawMessages.release();
                        currentlyProcessedMessages.release();
                    } catch (InterruptedException e) {
                        throw new IOException("Interrupted while waiting for mutex.", e);
                    }
                }
            });
            queue.channel.basicQos(maxParallelProcessedMsgs);
        }

        @Override
        public void run() throws Exception {
            long time = System.currentTimeMillis();
            terminationMutex.acquire();
            // wait until all messages have been read from the queue
            long messageCount = queue.messageCount();
            while (messageCount > 0) {
                Thread.sleep(100);
                messageCount = queue.messageCount();
            }
            // Collect all open mutex counts to make sure that there is no
            // message that is still processed
            currentlyProcessedMessages.acquire(maxParallelProcessedMsgs);
            time = System.currentTimeMillis() - time;
            LOGGER.info("Receiver took {}ms for {} messages ({} messages per second)", time,
                    sawMessages.availablePermits(), ((sawMessages.availablePermits() * 1000.0) / (double) time));
        }

        public void terminate() {
            terminationMutex.release();
        }

    }
}
