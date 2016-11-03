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
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getCloseChallengeQuery("http://example.org/MyChallenge", FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/closedChallengeConfig.ttl", FIRST_GRAPH_NAME });
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/closedChallengeConfig.ttl",
                SparqlQueries.getCloseChallengeQuery("http://example.org/MyChallenge", FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/closedChallengeConfig.ttl", FIRST_GRAPH_NAME });
        testConfigs.add(new Object[] {
                "org/hobbit/storage/queries/exampleChallengeConfig.ttl", SparqlQueries
                        .getCloseChallengeQuery("http://example.org/MyChallenge", SECOND_GRAPH_NAME),
                null, SECOND_GRAPH_NAME });

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
