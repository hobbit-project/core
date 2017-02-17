package org.hobbit.core.rabbit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hobbit.core.data.RabbitQueue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class FileStreamingTest implements RabbitQueueFactory {

    // TODO find a better way to define the host
    public static final String RABBIT_HOST = "192.168.99.100";

    private Connection connection = null;

    @Before
    public void before() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBIT_HOST);
        connection = factory.newConnection();
    }

    @Test
    public void test() {
        try {
            String sendInputDir = getTempDir();
            Assert.assertNotNull(sendInputDir);
            generateFiles(sendInputDir);
            String receiveOutputDir = getTempDir();
            Assert.assertNotNull(receiveOutputDir);
            String queueName = UUID.randomUUID().toString().replace("-", "");
            List<Exception> exceptions = new ArrayList<>();

            System.out.println("Starting receiver...");
            SimpleFileReceiver receiver = SimpleFileReceiver.create(this, queueName);
            List<String> receivedFiles = new ArrayList<>();
            Thread receiverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        receivedFiles.addAll(Arrays.asList(receiver.receiveData(receiveOutputDir)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        exceptions.add(e);
                    }
                }
            }, "receiver");
            receiverThread.start();

            System.out.println("Starting sender...");
            SimpleFileSender sender = SimpleFileSender.create(this, queueName);
            Thread senderThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    File inputDir = new File(sendInputDir);
                    InputStream is = null;
                    for (File f : inputDir.listFiles()) {
                        try {
                            is = new BufferedInputStream(new FileInputStream(f));
                            sender.streamData(is, f.getName());
                        } catch (Exception e) {
                            e.printStackTrace();
                            exceptions.add(e);
                        } finally {
                            IOUtils.closeQuietly(is);
                        }
                    }
                    // We have to tell the receiver that we have finished the
                    // sending
                    receiver.terminate();
                }
            }, "sender");
            senderThread.start();

            System.out.println("Waiting for sender...");
            senderThread.join();
            System.out.println("Waiting for receiver...");
            receiverThread.join();

            // make sure that there are no exceptions
            Assert.assertEquals("Exceptions occured.", 0, exceptions.size());

            System.out.println("Comparing files...");
            compareFiles(sendInputDir, receiveOutputDir);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void compareFiles(String sendInputDir, String receiveOutputDir) throws IOException {
        File inputDir = new File(sendInputDir);
        File outputDir = new File(receiveOutputDir);
        File inputFiles[] = inputDir.listFiles();
        Assert.assertTrue(inputFiles.length > 0);

        for (File inputFile : inputFiles) {
            compareFiles(inputFile, new File(outputDir.getAbsolutePath() + File.separator + inputFile.getName()));
        }

        Assert.assertEquals(inputFiles.length, outputDir.listFiles().length);
    }

    private void compareFiles(File inputFile, File outputFile) throws IOException {
        Assert.assertTrue(outputFile.exists());
        byte[] inputFileContent = FileUtils.readFileToByteArray(inputFile);
        byte[] outputFileContent = FileUtils.readFileToByteArray(outputFile);
        Assert.assertArrayEquals(inputFileContent, outputFileContent);
    }

    private void generateFiles(String sendInputDir) {
        System.out.println("Generating files...");
        if (!sendInputDir.endsWith(File.separator)) {
            sendInputDir += File.separator;
        }

        OutputStream os = null;
        // create first file
        try {
            os = new BufferedOutputStream(new FileOutputStream(sendInputDir + "file1.dat"));
            ByteBuffer buffer = ByteBuffer.allocate(4000);
            IntBuffer intBuffer = buffer.asIntBuffer();
            int number = 0;
            // for (int i = 0; i < 200; ++i) {
            for (int j = 0; j < 1000; ++j) {
                intBuffer.put(number);
                ++number;
            }
            os.write(buffer.array());
            buffer.position(0);
            intBuffer.position(0);
            // }
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            IOUtils.closeQuietly(os);
        }
        // create second file
        try {
            os = new BufferedOutputStream(new FileOutputStream(sendInputDir + "file2.txt"));
            byte data[] = "Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquid ex ea commodi consequat. Quis aute iure reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint obcaecat cupiditat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
                    .getBytes(Charsets.UTF_8);
            for (int i = 0; i < 200; ++i) {
                os.write(data);
            }
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            IOUtils.closeQuietly(os);
        }
        // create third file
        try {
            os = new BufferedOutputStream(new FileOutputStream(sendInputDir + "file3.dat"));
            ByteBuffer buffer = ByteBuffer.allocate(400);
            IntBuffer intBuffer = buffer.asIntBuffer();
            Random random = new Random();
            // for (int i = 0; i < 200; ++i) {
            for (int j = 0; j < 100; ++j) {
                intBuffer.put(random.nextInt());
            }
            os.write(buffer.array());
            buffer.position(0);
            intBuffer.position(0);
            // }
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public RabbitQueue createDefaultRabbitQueue(String name) throws IOException {
        Channel channel = connection.createChannel();
        channel.queueDeclare(name, false, false, true, null);
        return new RabbitQueue(channel, name);
    }

    public static String getTempDir() throws IOException {
        File tempFile = File.createTempFile("junittest", "Temp");
        if (!tempFile.delete()) {
            return null;
        }
        if (!tempFile.mkdir()) {
            return null;
        }
        return tempFile.getAbsolutePath();
    }
}
