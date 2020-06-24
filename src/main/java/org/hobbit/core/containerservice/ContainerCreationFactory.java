package org.hobbit.core.containerservice;

import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractBenchmarkController;

/**
 * Factory that provides functionality to get an instance of {@link ContainerCreation}
 * @author altaf, sourabh, yamini, melisa
 *
 */
public class ContainerCreationFactory {
	
	/**
	 * This method returns the instance of {@link RabbitMQContainerCreator} or {@link DirectContainerCreator}
	 * based on the environment variable {@link Constants#RABBIT_CONTAINER_SERVICE}
	 */
	public static ContainerCreation getContainerCreationObject(String isRabbitContainerService, AbstractBenchmarkController abstractBenchmarkController) {
		if(isRabbitContainerService.equals("true")) {
			return new RabbitMQContainerCreator(abstractBenchmarkController);
		}
		return new DirectContainerCreator(abstractBenchmarkController);
	}

}
