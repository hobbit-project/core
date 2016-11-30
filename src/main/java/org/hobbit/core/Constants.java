package org.hobbit.core;

import java.util.TimeZone;

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

    public static final String HOBBIT_SESSION_ID_KEY = "HOBBIT_SESSION_ID";

    public static final String RABBIT_MQ_HOST_NAME_KEY = "HOBBIT_RABBIT_HOST";

    public static final String GENERATOR_ID_KEY = "HOBBIT_GENERATOR_ID";

    public static final String GENERATOR_COUNT_KEY = "HOBBIT_GENERATOR_COUNT";

    public static final String SYSTEM_URI_KEY = "HOBBIT_SYSTEM_URI";
    
    public static final String CONTAINER_NAME_KEY = "HOBBIT_CONTAINER_NAME";
    @Deprecated
    public static final String SYSTEM_CONTAINER_ID_KEY = "HOBBIT_SYSTEM_CONTAINER_ID";

    public static final String BENCHMARK_PARAMETERS_MODEL_KEY = "BENCHMARK_PARAMETERS_MODEL";
    
    public static final String HOBBIT_EXPERIMENT_URI_KEY = "HOBBIT_EXPERIMENT_URI";

    // =============== RABBIT CONSTANTS ===============

    /**
     * Name of the hobbit command exchange.
     */
    public static final String HOBBIT_COMMAND_EXCHANGE_NAME = "hobbit.command";

    /**
     * Name of the hobbit docker service queue.
     */
    @Deprecated
    public static final String DOCKER_SERVICE_QUEUE_NAME = "hobbit.docker-service";

    public static final String DATA_GEN_2_TASK_GEN_QUEUE_NAME = "hobbit.datagen-taskgen";

    public static final String DATA_GEN_2_SYSTEM_QUEUE_NAME = "hobbit.datagen-system";

    public static final String TASK_GEN_2_SYSTEM_QUEUE_NAME = "hobbit.taskgen-system";

    public static final String TASK_GEN_2_EVAL_STORAGE_QUEUE_NAME = "hobbit.taskgen-evalstore";
    
    public static final String SYSTEM_2_EVAL_STORAGE_QUEUE_NAME = "hobbit.system-evalstore";

    public static final String EVAL_MODULE_2_EVAL_STORAGE_QUEUE_NAME = "hobbit.evalmod-evalstore";

	public static final String EVAL_STORAGE_2_EVAL_MODULE_QUEUE_NAME = "hobbit.evalstore-evalmod";

	public static final String FRONT_END_2_CONTROLLER_QUEUE_NAME = "hobbit.frontend-controller";

    public static final String CONTROLLER_2_FRONT_END_QUEUE_NAME = "hobbit.controller-frontend";

    public static final String CONTROLLER_2_ANALYSIS_QUEUE_NAME = "hobbit.controller-analysis";
	
	public static final String STORAGE_QUEUE_NAME = "hobbit.storage";
	
	// =============== GRAPH CONSTANTS ===============

    public static final String EXPERIMENT_URI_NS = "http://w3id.org/hobbit/experiments#";
	public static final String NEW_EXPERIMENT_URI = EXPERIMENT_URI_NS + "New";
	public static final String CHALLENGE_URI_NS = "http://w3id.org/hobbit/challenges#";
	public static final String PUBLIC_RESULT_GRAPH_URI = "http://hobbit.org/graphs/PublicResults";
    public static final String PRIVATE_RESULT_GRAPH_URI = "http://hobbit.org/graphs/PrivateResults";
    public static final String CHALLENGE_DEFINITION_GRAPH_URI = "http://hobbit.org/graphs/ChallengeDefinitions";

    // =============== OTHER CONSTANTS ===============

    public static final String HOBBIT_SESSION_ID_FOR_PLATFORM_COMPONENTS = "SYSTEM";

    public static final String HOBBIT_SESSION_ID_FOR_BROADCASTS = "BROADCAST";

    public static final String CONTAINER_TYPE_BENCHMARK = "BENCHMARK";

    public static final String CONTAINER_TYPE_SYSTEM = "SYSTEM";
    
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("GMT");
	
}
