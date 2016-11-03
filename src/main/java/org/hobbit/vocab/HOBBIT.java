package org.hobbit.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Representation of the Hobbit vocabulary as Java objects.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class HOBBIT {

    protected static final String uri = "http://w3id.org/hobbit/vocab#";

    /**
     * returns the URI for this schema
     * 
     * @return the URI for this schema
     */
    public static String getURI() {
        return uri;
    }

    protected static final Resource resource(String local) {
        return ResourceFactory.createResource(uri + local);
    }

    protected static final Property property(String local) {
        return ResourceFactory.createProperty(uri, local);
    }

    public static final Resource API = resource("API");
    public static final Resource Benchmark = resource("Benchmark");
    public static final Resource Challenge = resource("Challenge");
    public static final Resource ChallengeTask = resource("ChallengeTask");
    public static final Resource ConfigurableParameter = resource("ConfigurableParameter");
    public static final Resource Error = resource("Error");
    public static final Resource Experiment = resource("Experiment");
    public static final Resource FeatureParameter = resource("FeatureParameter");
    public static final Resource Hardware = resource("Hardware");
    public static final Resource KPI = resource("KPI");
    public static final Resource Parameter = resource("Parameter");
    public static final Resource System = resource("System");
    public static final Resource SystemInstance = resource("SystemInstance");

    public static final Property defaultValue = property("defaultValue");
    public static final Property endTime = property("endTime");
    public static final Property executionDate = property("executionDate");
    public static final Property hasAPI = property("hasAPI");
    public static final Property hasParameter = property("hasParameter");
    public static final Property hobbitPlatformVersion = property("hobbitPlatformVersion");
    public static final Property imageName = property("imageName");
    public static final Property instanceOf = property("instanceOf");
    public static final Property involvesBenchmark = property("involvesBenchmark");
    public static final Property involvesSystemInstance = property("involvesSystemInstance");
    public static final Property isPartOf = property("isPartOf");
    public static final Property isTaskOf = property("isTaskOf");
    public static final Property measuresKPI = property("measuresKPI");
    public static final Property organizer = property("organizer");
    public static final Property publicationDate = property("publicationDate");
    public static final Property relevantForAnalysis = property("relevantForAnalysis");
    public static final Property startTime = property("startTime");
    public static final Property terminatedWithError = property("terminatedWithError");
    public static final Property version = property("version");
    public static final Property wasCarriedOutOn = property("wasCarriedOutOn");

}
