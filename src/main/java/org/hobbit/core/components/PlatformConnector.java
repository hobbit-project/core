package org.hobbit.core.components;

import java.io.IOException;

import org.hobbit.core.Commands;
import org.hobbit.core.data.RabbitQueue;

import com.rabbitmq.client.Channel;

/**
 * This interface should be implemented by components if they want to offer
 * functionality of the platform to other classes.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface PlatformConnector {

    /**
     * This method opens a channel using the established {@link #connection} to
     * RabbitMQ and creates a new queue using the given name and the following
     * configuration:
     * <ul>
     * <li>The channel number is automatically derived from the connection.</li>
     * <li>The queue is not durable.</li>
     * <li>The queue is not exclusive.</li>
     * <li>The queue is configured to be deleted automatically.</li>
     * <li>No additional queue configuration is defined.</li>
     * </ul>
     * 
     * @param name
     *            name of the queue
     * @return {@link RabbitQueue} object comprising the {@link Channel} and the
     *         name of the created queue
     * @throws IOException
     */
    public RabbitQueue createDefaultRabbitQueue(String name) throws IOException;

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
     * @param observer
     *            the container state observer that is called if the container
     *            terminates
     * @return the name of the container instance or null if an error occurred
     */
    public String createContainer(String imageName, String[] envVariables, ContainerStateObserver observer);

    /**
     * This method sends a {@link Commands#DOCKER_CONTAINER_STOP} command to
     * stop the container with the given id.
     * 
     * @param containerName
     *            the name of the container instance that should be stopped
     */
    public void stopContainer(String containerName);

}
