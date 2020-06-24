package org.hobbit.core.containerservice;

import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements functionality for RabbitMQ container creation
 * @author altaf, sourabh, yamini, melisa
 *
 */
public class RabbitMQContainerCreator implements ContainerCreation {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQContainerCreator.class);
	
	private AbstractBenchmarkController abstractBenchmarkController;
	
	public RabbitMQContainerCreator(AbstractBenchmarkController abstractBenchmarkController) {
		this.abstractBenchmarkController = abstractBenchmarkController;
	}

	/**
	 * This method calls the createGenerator of {@link AbstractBenchmarkController} as the implementation
	 * is already implemented there
	 */
	@Override
	public void createDataGenerators(String dataGeneratorImageName, int numberOfDataGenerators, String[] envVariables,
			AbstractCommandReceivingComponent dummyComponent) {
		abstractBenchmarkController.createGenerator(dataGeneratorImageName, numberOfDataGenerators, envVariables, abstractBenchmarkController.getDataGenContainerIds());

	}

	/**
	 * This method calls the createTaskGenerators of {@link AbstractBenchmarkController} as the implementation
	 * is already implemented there
	 */
	@Override
	public void createTaskGenerators(String taskGeneratorImageName, int numberOfTaskGenerators, String[] envVariables,
			AbstractCommandReceivingComponent dummyComponent) {
		abstractBenchmarkController.createGenerator(taskGeneratorImageName, numberOfTaskGenerators, envVariables, abstractBenchmarkController.getTaskGenContainerIds());

	}

	/**
	 * This method calls the createEvaluationStorage of {@link AbstractBenchmarkController} as the implementation
	 * is already implemented there
	 */
	@Override
	public void createEvaluationStorage(String evalStorageImageName, String[] envVariables,
			AbstractCommandReceivingComponent dummyComponent) {
		abstractBenchmarkController.setEvalStoreContainerId(abstractBenchmarkController.createContainer(evalStorageImageName, Constants.CONTAINER_TYPE_DATABASE, envVariables));
        if (abstractBenchmarkController.getEvalStoreContainerId() == null) {
            String errorMsg = "Couldn't create evaluation storage. Aborting.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

	}

}
