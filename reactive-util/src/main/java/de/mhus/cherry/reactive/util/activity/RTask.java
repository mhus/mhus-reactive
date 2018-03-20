package de.mhus.cherry.reactive.util.activity;

import de.mhus.cherry.reactive.model.activity.AActivity;
import de.mhus.cherry.reactive.model.activity.ATask;
import de.mhus.cherry.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.cherry.reactive.model.errors.EngineException;
import de.mhus.cherry.reactive.model.util.ActivityUtil;

public abstract class RTask<P extends RPool<?>> extends RActivity<P> implements ATask<P> {

	@Override
	public void doExecuteActivity() throws Exception {
		String nextName = doExecute();
		if (nextName != null) {
			Class<? extends AActivity<?>> next = ActivityUtil.getOutputByName(this, nextName);
			if (next == null)
				throw new EngineException("Output Activity not found: " + nextName + " in " + getClass().getCanonicalName());
			getContext().createActivity(next);
			getContext().getPNode().setState(STATE_NODE.CLOSED);
		}
	}

	public abstract String doExecute()  throws Exception;

}