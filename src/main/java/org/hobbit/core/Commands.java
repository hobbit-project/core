package org.hobbit.core;

public final class Commands {

	private Commands() {
	}

	public static final byte SYSTEM_READY_SIGNAL = 1;

	public static final byte BENCHMARK_READY_SIGNAL = 2;

	public static final byte DATA_GENERATOR_READY_SIGNAL = 3;

	public static final byte TASK_GENERATOR_READY_SIGNAL = 4;

	public static final byte EVAL_STORAGE_READY_SIGNAL = 5;

	public static final byte EVAL_MODULE_READY_SIGNAL = 6;

	public static final byte DATA_GENERATOR_START_SIGNAL = 7;

	public static final byte TASK_GENERATOR_START_SIGNAL = 8;
}
