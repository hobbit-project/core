package org.hobbit.core.rabbit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hobbit.core.TestConstants;
import org.hobbit.core.utils.IdGenerator;
import org.hobbit.core.utils.SteppingIdGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ParallelDataStreamingTest implements IncomingStreamHandler {

    private static final String TEST_RESOURCES_DIR = "src/test/resources/org/hobbit/storage/queries";
    private static final int MESSAGE_SIZE = 256;

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testConfigs = new ArrayList<Object[]>();
//        testConfigs.add(new Object[] { 1, 1, 1 });
//        testConfigs.add(new Object[] { 2, 2, 5 });
//         testConfigs.add(new Object[] { 3, 1, 5 });
//         testConfigs.add(new Object[] { 1, 2, 5 });
//         testConfigs.add(new Object[] { 5, 2, 5 });
         testConfigs.add(new Object[] { 5, 2, 20 });
         testConfigs.add(new Object[] { 5, 3, 50 });
        return testConfigs;
    }

    private int senderCount;
    private int receiverCount;
    private int filesPerSender;
    private String outputDir;
    private final List<Exception> exceptions = new ArrayList<>();
    private Map<String, String> streamMapping = new HashMap<>();

    public ParallelDataStreamingTest(int senderCount, int receiverCount, int filesPerSender) {
        this.senderCount = senderCount;
        this.receiverCount = receiverCount;
        this.filesPerSender = filesPerSender;
    }

    @Test(timeout = 30000)
    public void testStream() {
        System.out.println("Starting test... with " + senderCount + " senders, " + receiverCount + " receivers and "
                + filesPerSender + " files per sender.");
        try {
            outputDir = FileStreamingTest.getTempDir();
            Objects.requireNonNull(outputDir);

            String queueName = UUID.randomUUID().toString().replace("-", "");
            ParallelDataStreamingTest testInstance = this;

            // we need to make sure that the receivers have been created before
            // we check their status
            final Semaphore receiverStartedSemaphore = new Semaphore(0);
            // we need to make sure that the receivers are not closed too early
            final Semaphore closeReceiverSemaphore = new Semaphore(0);

            System.out.println("Starting receiver...");
            // final DataReceiver receivers[] = new DataReceiver[receiverCount];
            Thread receiverThreads[] = new Thread[receiverCount];
            DataReceiver receivers[] = new DataReceiver[receiverCount];
            for (int i = 0; i < receiverCount; ++i) {
                final int receiverId = i;
                receiverThreads[i] = new Thread(new Runnable() {
                    public void run() {
                        SimpleRabbitQueueFactory factory = null;
                        DataReceiver receiver = null;
                        try {
                            factory = SimpleRabbitQueueFactory.create(TestConstants.RABBIT_HOST);
                            receiver = (new DataReceiverImpl.Builder()).dataHandler(testInstance)
                                    .maxParallelProcessedMsgs(100).queue(factory, queueName).build();
                            receiverStartedSemaphore.release();
                            receivers[receiverId] = receiver;
                            closeReceiverSemaphore.acquire();
                            receiver.closeWhenFinished();
                        } catch (Exception e) {
                            e.printStackTrace();
                            exceptions.add(e);
                        } finally {
                            IOUtils.closeQuietly(receiver);
                            IOUtils.closeQuietly(factory);
                        }
                    }
                });
                receiverThreads[i].start();
            }

            Thread senderThreads[] = new Thread[senderCount];
            System.out.println("Starting sender...");
            for (int i = 0; i < senderCount; ++i) {
                final int senderId = i;
                senderThreads[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        File files[] = (new File(TEST_RESOURCES_DIR)).listFiles();
                        Random random = new Random();
                        SimpleRabbitQueueFactory factory = null;
                        DataSender sender = null;
                        try {
                            factory = SimpleRabbitQueueFactory.create(TestConstants.RABBIT_HOST);
                            sender = (new DataSenderImpl.Builder()).queue(factory, queueName).idGenerator(null)
                                    .messageSize(MESSAGE_SIZE).build();
                            InputStream is = null;
                            IdGenerator generator = new SteppingIdGenerator(senderId, senderCount);
                            String streamId;
                            for (int j = 0; j < filesPerSender; ++j) {
                                try {
                                    File file = files[random.nextInt(files.length)];
                                    streamId = generator.getNextId();
                                    is = new BufferedInputStream(FileUtils.openInputStream(file));
                                    sender.sendData(is, streamId);
                                    addStreamMapping(streamId, file.getAbsolutePath());
                                } finally {
                                    IOUtils.closeQuietly(is);
                                }
                            }
                            sender.closeWhenFinished();
                        } catch (Exception e) {
                            e.printStackTrace();
                            exceptions.add(e);
                        } finally {
                            IOUtils.closeQuietly(sender);
                            IOUtils.closeQuietly(factory);
                        }
                    }
                });
                senderThreads[i].start();
            }

            System.out.println("Waiting for sender...");
            for (int i = 0; i < senderCount; ++i) {
                senderThreads[i].join();
            }
            receiverStartedSemaphore.acquire(receiverCount);
            closeReceiverSemaphore.release(receiverCount);
            System.out.println("Waiting for receiver...");
            for (int i = 0; i < receiverCount; ++i) {
                receiverThreads[i].join();
            }

            // make sure that there are no exceptions
            Assert.assertEquals("Exceptions occured.", 0, exceptions.size());
            for (int i = 0; i < receiverCount; ++i) {
                Assert.assertEquals("One of the receivers encountered an error!", 0, receivers[i].getErrorCount());
            }

            String fileName;
            for (String streamId : streamMapping.keySet()) {
                fileName = streamMapping.get(streamId);
                Assert.assertArrayEquals(
                        "Content of the original file " + fileName + " and the received stream " + streamId
                                + " differ.",
                        FileUtils.readFileToByteArray(new File(fileName)),
                        FileUtils.readFileToByteArray(new File(outputDir + "/" + streamId)));
            }
            Assert.assertEquals(senderCount * filesPerSender, streamMapping.size());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Catched an exception: " + e.toString());
        }
    }

    protected synchronized void addStreamMapping(String streamId, String fileName) {
        streamMapping.put(streamId, fileName);
    }

    @Override
    public void handleIncomingStream(String streamId, InputStream stream) {
        try {
            FileUtils.copyInputStreamToFile(stream, new File(outputDir + "/" + streamId));
            System.out.println(outputDir + "/" + streamId + " written");
        } catch (IOException e) {
            e.printStackTrace();
            exceptions.add(e);
        }
    }

}
