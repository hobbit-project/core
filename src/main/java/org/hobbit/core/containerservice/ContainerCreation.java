package org.hobbit.core.containerservice;

import org.hobbit.core.components.AbstractCommandReceivingComponent;
/**
 * ContainerCreation provides the facility to implement the functionalities 
 * to create {@link DirectContainerCreator} or {@link RabbitMQContainerCreator}
 * @author altaf, sourabh, yamini, melisa
 *
 */
public interface ContainerCreation {
	
	void createDataGenerators(String dataGeneratorImageName, int numberOfDataGenerators,
            String[] envVariables, AbstractCommandReceivingComponent dummyComponent);
	
	void createTaskGenerators(String taskGeneratorImageName, int numberOfTaskGenerators,
            String[] envVariables, AbstractCommandReceivingComponent dummyComponent);
	
	void createEvaluationStorage(String evalStorageImageName, String[] envVariables,
			AbstractCommandReceivingComponent dummyComponent);

}
