package de.mhus.cherry.reactive.model.engine;

import de.mhus.lib.core.MTimeInterval;

public class EngineConst {

	public static final String SCHEME_REACTIVE = "reactive";
	public static final String OPTION_CUSTOM_ID = "customId";
	public static long DEFAULT_ACTIVITY_TIMEOUT = MTimeInterval.MINUTE_IN_MILLISECOUNDS * 5;
	public static final int TRY_COUNT = 3;
	public static final String ENGINE_EXECUTE_PARALLEL = "engine.execute.parallel";
	public static final String ENGINE_EXECUTE_MAX_THREADS = "engine.execute.max.threads";
	public static final int DEFAULT_TRY_COUNT = 1;

}