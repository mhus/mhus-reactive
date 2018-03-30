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
package de.mhus.cherry.reactive.osgi;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;

import de.mhus.cherry.reactive.engine.Engine;
import de.mhus.cherry.reactive.engine.util.PoolValidator.Finding;
import de.mhus.cherry.reactive.model.engine.PEngine;
import de.mhus.cherry.reactive.model.engine.ProcessLoader;
import de.mhus.lib.errors.MException;

public interface ReactiveAdmin {

	enum STATE_ENGINE {STOPPED,SUSPENDED,RUNNING}
	
	void startEngine();

	void stopEngine();

	Collection<String> getAvailableProcesses();

	boolean addProcess(String name, ProcessLoader loader);

	void removeProcess(String name);

	List<Finding> deploy(String name, boolean addVersion, boolean activate) throws MException;

	void undeploy(String name) throws MException;

	String getProcessDeployName(String name);

	Engine getEngine();

	PEngine getEnginePersistence();

	void setExecutionSuspended(boolean suspend);

	STATE_ENGINE getEngineState();

	String getProcessInfo(String name);

	String addProcess(String[] fileNames, boolean remember) throws FileNotFoundException;

}
