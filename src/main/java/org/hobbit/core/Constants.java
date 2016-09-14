package org.hobbit.core;

/**
 * This class defines constants of the hobbit platform.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public final class Constants {

    private Constants() {
    }

    // =============== ENVIRONMENT CONSTANTS ===============

    public static final String HOBBIT_SESSION_ID_KEY = "hobbit.session-id";

    public static final String HOBBIT_SESSION_ID_FOR_PLATFORM_COMPONENTS = "SYSTEM";

    public static final String RABBIT_MQ_HOST_NAME_KEY = "hobbit.rabbit-host";

    public static final String GENERATOR_ID_KEY = "hobbit.generator-id";

    public static final String GENERATOR_COUNT_KEY = "hobbit.generator-count";

    public static final String SYSTEM_CONTAINER_ID_KEY = "hobbit.system-container-id";

    // =============== RABBIT CONSTANTS ===============

    // /**
    // * Name of the hobbit command queue.
    // */
    // public static final String HOBBIT_COMMAND_QUEUE_NAME = "hobbit.command";

    /**
     * Name of the hobbit command exchange.
     */
    public static final String HOBBIT_COMMAND_EXCHANGE_NAME = "hobbit.command";

    /**
     * Name of the hobbit docker service queue.
     */
    public static final String DOCKER_SERVICE_QUEUE_NAME = "hobbit.docker-service";

    public static final String DATA_GEN_2_TASK_GEN_QUEUE_NAME = "hobbit.datagen-taskgen";

    public static final String DATA_GEN_2_SYSTEM_QUEUE_NAME = "hobbit.datagen-system";

    public static final String TASK_GEN_2_SYSTEM_QUEUE_NAME = "hobbit.taskgen-system";

    public static final String TASK_GEN_2_EVAL_STORAGE_QUEUE_NAME = "hobbit.taskgen-evalstore";

    public static final String EVAL_MODULE_2_EVAL_STORAGE_QUEUE_NAME = "hobbit.evalmod-evalstore";

    public static final String EVAL_STORAGE_2_EVAL_MODULE_QUEUE_NAME = "hobbit.evalstore-evalmod";

}
