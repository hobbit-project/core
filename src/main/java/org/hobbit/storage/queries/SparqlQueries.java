package org.hobbit.storage.queries;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides static SPRAQL queries that are loaded from predefined
 * resources.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class SparqlQueries {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparqlQueries.class);

    private static final String CHALLENGE_PLACEHOLDER = "%CHALLENGE_URI%";
    private static final String CHALLENGE_TASK_PLACEHOLDER = "%CHALLENGE_TASK_URI%";
    private static final String EXPERIMENT_PLACEHOLDER = "%EXPERIMENT_URI%";
    private static final String GRAPH_PLACEHOLDER = "%GRAPH_URI%";
    private static final String SYSTEM_PLACEHOLDER = "%SYSTEM_URI%";

    /**
     * Loads the given resource, e.g., a SPARQL query, as String.
     * 
     * @param resourceName
     *            name of the resource that should be loaded
     * @return the resource as String or <code>null</code> if an error occurs
     */
    private static final String loadQuery(String resourceName) {
        InputStream is = SparqlQueries.class.getClassLoader().getResourceAsStream(resourceName);
        if (is != null) {
            try {
                return IOUtils.toString(is);
            } catch (IOException e) {
                LOGGER.error("Couldn't read query from resource \"" + resourceName + "\". Returning null.");
            } finally {
                IOUtils.closeQuietly(is);
            }
        } else {
            LOGGER.error("Couldn't find needed resource \"" + resourceName + "\". Returning null.");
        }
        return null;
    }

    /**
     * An update query that closes a challenge.
     */
    private static final String GET_CHALLENGE_GRAPH_QUERY = loadQuery("org/hobbit/storage/queries/getChallenge.query");

    /**
     * Returns a SPARQL query for retrieving the graph of a challenge.
     * 
     * @param challengeUri
     *            URI of the challenge that should be retrieved.
     *            <code>null</code> works like a wildcard.
     * @param graphUri
     *            URI of the graph the challenge is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getChallengeGraphQuery(String challengeUri, String graphUri) {
        return replacePlaceholders(GET_CHALLENGE_GRAPH_QUERY, new String[] { CHALLENGE_PLACEHOLDER, GRAPH_PLACEHOLDER },
                new String[] { challengeUri, graphUri });
    }

    /**
     * An update query that closes a challenge.
     */
    private static final String CLOSE_CHALLENGE_UPDATE_QUERY = loadQuery(
            "org/hobbit/storage/queries/closeChallenge.query");

    /**
     * Returns a SPARQL query for adding the closed triple to the challenge with
     * the given URI.
     * 
     * @param challengeUri
     *            URI of the challenge that should be closed. <code>null</code>
     *            works like a wildcard.
     * @param graphUri
     *            URI of the graph the challenge is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL update query that performs the insertion or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getCloseChallengeQuery(String challengeUri, String graphUri) {
        return replacePlaceholders(CLOSE_CHALLENGE_UPDATE_QUERY,
                new String[] { CHALLENGE_PLACEHOLDER, GRAPH_PLACEHOLDER }, new String[] { challengeUri, graphUri });
    }

    /**
     * A construct query that selects the graph of an experiment.
     */
    private static final String GET_EXPERIMENT_QUERY = loadQuery("org/hobbit/storage/queries/getExperiment.query");

    /**
     * Returns a SPARQL query for retrieving the graph of an experiment.
     * 
     * @param experimentUri
     *            URI of the experiment that should be retrieved.
     *            <code>null</code> works like a wildcard.
     * @param graphUri
     *            URI of the graph the experiment is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getExperimentGraphQuery(String experimentUri, String graphUri) {
        return replacePlaceholders(GET_EXPERIMENT_QUERY, new String[] { EXPERIMENT_PLACEHOLDER, GRAPH_PLACEHOLDER },
                new String[] { experimentUri, graphUri });
    }

    /**
     * A construct query that selects the subgraph of a challenge task that
     * defines an experiment of this task.
     */
    private static final String CREATE_EXPERIMENT_FROM_CHALLENGE_TASK = loadQuery(
            "org/hobbit/storage/queries/createExperimentFromTask.query");

    /**
     * Returns a SPARQL query for creating a subgraph of a challenge task that
     * defines an experiment of this task.
     * 
     * @param systemUri
     *            URI of the challenge that should be closed. <code>null</code>
     *            works like a wildcard.
     * @param experimentUri
     *            URI of the newly created experiment.
     * @param graphUri
     *            URI of the graph the experiment is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getCreateExperimentFromTaskQuery(String experimentUri, String challengeTaskUri,
            String systemUri, String graphUri) {
        return replacePlaceholders(
                CREATE_EXPERIMENT_FROM_CHALLENGE_TASK, new String[] { EXPERIMENT_PLACEHOLDER,
                        CHALLENGE_TASK_PLACEHOLDER, SYSTEM_PLACEHOLDER, GRAPH_PLACEHOLDER },
                new String[] { experimentUri, challengeTaskUri, systemUri, graphUri });
    }

    private static final String replacePlaceholders(String query, String[] placeholders, String[] replacements) {
        if (query == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < replacements.length; ++i) {
            if (replacements[i] == null) {
                // create a variable name
                builder.append("?v");
                builder.append(i);
            } else {
                // create <URI>
                builder.append('<');
                builder.append(replacements[i]);
                builder.append('>');
            }
            replacements[i] = builder.toString();
            builder.delete(0, builder.length());
        }
        return StringUtils.replaceEachRepeatedly(query, placeholders, replacements);
    }
}
