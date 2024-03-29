/**
 * Copyright (C) 2018 Mike Hummel (mh@mhus.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mhus.app.reactive.karaf;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import de.mhus.app.reactive.engine.util.EngineUtil;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PCaseLock;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNodeInfo;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.osgi.ReactiveAdmin;
import de.mhus.lib.core.M;
import de.mhus.osgi.api.karaf.AbstractCmd;

@Command(
        scope = "reactive",
        name = "pcase-cancel",
        description = "Case modifications - cancel hard")
@Service
public class CmdCaseCancel extends AbstractCmd {

    @Argument(
            index = 0,
            name = "id",
            required = true,
            description = "case id or custom id",
            multiValued = true)
    String[] caseId;

    @Option(name = "-o", aliases = "--only", description = "Do not cancel nodes", required = false)
    private boolean only;

    @Override
    public Object execute2() throws Exception {

        ReactiveAdmin api = M.l(ReactiveAdmin.class);

        for (String id : caseId) {
            try (PCaseLock lock = EngineUtil.getCaseLock(api.getEngine(), id, "cmdcase.cancel")) {
                PCase caze = lock.getCase();
                System.out.println("Case: " + caze);
                lock.closeCase(true, -1, "cancelled by cmd");
                if (!only)
                    for (PNodeInfo info : api.getEngine().storageGetFlowNodes(caze.getId(), null)) {
                        if (info.getState() != STATE_NODE.CLOSED
                                && info.getState() != STATE_NODE.FAILED) {
                            System.out.println("Node: " + info);
                            try {
                                PNode pNode = lock.getFlowNode(info.getId());
                                pNode.setState(STATE_NODE.CLOSED);
                                lock.saveFlowNode(pNode);
                            } catch (Throwable t) {
                                System.out.println("Error in " + info);
                                t.printStackTrace();
                            }
                        }
                    }
            } catch (Throwable t) {
                System.out.println("Error in " + id);
                t.printStackTrace();
            }
        }

        return null;
    }
}
