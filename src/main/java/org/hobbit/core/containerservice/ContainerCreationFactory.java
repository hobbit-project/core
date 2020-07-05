package org.hobbit.core.containerservice;

import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractBenchmarkController;

/**
 * This class provides functionality to obtain an instance of {@link ContainerCreation}
 *
 * @author Altafhusen Makandar
 * @author Sourabh Poddar
 * @author Yamini Punetha
 * @author Melissa Das
 *
 */
public class ContainerCreationFactory {

    /**
     * This static method returns the instance of {@link RabbitMQContainerCreator} or {@link DirectContainerCreator}
     * based on the value of the environment variable {@link Constants#RABBIT_CONTAINER_SERVICE}
     *
     * @param isRabbitContainerService
     *            the environment variable that decides which instance of {@link ContainerCreation} will be returned.
     * @param abstractBenchmarkController
     *            instance of {@link AbstractBenchmarkController}
     * @return an instance of {@link RabbitMQContainerCreator} or {@link DirectContainerCreator}
     */
	public static ContainerCreation getContainerCreationObject(String isRabbitContainerService, AbstractBenchmarkController abstractBenchmarkController) {
		if(isRabbitContainerService.equals("true")) {
			return new RabbitMQContainerCreator(abstractBenchmarkController);
		}
		return new DirectContainerCreator(abstractBenchmarkController);
	}

}
