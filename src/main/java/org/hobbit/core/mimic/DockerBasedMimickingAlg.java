package org.hobbit.core.mimic;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.components.ContainerStateObserver;
import org.hobbit.core.components.PlatformConnector;
import org.hobbit.core.data.ContainerTermination;
import org.hobbit.core.data.RabbitQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;

/**
 * This implementation of a {@link MimickingAlgorithmManager} creates a Docker
 * container for executing the mimicking algorithm and assumes that the
 * algorithm will send its data via a RabbitMQ queue. The name of the queue is
 * given as environment variable with the key
 * {@link Constants#DATA_QUEUE_NAME_KEY}. The generated data is written to a
 * file with the given file name.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class DockerBasedMimickingAlg implements MimickingAlgorithmManager, ContainerStateObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerBasedMimickingAlg.class);

    private static final long DEFAULT_TIMEOUT = 1000;

    /**
     * The name of the image containing the mimicking algorithm.
     */
    private String dockerImage;
    /**
     * The connector that offers the needed functionality of the Hobbit
     * platform.
     */
    private PlatformConnector connector;
    /**
     * The containers and their termination status.
     */
    private Map<String, ContainerTermination> terminations = new HashMap<String, ContainerTermination>();

    public DockerBasedMimickingAlg(PlatformConnector connector, String dockerImage) {
        this.dockerImage = dockerImage;
        this.connector = connector;
    }

    @Override
    public void generateData(String outputFile, String[] envVariables) throws Exception {
        RabbitQueue queue = null;
        String containerName = null;
        OutputStream os = null;
        ContainerTermination termination = null;
        try {
            // create the output file
            os = new BufferedOutputStream(new FileOutputStream(outputFile));
            // create the queue to get data from the container
            queue = connector.createDefaultRabbitQueue(UUID.randomUUID().toString().replace("-", ""));
            // create a consumer that writes incoming data to the file
            QueueingConsumer consumer = new QueueingConsumer(queue.channel);
            queue.channel.basicConsume(queue.name, true, consumer);

            // create the container
            containerName = connector.createContainer(dockerImage, envVariables, this);
            if (containerName == null) {
                throw new Exception("Couldn't create container with image \"" + dockerImage + "\".");
            }
            try {
                synchronized (terminations) {
                    if (terminations.containsKey(containerName)) {
                        termination = terminations.get(containerName);
                    } else {
                        termination = new ContainerTermination();
                        terminations.put(containerName, termination);
                    }
                }

                Delivery delivery;
                while ((!termination.isTerminated()) || (queue.channel.messageCount(queue.name) > 0)) {
                    delivery = consumer.nextDelivery(DEFAULT_TIMEOUT);
                    if (delivery != null) {
                        os.write(delivery.getBody());
                    }
                }
            } finally {
                // a problem occurred -> destroy the container
                connector.stopContainer(containerName);
            }
        } finally {
            // close the queue and the file
            IOUtils.closeQuietly(queue);
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public void containerStopped(String containerName, int exitCode) {
        synchronized (terminations) {
            ContainerTermination termination = null;
            if (terminations.containsKey(containerName)) {
                termination = terminations.get(containerName);
            } else {
                LOGGER.warn("Got a termination message for an unknown container. Adding it.");
                termination = new ContainerTermination();
                terminations.put(containerName, termination);
            }
            termination.notifyTermination(exitCode);
        }
    }

}
