package org.hobbit.core.containerservice;

import java.util.Set;

import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.components.AbstractPlatformController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the functionality for RabbitMQ container creation.
 *
 * @author Altafhusen Makandar
 * @author Sourabh Poddar
 * @author Yamini Punetha
 * @author Melissa Das
 *
 */
public class RabbitMQContainerCreator implements ContainerCreation {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQContainerCreator.class);
	
	private AbstractBenchmarkController abstractBenchmarkController;
	
	public RabbitMQContainerCreator(AbstractBenchmarkController abstractBenchmarkController) {
		this.abstractBenchmarkController = abstractBenchmarkController;
	}

    /**
     * This method calls the createGenerator method of {@link AbstractBenchmarkController}
     *
     * @param dataGeneratorImageName
     *            name of the data generator Docker image
     * @param numberOfDataGenerators
     *            number of generators that should be created
     * @param envVariables
     *            environment variables required for the creation of the data generators
     * @param dummyComponent
     *
     */
	@Override
	public Set<String> createDataGenerators(String dataGeneratorImageName, int numberOfDataGenerators, String[] envVariables,
	        AbstractPlatformController dummyComponent) {
		return abstractBenchmarkController.createGenerator(dataGeneratorImageName, numberOfDataGenerators, envVariables);

	}

    /**
     * This method calls the createTaskGenerators method of {@link AbstractBenchmarkController}
     *
     * @param taskGeneratorImageName
     *            name of the task generator Docker image
     * @param numberOfTaskGenerators
     *            number of generators that should be created
     * @param envVariables
     *            environment variables required for the creation of the task generators
     * @param dummyComponent
     *
     */
	@Override
	public Set<String> createTaskGenerators(String taskGeneratorImageName, int numberOfTaskGenerators, String[] envVariables,
	        AbstractPlatformController dummyComponent) {
		return abstractBenchmarkController.createGenerator(taskGeneratorImageName, numberOfTaskGenerators, envVariables);

	}

    /**
     * This method calls the createEvaluationStorage method of {@link AbstractBenchmarkController}
     *
     * @param evalStorageImageName
     *            name of the evaluation storage image
     * @param envVariables
     *            environment variables required for the creation of evaluation storage
     * @param dummyComponent
     * @return the container id of the evaluation storage.
     */
	@Override
	public String createEvaluationStorage(String evalStorageImageName, String[] envVariables,
	        AbstractPlatformController dummyComponent) {
		abstractBenchmarkController.setEvalStoreContainerId(abstractBenchmarkController.createContainer(evalStorageImageName, Constants.CONTAINER_TYPE_DATABASE, envVariables));
        if (abstractBenchmarkController.getEvalStoreContainerId() == null) {
            String errorMsg = "Couldn't create evaluation storage. Aborting.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        return abstractBenchmarkController.getEvalStoreContainerId();
	}
}

