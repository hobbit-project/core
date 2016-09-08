package org.hobbit.core;

public final class Commands {

    private Commands() {
    }

    public static final byte SYSTEM_READY_SIGNAL = 1;

    public static final byte BENCHMARK_READY_SIGNAL = 2;

    public static final byte DATA_GENERATOR_READY_SIGNAL = 3;

    public static final byte TASK_GENERATOR_READY_SIGNAL = 4;

    public static final byte EVAL_STORAGE_READY_SIGNAL = 5;

    public static final byte EVAL_MODULE_READY_SIGNAL = 6;

    public static final byte DATA_GENERATOR_START_SIGNAL = 7;

    public static final byte TASK_GENERATOR_START_SIGNAL = 8;

    public static final byte EVAL_MODULE_FINISHED_SIGNAL = 9;

    public static final byte EVAL_STORAGE_TERMINATE = 10;

    public static final byte BENCHMARK_FINISHED_SIGNAL = 11;
    /**
     * Command used to ask a docker managing component to start a certain
     * container.
     * <p>
     * The command is followed by a String containing the following JSON data:
     * <br>
     * <code>
     * {<br>"image": "image-to-run",<br> "type": "system|benchmark",<br> "parent":"parent-container-id"<br>}
     * </code>
     * </p>
     */
    public static final byte DOCKER_CONTAINER_START = 12;
    /**
     * Command used to ask a docker managing component to stop a certain
     * container.
     * <p>
     * The command is followed by a String containing the following JSON data:
     * <br>
     * <code>
     * {<br>"containerId": "container-to-stop"<br>}
     * </code>
     * </p>
     */
    public static final byte DOCKER_CONTAINER_STOP = 13;
}
