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

import java.util.HashMap;
import java.util.UUID;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import de.mhus.app.reactive.engine.Engine;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.PNodeInfo;
import de.mhus.app.reactive.model.engine.Result;
import de.mhus.app.reactive.osgi.ReactiveAdmin;
import de.mhus.lib.core.M;
import de.mhus.lib.core.MCast;
import de.mhus.lib.core.console.ConsoleTable;
import de.mhus.osgi.api.karaf.AbstractCmd;

@Command(
        scope = "reactive",
        name = "pnode-executing",
        description = "Node modifications - print currently executing nodes")
@Service
public class CmdNodeExecuting extends AbstractCmd {

    @Option(
            name = "-u",
            aliases = "--upcoming",
            description = "Print upcoming tasks",
            required = false)
    private boolean upcoming;

    @Option(
            name = "-z",
            aliases = "--zombie",
            description = "Print zombie threads",
            required = false)
    private boolean zombi;

    @Option(
            name = "-st",
            aliases = "--stacktrace",
            description = "Print stack traces",
            required = false)
    private boolean stacktraces;

    @SuppressWarnings("deprecation")
    @Override
    public Object execute2() throws Exception {

        ReactiveAdmin api = M.l(ReactiveAdmin.class);
        if (!upcoming) {
            // get Threads
            HashMap<String, Thread> threadList = new HashMap<>();
            for (Thread thread : Thread.getAllStackTraces().keySet())
                if (thread.getName().startsWith("reactive-executor:")) {
                    String[] parts = thread.getName().split(" ", 3);
                    if (parts.length > 1) {
                        threadList.put(parts[1], thread);
                    }
                }
            ConsoleTable table = new ConsoleTable(tblOpt);
            table.setHeaderValues(
                    "Id", "Case", "Name", "Time", "State", "Type", "CaseId", "Thread");
            for (UUID nodeId : api.getEngine().getExecuting()) {
                PNode node = api.getEngine().getNodeWithoutLock(nodeId);
                PCase caze = api.getEngine().getCaseWithoutLock(node.getCaseId());
                String time = Util.toPeriod(System.currentTimeMillis() - node.getLastRunDate());
                Thread thread = threadList.remove(node.getId().toString());
                table.addRowValues(
                        node.getId(),
                        caze.getName(),
                        node.getName(),
                        time,
                        node.getState(),
                        node.getType(),
                        node.getCaseId(),
                        thread == null
                                ? ""
                                : thread.getId()
                                        + (stacktraces
                                                ? MCast.toString("", thread.getStackTrace())
                                                : ""));
            }
            table.print();

            if (zombi)
                threadList.forEach(
                        (k, v) -> {
                            System.out.println("Zombi Thread: " + v.getId() + " " + v.getName());
                            if (stacktraces)
                                System.out.println(MCast.toString("", v.getStackTrace()));
                        });
        }

        if (upcoming) {
            ConsoleTable table = new ConsoleTable(tblOpt);
            table.setHeaderValues("Id", "Case", "Name", "State", "Type", "CaseId", "Running");

            Engine engine = api.getEngine();
            long now = System.currentTimeMillis();
            Result<PNodeInfo> result =
                    engine.getStorage().getScheduledFlowNodes(STATE_NODE.RUNNING, now, true);
            int cnt = 0;
            for (PNodeInfo nodeInfo : result) {
                PCase caze = engine.getCaseWithoutLock(nodeInfo.getCaseId());
                PNode node = engine.getStorage().loadFlowNode(nodeInfo.getId());
                String status = cnt > 10 ? "waiting" : "ok";
                if (!engine.isNodeActive(nodeInfo)) {
                    status = "not active";
                } else {
                    if (!engine.isProcessHealthy(caze)) {
                        status = "not healthy";
                    } else cnt++;
                }
                table.addRowValues(
                        node.getId(),
                        caze.getName(),
                        node.getName(),
                        node.getState(),
                        node.getType(),
                        node.getCaseId(),
                        status);
            }
            table.print();
        }
        return null;
    }
}
