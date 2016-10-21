package org.hobbit.core.components;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.ArrayUtils;
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

    protected static final String DEFAULT_EVAL_STORAGE_IMAGE = "hobbit/evaluation_store";
    protected static final String[] DEFAULT_EVAL_STORAGE_PARAMETERS = new String[] { "HOBBIT_RIAK_NODES=1" };

    /**
     * The benchmark result as RDF model received from the evaluation module.
     */
    protected Model resultModel;
    /**
     * Mutex used to wait for the start signal from the controller.
     */
    protected Semaphore startBenchmarkMutex = new Semaphore(0);
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
     * The container id of the evaluation module.
     */
    private String evalModuleContainerId;
    /**
     * The container id of the benchmarked system.
     */
    private String systemContainerId = null;
    /**
     * The RDF model containing the benchmark parameters.
     */
    protected Model benchmarkParamModel;
    /**
     * The URI of the experiment.
     */
    protected String experimentUri;

    @Override
    public void init() throws Exception {
        super.init();
        // benchmark controllers should be able to accept broadcasts
        addCommandHeaderId(Constants.HOBBIT_SESSION_ID_FOR_BROADCASTS);

        Map<String, String> env = System.getenv();
        // Get the benchmark parameter model
        if (env.containsKey(Constants.BENCHMARK_PARAMETERS_MODEL_KEY)) {
            try {
                benchmarkParamModel = RabbitMQUtils.readModel(env.get(Constants.BENCHMARK_PARAMETERS_MODEL_KEY));
            } catch (Exception e) {
                LOGGER.error("Couldn't deserialize the given parameter model. Aborting.", e);
            }
        } else {
            String errorMsg = "Couldn't get the expected parameter model from the variable "
                    + Constants.BENCHMARK_PARAMETERS_MODEL_KEY + ". Aborting.";
            LOGGER.error(errorMsg);
            throw new Exception(errorMsg);
        }
        // Get the experiment URI
        if (env.containsKey(Constants.HOBBIT_EXPERIMENT_URI_KEY)) {
            experimentUri = env.get(Constants.HOBBIT_EXPERIMENT_URI_KEY);
        } else {
            String errorMsg = "Couldn't get the experiment URI from the variable " + Constants.HOBBIT_EXPERIMENT_URI_KEY
                    + ". Aborting.";
            LOGGER.error(errorMsg);
            throw new Exception(errorMsg);
        }
    }

    @Override
    public void run() throws Exception {
        sendToCmdQueue(Commands.BENCHMARK_READY_SIGNAL);
        // wait for the start signal
        startBenchmarkMutex.acquire();
        executeBenchmark();
    }

    protected abstract void executeBenchmark() throws Exception;

    /**
     * Creates the given number of data generators using the given image name
     * and environment variables.
     * 
     * @param dataGeneratorImageName
     * @param numberOfDataGenerators
     * @param envVariables
     */
    protected void createDataGenerators(String dataGeneratorImageName, int numberOfDataGenerators,
            String[] envVariables) {
        createGenerator(dataGeneratorImageName, numberOfDataGenerators, envVariables, dataGenContainerIds);
    }

    /**
     * Creates the given number of task generators using the given image name
     * and environment variables.
     * 
     * @param taskGeneratorImageName
     * @param numberOfTaskGenerators
     * @param envVariables
     */
    protected void createTaskGenerators(String taskGeneratorImageName, int numberOfTaskGenerators,
            String[] envVariables) {
        createGenerator(taskGeneratorImageName, numberOfTaskGenerators, envVariables, taskGenContainerIds);
    }

    /**
     * Internal method for creating generator components.
     * 
     * @param generatorImageName
     * @param numberOfGenerators
     * @param envVariables
     * @param generatorIds
     */
    private void createGenerator(String generatorImageName, int numberOfGenerators, String[] envVariables,
            Set<String> generatorIds) {
        String containerId;
        String variables[] = envVariables != null ? Arrays.copyOf(envVariables, envVariables.length + 2)
                : new String[2];
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

    /**
     * Creates the evaluate module using the given image name and environment
     * variables.
     * 
     * @param evalModuleImageName
     *            name of the evaluation module image
     * @param envVariables
     *            environment variables that should be given to the module
     */
    protected void createEvaluationModule(String evalModuleImageName, String[] envVariables) {
        envVariables = ArrayUtils.add(envVariables, Constants.HOBBIT_EXPERIMENT_URI_KEY + "=" + experimentUri);
        evalModuleContainerId = createContainer(evalModuleImageName, envVariables);
        if (evalModuleContainerId == null) {
            String errorMsg = "Couldn't create evaluation module. Aborting.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }

    /**
     * Creates the default evaluation storage using the given image name and
     * environment variables.
     */
    protected void createEvaluationStorage() {
        String[] envVariables = ArrayUtils.add(DEFAULT_EVAL_STORAGE_PARAMETERS,
                Constants.RABBIT_MQ_HOST_NAME_KEY + "=" + this.rabbitMQHostName);
        createEvaluationStorage(DEFAULT_EVAL_STORAGE_IMAGE, envVariables);
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
    protected void createEvaluationStorage(String evalStorageImageName, String[] envVariables) {
        evalStoreContainerId = createContainer(evalStorageImageName, envVariables);
        if (evalStoreContainerId == null) {
            String errorMsg = "Couldn't create evaluation storage. Aborting.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }

    /**
     * This method waits for the data generators, task generators and evaluation
     * storage to send their ready signals.
     */
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

    /**
     * Waits for the termination of all data generators.
     */
    protected void waitForDataGenToFinish() {
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

    /**
     * Waits for the termination of all task generators.
     */
    protected void waitForTaskGenToFinish() {
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

    /**
     * This method waits for the benchmarked system to terminate.
     */
    protected void waitForSystemToFinish() {
        try {
            systemTerminatedMutex.acquire();
        } catch (InterruptedException e) {
            String errorMsg = "Interrupted while waiting for the system to terminate.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * Waits for the termination of the evaluation module and the evaluation
     * storage.
     */
    protected void waitForEvalComponentsToFinish() {
        try {
            evalModuleTerminatedMutex.acquire();
        } catch (InterruptedException e) {
            String errorMsg = "Interrupted while waiting for the evaluation module to terminate.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
        try {
            evalStoreTerminatedMutex.acquire();
        } catch (InterruptedException e) {
            String errorMsg = "Interrupted while waiting for the evaluation storage to terminate.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * Sends the result RDF model to the platform controller.
     * 
     * @param model
     *            model containing the results
     */
    protected void sendResultModel(Model model) {
        try {
            sendToCmdQueue(Commands.BENCHMARK_FINISHED_SIGNAL, RabbitMQUtils.writeModel(model));
        } catch (IOException e) {
            String errorMsg = "Exception while trying to send the result to the platform controller.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        switch (command) {
        case Commands.START_BENCHMARK_SIGNAL: {
            startBenchmarkMutex.release();
            systemContainerId = RabbitMQUtils.readString(data);
            break;
        }
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
            // FIXME Add exit code check
            String containerId = RabbitMQUtils.readString(data);
            if (dataGenContainerIds.contains(containerId)) {
                dataGenTerminatedMutex.release();
            } else if (taskGenContainerIds.contains(containerId)) {
                taskGenTerminatedMutex.release();
            } else if (containerId.equals(evalStoreContainerId)) {
                evalStoreTerminatedMutex.release();
            } else if (containerId.equals(systemContainerId)) {
                systemTerminatedMutex.release();
            } else if (containerId.equals(evalModuleContainerId)) {
                evalModuleTerminatedMutex.release();
                try {
                    sendToCmdQueue(Commands.EVAL_STORAGE_TERMINATE);
                } catch (IOException e) {
                    LOGGER.error("Couldn't send the " + Commands.EVAL_STORAGE_TERMINATE
                            + " command. Won't wait for the evaluation store to terminate!", e);
                    evalStoreTerminatedMutex.release();
                }
            }
            break;
        }
        case Commands.EVAL_MODULE_FINISHED_SIGNAL: {
            resultModel = RabbitMQUtils.readModel(data);
            LOGGER.info("model size = " + resultModel.size());
        }
        }
    }
}
