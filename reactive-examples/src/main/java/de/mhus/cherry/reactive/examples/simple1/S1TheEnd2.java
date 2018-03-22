package de.mhus.cherry.reactive.examples.simple1;

import de.mhus.cherry.reactive.model.annotations.ActivityDescription;
import de.mhus.cherry.reactive.util.activity.REndPoint;
import de.mhus.cherry.reactive.util.activity.RTerminate;

@ActivityDescription(lane = S1Lane1.class, outputs = {})
public class S1TheEnd2 extends RTerminate<S1Pool> {

	@Override
	protected int getExitCode() {
		return 2;
	}

	@Override
	protected String getExitMessage() {
		return "end2";
	}

}
