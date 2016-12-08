package org.hobbit.storage.queries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Constants;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This test class tests SPARQL CONSTRUCT queries.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
@RunWith(Parameterized.class)
public class ConstructQueryTest extends AbstractQueryTest {

    @Parameters
    public static Collection<Object[]> data() throws IOException {
        List<Object[]> testConfigs = new ArrayList<Object[]>();

        // Construct experiment graph
        testConfigs.add(new Object[] {
                "org/hobbit/storage/queries/exampleExperiment.ttl", SparqlQueries
                        .getExperimentGraphQuery("http://w3id.org/hobbit/experiments#LinkingExp10", FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/getExperimentResult.ttl" });
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleExperiment.ttl",
                SparqlQueries.getExperimentGraphQuery("http://w3id.org/hobbit/experiments#LinkingExp10", null),
                "org/hobbit/storage/queries/getExperimentResult.ttl" });
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleExperiment.ttl", SparqlQueries
                .getExperimentGraphQuery("http://w3id.org/hobbit/experiments#LinkingExp10", SECOND_GRAPH_NAME), null });

        // Construct experiment graph of challenge task
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleExperiment.ttl",
                SparqlQueries.getExperimentOfTaskQuery("http://w3id.org/hobbit/experiments#LinkingExp10",
                        "http://w3id.org/hobbit/challenges#OAEILinkingChallenge", FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/getExperimentResult.ttl" });

        // Construct challenge graph
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getChallengeGraphQuery("http://example.org/MyChallenge", FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/getChallengeResult.ttl" });
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getChallengeGraphQuery("http://example.org/MyChallenge", null),
                "org/hobbit/storage/queries/getChallengeResult.ttl" });
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getChallengeGraphQuery("http://example.org/MyChallenge", SECOND_GRAPH_NAME), null });

        // Construct experiment from challenge task
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getCreateExperimentFromTaskQuery(Constants.NEW_EXPERIMENT_URI,
                        "http://example.org/MyChallengeTask1", "http://example.org/SystemA", null),
                "org/hobbit/storage/queries/createExpFromTaskSystemA.ttl" });
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getCreateExperimentFromTaskQuery(Constants.NEW_EXPERIMENT_URI,
                        "http://example.org/MyChallengeTask1", "http://example.org/SystemC", null),
                null });
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/exampleChallengeConfig.ttl",
                SparqlQueries.getCreateExperimentFromTaskQuery(Constants.NEW_EXPERIMENT_URI,
                        "http://example.org/MyChallengeTask2", "http://example.org/SystemC", null),
                "org/hobbit/storage/queries/createExpFromTaskSystemC.ttl" });

        // Construct experiment from challenge task
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/getExperimentForSystems.ttl",
                SparqlQueries.getExperimentGraphOfSystemsQuery(
                        Arrays.asList("http://w3id.org/system#limesV1", "http://w3id.org/system#limesV2"),
                        FIRST_GRAPH_NAME),
                "org/hobbit/storage/queries/getExperimentForSystemsResults.ttl" });
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/getExperimentForSystems.ttl",
                SparqlQueries.getExperimentGraphOfSystemsQuery(
                        Arrays.asList("http://w3id.org/system#limesV1", "http://w3id.org/system#limesV2"),
                        SECOND_GRAPH_NAME),
                null });
        testConfigs.add(new Object[] { "org/hobbit/storage/queries/getExperimentForSystems.ttl",
                SparqlQueries.getExperimentGraphOfSystemsQuery(
                        Arrays.asList("http://w3id.org/system#DoesNotExistV1", "http://w3id.org/system#DoesNotExistV2"),
                        FIRST_GRAPH_NAME),
                null });

        return testConfigs;
    }

    public ConstructQueryTest(String storeContentResource, String query, String expectedResultResource) {
        super(storeContentResource, query, expectedResultResource);
    }

    @Override
    protected Model executeQuery(String query, Dataset storeContent) {
        QueryExecution qe = QueryExecutionFactory.create(query, storeContent);
        return qe.execConstruct();
    }

}
