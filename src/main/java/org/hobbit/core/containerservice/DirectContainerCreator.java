package org.hobbit.core.containerservice;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.apache.commons.io.Charsets;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.components.AbstractPlatformController;
import org.hobbit.core.data.StartCommandData;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.utils.EnvVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * This class provides the implementation to create container functionality
 * @author altaf, sourabh, yamini, melisa
 *
 */
public class DirectContainerCreator implements ContainerCreation {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectContainerCreator.class);
	
    private AbstractPlatformController platformController = null;
	
    private AbstractCommandReceivingComponent abstractCommandReceivingComponent;
	
    private String containerName;
	
    private String hobbitSessionId;
	
    public DirectContainerCreator(AbstractBenchmarkController abstractBenchmarkController) {
        this.abstractCommandReceivingComponent = abstractBenchmarkController;
        hobbitSessionId = EnvVariables.getString(Constants.HOBBIT_SESSION_ID_KEY,
                Constants.HOBBIT_SESSION_ID_FOR_PLATFORM_COMPONENTS);
        containerName = EnvVariables.getString(Constants.CONTAINER_NAME_KEY, containerName);
    }

    /**
     * This method creates and starts an instance of the given image using the given
     * environment variables.
     * <p>
     * Note that the containerType parameter should have one of the following
     * values.
     * <ul>
     * <li>{@link Constants#CONTAINER_TYPE_DATABASE} if this container is part
     * of a benchmark but should be located on a storage node.</li>
     * </ul>
     *
     * @param imageName
     *            the name of the image of the docker container
     * @param containerType
     *            the type of the container
     * @param envVariables
     *            environment variables that should be added to the created
     *            container
     * @param netAliases
     *            network aliases that should be added to the created container
     * @return the Future object with the name of the container instance or null if an error occurred
     */
    protected Future<String> createContainer(String imageName, String containerType, String[] envVariables, String[] netAliases) {
        try {
            Gson gson = new Gson();
            envVariables = extendContainerEnvVariables(envVariables);
            abstractCommandReceivingComponent.initResponseQueue(); 
            String correlationId = UUID.randomUUID().toString(); 
            SettableFuture<String> containerFuture = SettableFuture.create();
            synchronized (abstractCommandReceivingComponent.getResponseFutures()) {
                abstractCommandReceivingComponent.getResponseFutures().put(correlationId, containerFuture); 
            }
            byte data[] = RabbitMQUtils.writeString(
                    gson.toJson(new StartCommandData(imageName, containerType, containerName, envVariables, netAliases)));
            BasicProperties.Builder propsBuilder = new BasicProperties.Builder();
            propsBuilder.deliveryMode(2);
            propsBuilder.replyTo(abstractCommandReceivingComponent.getResponseQueueName());
            propsBuilder.correlationId(correlationId);
            BasicProperties props = propsBuilder.build();
            byte sessionIdBytes[] = hobbitSessionId.getBytes(Charsets.UTF_8);
            int dataLength = sessionIdBytes.length + 5;
            boolean attachData = (data != null) && (data.length > 0);
            if (attachData) {
                dataLength += data.length;
            }
            ByteBuffer buffer = ByteBuffer.allocate(dataLength);
            buffer.putInt(sessionIdBytes.length);
            buffer.put(sessionIdBytes);
            buffer.put(Commands.DOCKER_CONTAINER_START);
            if (attachData) {
                buffer.put(data);
            }
            byte sessionIdBytes1[] = new byte[sessionIdBytes.length];
            String sessionId = new String(sessionIdBytes1, Charsets.UTF_8);
            byte command = Commands.DOCKER_CONTAINER_START;
            platformController.createComponent(command, data, sessionId, props);
            return containerFuture;
        } catch (Exception e) {
            LOGGER.error("Got exception while trying to request the creation of an instance of the \"" + imageName
                    + "\" image.", e);
        }
        return null;
    }
	
    /**
     * Creates the given number of data generators using the given image name
     * and environment variables.
     *
     * @param dataGeneratorImageName
     *            name of the data generator Docker image
     * @param numberOfDataGenerators
     *            number of generators that should be created
     * @param envVariables
     *            environment variables for the data generators
     */
    @Override
    public Set<String> createDataGenerators(String dataGeneratorImageName, int numberOfDataGenerators,
            String[] envVariables, AbstractPlatformController dummyComponent) {
        this.platformController = dummyComponent;
        return createGenerator(dataGeneratorImageName, numberOfDataGenerators, envVariables);
    }
	
    /**
     * Creates the given number of task generators using the given image name
     * and environment variables.
     *
     * @param taskGeneratorImageName
     *            name of the task generator Docker image
     * @param numberOfTaskGenerators
     *            number of generators that should be created
     * @param envVariables
     *            environment variables for the task generators
     */
    @Override
    public Set<String> createTaskGenerators(String taskGeneratorImageName, int numberOfTaskGenerators,
            String[] envVariables, AbstractPlatformController dummyComponent) {
        this.platformController = dummyComponent;
        return createGenerator(taskGeneratorImageName, numberOfTaskGenerators, envVariables);
    }
	
    /**
     * Creates the evaluate storage using the given image name and environment
     * variables.
     *
     * @param evalStorageImageName
     *            name of the evaluation storage image
     * @param envVariables
     *            environment variables that should be given to the component
     */
    @Override
    public String createEvaluationStorage(String evalStorageImageName, String[] envVariables,
            AbstractPlatformController dummyComponent) {
        this.platformController = dummyComponent;
        String evaluationStoreContainerId = null;
        evaluationStoreContainerId = abstractCommandReceivingComponent.createContainer(evalStorageImageName, Constants.CONTAINER_TYPE_DATABASE, envVariables);
        if (evaluationStoreContainerId == null) {
            String errorMsg = "Couldn't create evaluation storage. Aborting.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        return evaluationStoreContainerId;
    }

    /**
     * Internal method for creating generator components.
     *
     * @param generatorImageName
     *            name of the generator Docker image
     * @param numberOfGenerators
     *            number of generators that should be created
     * @param envVariables
     *            environment variables for the task generators
     * @param generatorIds
     *            set of generator container names
     */
    public Set<String> createGenerator(String generatorImageName, int numberOfGenerators, String[] envVariables) {
        Set<String> generatorIds = new HashSet<>();
        try {
            String containerId;
            String variables[] = envVariables != null ? Arrays.copyOf(envVariables, envVariables.length + 2)
                    : new String[2];
            // NOTE: Count only includes generators created within this method call.
            variables[variables.length - 2] = Constants.GENERATOR_COUNT_KEY + "=" + numberOfGenerators;
            for (int i = 0; i < numberOfGenerators; ++i) {
                // At the start generatorIds is empty, and new generators are added to it immediately.
                // Current size of that set is used to make IDs for new generators.
                variables[variables.length - 1] = Constants.GENERATOR_ID_KEY + "=" + generatorIds.size();
                containerId = createContainer(generatorImageName, null, envVariables, null).get();// createContainer(generatorImageName, variables);
                if (containerId != null) {
                    generatorIds.add(containerId);
                } else {
                    String errorMsg = "Couldn't create generator component. Aborting.";
                    LOGGER.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
            }
        }catch(Exception e) {
            LOGGER.error("Exception occured",e);
        }
        return generatorIds;
    }
	
    public String[] extendContainerEnvVariables(String[] envVariables) {
        String rabbitMQHostName = EnvVariables.getString(Constants.RABBIT_MQ_HOST_NAME_KEY, LOGGER);
        if (envVariables == null) {
            envVariables = new String[0];
        }

        // Only add RabbitMQ host env if there isn't any.
        if (Stream.of(envVariables).noneMatch(kv -> kv.startsWith(Constants.RABBIT_MQ_HOST_NAME_KEY + "="))) {
            envVariables = Arrays.copyOf(envVariables, envVariables.length + 2);
            envVariables[envVariables.length - 2] = Constants.RABBIT_MQ_HOST_NAME_KEY + "=" + rabbitMQHostName;
        } else {
            envVariables = Arrays.copyOf(envVariables, envVariables.length + 1);
        }

        envVariables[envVariables.length - 1] = Constants.HOBBIT_SESSION_ID_KEY + "=" + hobbitSessionId;
        return envVariables;
    }

  
}