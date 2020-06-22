package org.hobbit.core.containerservice;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.Future;

import org.apache.commons.io.Charsets;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.data.StartCommandData;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.SettableFuture;
import com.rabbitmq.client.AMQP.BasicProperties;

public abstract class DirectContainerCreator extends AbstractBenchmarkController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DirectContainerCreator.class);
	
	AbstractCommandReceivingComponent directComponent = null;

	protected Future<String> createContainerAsync(String imageName, String containerType, String[] envVariables, String[] netAliases) {
        try {
        	System.out.println("Inside RabbitMQContainerCreater");
            envVariables = extendContainerEnvVariables(envVariables);

            initResponseQueue();
            String correlationId = UUID.randomUUID().toString();
            SettableFuture<String> containerFuture = SettableFuture.create();

            synchronized (responseFutures) {
                responseFutures.put(correlationId, containerFuture);
            }

            byte data[] = RabbitMQUtils.writeString(
                    gson.toJson(new StartCommandData(imageName, containerType, containerName, envVariables, netAliases)));
            BasicProperties.Builder propsBuilder = new BasicProperties.Builder();
            propsBuilder.deliveryMode(2);
            propsBuilder.replyTo(responseQueueName);
            propsBuilder.correlationId(correlationId);
            BasicProperties props = propsBuilder.build();
            
            
         
         byte sessionIdBytes[] = getHobbitSessionId().getBytes(Charsets.UTF_8);
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
            
            
            
            
          directComponent.createDummyComponent(command, data, sessionId, props);
            
            return containerFuture;
        } catch (Exception e) {
            LOGGER.error("Got exception while trying to request the creation of an instance of the \"" + imageName
                    + "\" image.", e);
        }
        return null;
    }
	
	protected void createDataGenerators(String dataGeneratorImageName, int numberOfDataGenerators,
            String[] envVariables, AbstractCommandReceivingComponent dummyComponent) {
		this.directComponent = dummyComponent;
        createGenerator(dataGeneratorImageName, numberOfDataGenerators, envVariables, dataGenContainerIds);
    }
	
	protected void createTaskGenerators(String taskGeneratorImageName, int numberOfTaskGenerators,
            String[] envVariables, AbstractCommandReceivingComponent dummyComponent) {
		this.directComponent = dummyComponent;
        createGenerator(taskGeneratorImageName, numberOfTaskGenerators, envVariables, taskGenContainerIds);
    }
	
	protected void createEvaluationStorage(String evalStorageImageName, String[] envVariables,
			AbstractCommandReceivingComponent dummyComponent) {
		this.directComponent = dummyComponent;
        evalStoreContainerId = createContainer(evalStorageImageName, Constants.CONTAINER_TYPE_DATABASE, envVariables);
        if (evalStoreContainerId == null) {
            String errorMsg = "Couldn't create evaluation storage. Aborting.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }
	

}