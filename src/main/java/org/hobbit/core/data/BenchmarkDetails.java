package org.hobbit.core.data;

import java.util.List;

import org.apache.jena.rdf.model.Model;

public class BenchmarkDetails {

    public Model benchmarkModel;
    public List<SystemMetaData> systems;

    public BenchmarkDetails(Model benchmarkModel, List<SystemMetaData> systems) {
        this.benchmarkModel = benchmarkModel;
        this.systems = systems;
    }

}
