package org.hobbit.storage.queries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This test class tests SPARQL UPDATE queries.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
@RunWith(Parameterized.class)
public class UpdateQueryTest extends AbstractQueryTest {

    @Parameters
    public static Collection<Object[]> data() throws IOException {
        List<Object[]> testConfigs = new ArrayList<Object[]>();

        // Close challenge update
        /*
         * A normal challenge is closed after the update is executed.
         */
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getCloseChallengeQuery("http://example.org/MyChallenge", FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/closedChallengeConfig.ttl", FIRST_GRAPH_NAME });
        /*
         * An already closed challenge stays closed.
         */
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/closedChallengeConfig.ttl",
                SparqlQueries.getCloseChallengeQuery("http://example.org/MyChallenge", FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/closedChallengeConfig.ttl", FIRST_GRAPH_NAME });
        /*
         * An empty graph is not changed.
         */
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getCloseChallengeQuery("http://example.org/MyChallenge", SECOND_GRAPH_NAME), null,
                SECOND_GRAPH_NAME });

        // Check the model diff based SPARQL UPDATE query creation
        Model original, updated;
        original = loadModel("org/hobbit/storage/queries/exampleChallengeConfig.ttl");
        updated = loadModel("org/hobbit/storage/queries/changedChallengeConfig.ttl");
        /*
         * The original model is changed to the updated model as expected.
         */
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getUpdateQueryFromDiff(original, updated, FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/changedChallengeConfig.ttl", FIRST_GRAPH_NAME });
        /*
         * A query that should focus on the second graph does not change the
         * first graph
         */
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getUpdateQueryFromDiff(original, updated, SECOND_GRAPH_NAME),
                "org/hobbit/storage/queries/exampleChallengeConfig.ttl", FIRST_GRAPH_NAME });
        /*
         * A query that should DELETE and INSERT something does not change an
         * empty graph.
         */
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getUpdateQueryFromDiff(original, updated, SECOND_GRAPH_NAME), null, SECOND_GRAPH_NAME });
        /*
         * The difference between an open and a closed challenge can be
         * expressed with this method as well.
         */
        updated = loadModel("org/hobbit/storage/queries/closedChallengeConfig.ttl");
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getUpdateQueryFromDiff(original, updated, FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/closedChallengeConfig.ttl", FIRST_GRAPH_NAME });

        // Delete challenge
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/deleteChallengeExample.ttl",
                SparqlQueries.deleteChallengeGraphQuery("http://example.org/MyChallenge", FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/cleanUpChallengeExample1.ttl", FIRST_GRAPH_NAME });

        // Check the clean up challenge config query
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/cleanUpChallengeExample1.ttl",
                SparqlQueries.cleanUpChallengeGraphQuery(FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/cleanUpChallengeExample1Result.ttl", FIRST_GRAPH_NAME });
        // Check the clean up challenge config query with an already clean
        // config graph
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/cleanUpChallengeExample2.ttl",
                SparqlQueries.cleanUpChallengeGraphQuery(FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/cleanUpChallengeExample2.ttl", FIRST_GRAPH_NAME });
        return testConfigs;
    }

    private String graphUri;

    public UpdateQueryTest(String storeContentResource, String query, String expectedResultResource, String graphUri) {
        super(storeContentResource, query, expectedResultResource);
        this.graphUri = graphUri;
    }

    @Override
    protected Model executeQuery(String query, Dataset storeContent) {
        UpdateRequest update = UpdateFactory.create(query);
        UpdateProcessor up = UpdateExecutionFactory.create(update, storeContent);
        up.execute();
        DatasetGraph dg = up.getDatasetGraph();
        return ModelFactory.createModelForGraph(dg.getGraph(NodeFactory.createURI(graphUri)));
    }

}
