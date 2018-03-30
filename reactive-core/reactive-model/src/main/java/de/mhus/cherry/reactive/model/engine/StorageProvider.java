/**
 * Copyright 2018 Mike Hummel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
package de.mhus.cherry.reactive.model.engine;

import java.io.IOException;
import java.util.UUID;

import de.mhus.lib.errors.NotFoundException;

/**
 * 
 * @author mikehummel
 *
 */
public interface StorageProvider {

	/**
	 * Save or update the case into storage.
	 * 
	 * @param caze
	 * @throws IOException 
	 */
	void saveCase(PCase caze) throws IOException;
	
	/**
	 * Load the case from storage'
	 * @param id
	 * @return The requested case
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	PCase loadCase(UUID id) throws IOException, NotFoundException;
	
	/**
	 * Delete the case and all flow data from storage.
	 * 
	 * @param id
	 * @throws IOException 
	 */
	void deleteCaseAndFlowNodes(UUID id) throws IOException;
	
	/**
	 * Save or update the flow node.
	 * @param flow
	 * @throws IOException 
	 */
	void saveFlowNode(PNode flow) throws IOException;
	
	/**
	 * Load the flow node.
	 * 
	 * @param id
	 * @return The requested node
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	PNode loadFlowNode(UUID id) throws IOException,NotFoundException;

	/**
	 * Load all cases with the specified state or all. 
	 * 
	 * The set is only used to
	 * iterate the result. Other functionality is not needed. You can
	 * use a open database handle until the end of the queue is reached.
	 * 
	 * @param state The state or null for all states.
	 * @return Set to iterate the results.
	 * @throws IOException 
	 */
	Result<PCaseInfo> getCases(PCase.STATE_CASE state) throws IOException;
	
	/**
	 * Load all flows for this case with the specified state or all. 
	 * 
	 * The set is only used to
	 * iterate the result. Other functionality is not needed. You can
	 * use a open database handle until the end of the queue is reached.
	 * 
	 * @param caseId The id of the case.
	 * @param state The state or null for all states.
	 * @return list
	 * @throws IOException 
	 */
	Result<PNodeInfo> getFlowNodes(UUID caseId, PNode.STATE_NODE state) throws IOException;
	
	/**
	 * Returns all flow nodes with the state and a scheduled time greater zero and
	 * lesser or equals 'scheduled'.
	 * 
	 * @param state The state or null
	 * @param scheduled
	 * @return List of results
	 * @throws IOException
	 */
	Result<PNodeInfo> getScheduledFlowNodes(PNode.STATE_NODE state, long scheduled) throws IOException;
	
	Result<PNodeInfo> getSignalFlowNodes(PNode.STATE_NODE state, String signal) throws IOException;
	
	Result<PNodeInfo> getMessageFlowNodes(UUID caseId, PNode.STATE_NODE state, String message) throws IOException;

	Result<PNodeInfo> searchFlowNodes(SearchCriterias criterias) throws IOException;
	
	/**
	 * Return new engine persistence status. If no engine status is stored return null.
	 * @return List of results
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	PEngine loadEngine() throws IOException, NotFoundException;
	
	void saveEngine(PEngine engine) throws IOException;

	PNodeInfo loadFlowNodeInfo(UUID nodeId) throws IOException;

	PCaseInfo loadCaseInfo(UUID caseId) throws IOException;

	Result<PCaseInfo> searchCases(SearchCriterias criterias) throws IOException;

	boolean setNodePriority(UUID nodeId, int priority) throws IOException;

	boolean setNodeScope(UUID nodeId, int scope) throws IOException;

	boolean setCasePriority(UUID caseId, int priority) throws IOException;

	boolean setCaseScope(UUID caseId, int scope) throws IOException;
	
}
