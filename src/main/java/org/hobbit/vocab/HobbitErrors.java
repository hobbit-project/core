package org.hobbit.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Resources representing Hobbit errors.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class HobbitErrors {

    protected static final String uri = "http://w3id.org/hobbit/error#";

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

    public static final Resource BenchmarkCrashed = resource("BenchmarkCrashed");
    public static final Resource BenchmarkImageMissing = resource("BenchmarkImageMissing");
    public static final Resource BenchmarkCreationError = resource("BenchmarkCreationError");
    public static final Resource ExperimentTookTooMuchTime = resource("ExperimentTookTooMuchTime");
    public static final Resource SystemCrashed = resource("SystemCrashed");
    public static final Resource SystemImageMissing = resource("SystemImageMissing");
    public static final Resource SystemCreationError = resource("SystemCreationError");
    public static final Resource UnexpectedError = resource("UnexpectedError");
}
