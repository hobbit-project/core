package org.hobbit.core.components;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class implements basic methods for a benchmark controller.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractBenchmarkController extends AbstractCommandReceivingComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBenchmarkController.class);

    /**
     * The benchmark result as RDF model received from the evaluation module.
     */
    protected Model resultModel;
    /**
     * Mutex used to wait for the data generators to be ready.
     */
    protected Semaphore dataGenReadyMutex = new Semaphore(0);
    /**
     * Mutex used to wait for the task generators to be ready.
     */
    protected Semaphore taskGenReadyMutex = new Semaphore(0);
    /**
     * Mutex used to wait for the evaluation storage to be ready.
     */
    protected Semaphore evalStoreReadyMutex = new Semaphore(0);
    /**
     * Mutex used to wait for the start signal for this benchmark.
     */
    protected Semaphore benchmarkStartSignalMutex = new Semaphore(0);
    /**
     * Mutex used to wait for the data generators to terminate.
     */
    protected Semaphore dataGenTerminatedMutex = new Semaphore(0);
    /**
     * Mutex used to wait for the task generators to terminate.
     */
    protected Semaphore taskGenTerminatedMutex = new Semaphore(0);
    /**
     * Mutex used to wait for the benchmarked system to terminate.
     */
    protected Semaphore systemTerminatedMutex = new Semaphore(0);
    /**
     * Mutex used to wait for the evaluation storage to terminate.
     */
    protected Semaphore evalStoreTerminatedMutex = new Semaphore(0);
    /**
     * Mutex used to wait for the evaluation module to terminate.
     */
    protected Semaphore evalModuleTerminatedMutex = new Semaphore(0);
    /**
     * The set of data generator container ids.
     */
    private Set<String> dataGenContainerIds = new HashSet<String>();
    /**
     * The set of task generator container ids.
     */
    private Set<String> taskGenContainerIds = new HashSet<String>();
    /**
     * The container id of the evaluation storage.
     */
    private String evalStoreContainerId;
    /**
     * The container id of the benchmarked system.
     */
    private String systemContainerId;

    @Override
    public void init() throws Exception {
        super.init();

        if (System.getenv().containsKey(Constants.SYSTEM_CONTAINER_ID_KEY)) {
            systemContainerId = System.getenv().get(Constants.SYSTEM_CONTAINER_ID_KEY);
        }
        if (systemContainerId == null) {
            String errorMsg = "Couldn't get the system container id. Aborting.";
            LOGGER.error(errorMsg);
            throw new Exception(errorMsg);
        }
    }

    protected void createDataGenerators(String dataGeneratorImageName, int numberOfDataGenerators,
            String[] envVariables) {
        createGenerator(dataGeneratorImageName, numberOfDataGenerators, envVariables, dataGenContainerIds);
    }

    protected void createTaskGenerators(String taskGeneratorImageName, int numberOfTaskGenerators,
            String[] envVariables) {
        createGenerator(taskGeneratorImageName, numberOfTaskGenerators, envVariables, taskGenContainerIds);
    }

    private void createGenerator(String generatorImageName, int numberOfGenerators, String[] envVariables,
            Set<String> generatorIds) {
        String containerId;
        String variables[] = Arrays.copyOf(envVariables, envVariables.length);
        variables[variables.length - 2] = Constants.GENERATOR_COUNT_KEY + "=" + numberOfGenerators;
        for (int i = 0; i < numberOfGenerators; ++i) {
            variables[variables.length - 1] = Constants.GENERATOR_ID_KEY + "=" + i;
            containerId = createContainer(generatorImageName, variables);
            if (containerId != null) {
                generatorIds.add(containerId);
            } else {
                String errorMsg = "Couldn't create generator component. Aborting.";
                LOGGER.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
        }
    }

    protected void waitForComponentsToInitialize() {
        try {
            dataGenReadyMutex.acquire(dataGenContainerIds.size());
        } catch (InterruptedException e) {
            String errorMsg = "Interrupted while waiting for the data generators to be ready.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
        try {
            taskGenReadyMutex.acquire(taskGenContainerIds.size());
        } catch (InterruptedException e) {
            String errorMsg = "Interrupted while waiting for the task generators to be ready.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
        try {
            evalStoreReadyMutex.acquire();
        } catch (InterruptedException e) {
            String errorMsg = "Interrupted while waiting for the evaluation storage to be ready.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    protected void sendResultModel(Model model) {
        // TODO Auto-generated method stub

    }

    protected void waitForEvalComponentsToFinish() {
        // TODO Auto-generated method stub

    }

    protected Model receiveResultModel() {
        // TODO Auto-generated method stub
        return null;
    }

    protected void createEvaluationModule(String evalModuleImageName, String[] envVariables) {
        createContainer(evalModuleImageName, envVariables);
    }

    protected void waitForSystemToFinish() {
        try {
            systemTerminatedMutex.acquire();
        } catch (InterruptedException e) {
            String errorMsg = "Interrupted while waiting for the system to terminate.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    protected void waitForDataGenToFinish() {
        try {
            taskGenTerminatedMutex.acquire(taskGenContainerIds.size());
        } catch (InterruptedException e) {
            String errorMsg = "Interrupted while waiting for the task generators to terminate.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
        try {
            sendToCmdQueue(Commands.TASK_GENERATION_FINISHED);
        } catch (IOException e) {
            String errorMsg = "Couldn't send the " + Commands.TASK_GENERATION_FINISHED + " command. Aborting.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    protected void waitForTaskGenToFinish() {
        try {
            dataGenTerminatedMutex.acquire(dataGenContainerIds.size());
        } catch (InterruptedException e) {
            String errorMsg = "Interrupted while waiting for the data generators to terminate.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
        try {
            sendToCmdQueue(Commands.DATA_GENERATION_FINISHED);
        } catch (IOException e) {
            String errorMsg = "Couldn't send the " + Commands.DATA_GENERATION_FINISHED + " command. Aborting.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        switch (command) {
        case Commands.DATA_GENERATOR_READY_SIGNAL: {
            dataGenReadyMutex.release();
            break;
        }
        case Commands.TASK_GENERATOR_READY_SIGNAL: {
            taskGenReadyMutex.release();
            break;
        }
        case Commands.EVAL_STORAGE_READY_SIGNAL: {
            evalStoreReadyMutex.release();
            break;
        }
        case Commands.DOCKER_CONTAINER_TERMINATED: {
            String containerId = RabbitMQUtils.readString(data);
            if (dataGenContainerIds.contains(containerId)) {
                dataGenTerminatedMutex.release();
                break;
            }
            if (taskGenContainerIds.contains(containerId)) {
                taskGenTerminatedMutex.release();
                break;
            }
            if (containerId.equals(evalStoreContainerId)) {
                evalStoreTerminatedMutex.release();
            }
            if (containerId.equals(systemContainerId)) {
                systemTerminatedMutex.release();
            }
            break;
        }
        }
    }
}
