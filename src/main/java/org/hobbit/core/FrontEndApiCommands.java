package org.hobbit.core;

/**
 * Command bytes that can be sent to the controller. The response depends on the
 * used command.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class FrontEndApiCommands {

    /**
     * This command requests
     * <ul>
     * <li>the currently running experiment and its status</li>
     * <li>the list of experiments in the queue</li>
     * <li>the planned challenges and their experiments</li>
     * </ul>
     */
    public static final byte LIST_CURRENT_STATUS = 0;

    /**
     * Lists the benchmarks that are currently available.
     */
    public static final byte LIST_AVAILABLE_BENCHMARKS = 1;

    /**
     * This command requests the parameters of a benchmark and the systems that
     * can be benchmarked with it.
     */
    public static final byte GET_BENCHMARK_DETAILS = 2;

    /**
     * This command adds the given benchmark system combination to the queue.
     */
    public static final byte ADD_EXPERIMENT_CONFIGURATION = 3;

    /**
     * This command requests systems that have been uploaded by the given user.
     */
    public static final byte GET_SYSTEMS_OF_USER = 4;

    /**
     * This command closes the given challenge.
     */
    public static final byte CLOSE_CHALLENGE = 5;
}
