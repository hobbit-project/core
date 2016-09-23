package org.hobbit.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

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

    public static final Resource Benchmark = resource("Benchmark");
    public static final Resource BenchmarkParameter = resource("BenchmarkParameter");

    public static final Resource A2KB = resource("A2KB");
    public static final Resource C2KB = resource("C2KB");
    public static final Resource D2KB = resource("D2KB");
    public static final Resource Rc2KB = resource("Rc2KB");
    public static final Resource Sa2KB = resource("Sa2KB");
    public static final Resource Sc2KB = resource("Sc2KB");
    public static final Resource OKE2015_Task1 = resource("OKE2015_Task1");
    public static final Resource OKE2015_Task2 = resource("OKE2015_Task2");
    public static final Resource ERec = resource("ERec");
    public static final Resource ETyping = resource("ETyping");

    public static final Resource StrongAnnoMatch = resource("StrongAnnoMatch");
    public static final Resource WeakAnnoMatch = resource("WeakAnnoMatch");
    public static final Resource StrongEntityMatch = resource("StrongEntityMatch");

    public static final Property hasNumberOfDocuments = property("hasNumberOfDocuments");
    public static final Property hasExperimentType = property("hasExperimentType");
    public static final Property hasImageName = property("hasImageName");
    public static final Property involvesBenchmark = property("involvesBenchmark");
    public static final Property involvesSystem = property("involvesSystem");
    
}
