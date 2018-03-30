/**
 * This file is part of cherry-reactive.
 *
 *     cherry-reactive is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     cherry-reactive is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with cherry-reactive.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.cherry.reactive.util.activity;

import de.mhus.cherry.reactive.model.activity.AActivity;
import de.mhus.cherry.reactive.model.activity.AGateway;
import de.mhus.cherry.reactive.model.annotations.Output;
import de.mhus.cherry.reactive.model.engine.PNode.STATE_NODE;

public abstract class RGateway<P extends RPool<?>> extends RActivity<P> implements AGateway<P> {

	@Override
	public void doExecuteActivity() throws Exception {
		Output[] next = doExecute();
		if (next != null) {
			for (Output output : next) {
				Class<? extends AActivity<?>> act = output.activity();
				try {
					getContext().createActivity(act);
				} catch (Throwable t) {
					log().w(act,t);
				}
			}
			getContext().getPNode().setState(STATE_NODE.CLOSED);
		}
	}

	public abstract Output[] doExecute()  throws Exception;

}
