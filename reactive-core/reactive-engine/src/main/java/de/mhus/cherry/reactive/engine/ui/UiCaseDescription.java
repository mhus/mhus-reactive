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
package de.mhus.cherry.reactive.engine.ui;

import de.mhus.cherry.reactive.model.ui.ICaseDescription;
import de.mhus.cherry.reactive.model.ui.IProcess;
import de.mhus.lib.core.MLog;
import de.mhus.lib.errors.MException;

public class UiCaseDescription extends MLog implements ICaseDescription {

	private IProcess process;
	private String uri;

	public UiCaseDescription(UiEngine ui, String uri) {
		this.uri = uri;
		try {
			process = ui.getProcess(uri);
		} catch (MException e) {
			log().d(uri,e);
		}
	}

	@Override
	public String getDisplayName() {
		return process.getDisplayName(uri, null);
	}

	@Override
	public String getDescription() {
		return process.getDescription(uri, null);
	}

	@Override
	public String getPropertyName(String property) {
		return process.getPropertyName(uri, null, property);
	}

}
