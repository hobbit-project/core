package org.hobbit.core.components;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.hobbit.core.Commands;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.RabbitMQUtils;

/**
 * This extension of the {@link AbstractCommandReceivingComponent} offers some
 * platform functionalities to other classes by implementing the
 * {@link PlatformConnector} interface.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractPlatformConnectorComponent extends AbstractCommandReceivingComponent
        implements PlatformConnector {

    private Map<String, ContainerStateObserver> containerObservers = new HashMap<String, ContainerStateObserver>();

    /**
     * {@inheritDoc}
     */
    @Override
    public RabbitQueue createDefaultRabbitQueue(String name) throws IOException {
        return super.createDefaultRabbitQueue(name);
    }

    @Override
    public String createContainer(String imageName, String[] envVariables, ContainerStateObserver observer) {
        if ((observer == null) || (imageName == null)) {
            return null;
        }
        String containerName = createContainer(imageName, envVariables);
        if (containerName != null) {
            containerObservers.put(containerName, observer);
        }
        return containerName;
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        if (command == Commands.DOCKER_CONTAINER_TERMINATED) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            String containerName = RabbitMQUtils.readString(buffer);
            int exitCode = buffer.get();
            if (containerObservers.containsKey(containerName)) {
                containerObservers.get(containerName).containerStopped(containerName, exitCode);
                containerObservers.remove(containerName);
            }
        }
    }

    @Override
    public void stopContainer(String containerName) {
        super.stopContainer(containerName);
    }

}
