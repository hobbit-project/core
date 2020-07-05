package org.hobbit.core.containerservice;

import java.util.Set;
import org.hobbit.core.components.AbstractPlatformController;

/**
 * This interface provides methods that provides functionalities
 * to create {@link DirectContainerCreator} or {@link RabbitMQContainerCreator}
 *
 * @author Altafhusen Makandar
 * @author Sourabh Poddar
 * @author Yamini Punetha
 * @author Melissa Das
 *
 */
public interface ContainerCreation {
    
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
    Set<String> createDataGenerators(String dataGeneratorImageName, int numberOfDataGenerators,
            String[] envVariables, AbstractPlatformController dummyComponent);
    
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
    Set<String> createTaskGenerators(String taskGeneratorImageName, int numberOfTaskGenerators,
            String[] envVariables, AbstractPlatformController dummyComponent);
    
    /**
     * Creates the evaluate storage using the given image name and environment
     * variables.
     *
     * @param evalStorageImageName
     *            name of the evaluation storage image
     * @param envVariables
     *            environment variables that should be given to the component
     */
    String createEvaluationStorage(String evalStorageImageName, String[] envVariables,
	        AbstractPlatformController dummyComponent);

}
