package org.hobbit.storage.queries;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.modify.request.QuadAcc;
import org.apache.jena.sparql.modify.request.UpdateDeleteInsert;
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
     * A construct query that retrieves the graph of a challenge.
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
     * @param experimentUri
     *            URI of the newly created experiment.
     * @param challengeTaskUri
     *            URI of the challenge task from which the experiment should be
     *            created. <code>null</code> works like a wildcard.
     * @param systemUri
     *            URI of the system of the experiment. <code>null</code> works
     *            like a wildcard.
     * @param graphUri
     *            URI of the graph the experiment is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     * @throws IllegalArgumentException
     *             if the given experimentUri is null
     */
    public static final String getCreateExperimentFromTaskQuery(String experimentUri, String challengeTaskUri,
            String systemUri, String graphUri) {
        if (experimentUri == null) {
            throw new IllegalArgumentException("The given experiment URI must not be null.");
        }
        return replacePlaceholders(
                CREATE_EXPERIMENT_FROM_CHALLENGE_TASK, new String[] { EXPERIMENT_PLACEHOLDER,
                        CHALLENGE_TASK_PLACEHOLDER, SYSTEM_PLACEHOLDER, GRAPH_PLACEHOLDER },
                new String[] { experimentUri, challengeTaskUri, systemUri, graphUri });
    }

    /**
     * An update query that deletes the graph of a challenge.
     */
    private static final String DELETE_CHALLENGE_GRAPH_QUERY = loadQuery(
            "org/hobbit/storage/queries/deleteChallenge.query");

    /**
     * Returns a SPARQL update query for deleting the graph of a challenge.
     * 
     * @param challengeUri
     *            URI of the challenge that should be retrieved.
     *            <code>null</code> works like a wildcard.
     * @param graphUri
     *            URI of the graph the challenge is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL update query that performs the deletion or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String deleteChallengeGraphQuery(String challengeUri, String graphUri) {
        return replacePlaceholders(DELETE_CHALLENGE_GRAPH_QUERY,
                new String[] { CHALLENGE_PLACEHOLDER, GRAPH_PLACEHOLDER }, new String[] { challengeUri, graphUri });
    }

    /**
     * Replaces the given placeholders in the given query with the given
     * replacements. If a replacement is <code>null</code>, it is replaced by a
     * variable.
     * 
     * @param query
     *            the query containing placeholders
     * @param placeholders
     *            the placeholders that should be replaced
     * @param replacements
     *            the replacements that should be used to replace the
     *            placeholders.
     * @return the newly created query or <code>null</code> if the given query
     *         was <code>null</code>.
     */
    private static final String replacePlaceholders(String query, String[] placeholders, String[] replacements) {
        if (query == null) {
            return null;
        }
        if (placeholders.length != replacements.length) {
            throw new IllegalArgumentException("The length of the placeholders != length of replacements.");
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

    /**
     * Generates a SPARQL UPDATE query based on the differences between the two
     * given models. Triples that are present in the original model but not in
     * the updated model will be put into the DELETE part of the query. Triples
     * that are present in the updated model but can not be found in the
     * original model will be put into the INSERT part of the query.
     * 
     * @param original
     *            the original RDF model
     * @param updated
     *            the updated RDF model
     * @param graphUri
     *            the URI of the graph to which the UPDATE query should be
     *            applied or <code>null</code>
     * @return The SPARQL UPDATE query
     */
    public static final String getUpdateQueryFromDiff(Model original, Model updated, String graphUri) {
        UpdateDeleteInsert update = new UpdateDeleteInsert();
        Node graph = null;
        if (graphUri != null) {
            graph = NodeFactory.createURI(graphUri);
            update.setWithIRI(graph);
        }
        StmtIterator iterator;

        // deleted statements
        Model temp = original.difference(updated);
        iterator = temp.listStatements();
        QuadAcc quads = update.getDeleteAcc();
        while (iterator.hasNext()) {
            quads.addTriple(iterator.next().asTriple());
        }

        // inserted statements
        temp = updated.difference(original);
        iterator = temp.listStatements();
        quads = update.getInsertAcc();
        while (iterator.hasNext()) {
            quads.addTriple(iterator.next().asTriple());
        }

        System.out.println(update.toString(original));
        return update.toString(original);
    }
}
