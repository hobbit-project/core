package org.hobbit.core.components;

import org.apache.jena.rdf.model.Model;

public abstract class AbstractBenchmarkController extends AbstractComponent {

	protected Model resultModel;
	
	@Override
	public void init() throws Exception {
		super.init();
		// FIXME create Data Generators
		// FIXME create Task Generators
		// FIXME create Evaluation Storage
		// FIXME wait for all modules to finish their initialization
		// FIXME send ready signal
	}
	
	@Override
	public void run() throws Exception {
		// FIXME start data generation
		// FIXME wait for data generation to finish
		// FIXME send data generation finished signal
		// FIXME wait for task generation to finish
		// FIXME send task generation finished signal
		// FIXME wait for system adapter to finish
		// FIXME create evaluation module
		// FIXME wait to receive the result module from the evaluation module
		// FIXME wait for evaluation storage and module to terminate
		// FIXME send the resultModul to the platform controller and terminate
	}
}
