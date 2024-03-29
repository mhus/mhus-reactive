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
import org.apache.karaf.shell.api.action.lifecycle.Service;

import de.mhus.app.reactive.engine.util.JavaPackageProcessProvider;
import de.mhus.app.reactive.model.activity.ACondition;
import de.mhus.app.reactive.model.activity.AEndPoint;
import de.mhus.app.reactive.model.activity.AEvent;
import de.mhus.app.reactive.model.activity.AExclusiveGateway;
import de.mhus.app.reactive.model.activity.AGateway;
import de.mhus.app.reactive.model.activity.AParallelGateway;
import de.mhus.app.reactive.model.activity.APoint;
import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.activity.AProcess;
import de.mhus.app.reactive.model.activity.AServiceTask;
import de.mhus.app.reactive.model.activity.AStartPoint;
import de.mhus.app.reactive.model.activity.ASwimlane;
import de.mhus.app.reactive.model.activity.ATask;
import de.mhus.app.reactive.model.activity.AUserTask;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.annotations.Trigger;
import de.mhus.app.reactive.model.engine.EElement;
import de.mhus.app.reactive.model.engine.EPool;
import de.mhus.app.reactive.model.engine.EProcess;
import de.mhus.app.reactive.model.engine.ProcessLoader;
import de.mhus.app.reactive.model.util.NoPool;
import de.mhus.app.reactive.osgi.ReactiveAdmin;
import de.mhus.lib.core.M;
import de.mhus.lib.core.MString;
import de.mhus.lib.core.console.ConsoleTable;
import de.mhus.lib.core.util.MUri;
import de.mhus.lib.errors.MException;
import de.mhus.lib.errors.NotFoundException;
import de.mhus.osgi.api.karaf.AbstractCmd;

@Command(
        scope = "reactive",
        name = "pinspect-elements",
        description = "Elements of deployed processes")
@Service
public class CmdInspectElements extends AbstractCmd {

    @Argument(index = 0, name = "Pool", required = false, description = "Pool", multiValued = false)
    String name;

    @Override
    public Object execute2() throws Exception {

        MUri uri = MUri.toUri(name);
        EProcess process = findProcess(name);
        EPool pool = getPool(process, uri);

        ConsoleTable table = new ConsoleTable(tblOpt);
        table.setHeaderValues("Type", "Name", "Canonical Name", "Swimlane", "Outputs", "Trigger");
        for (String name : pool.getElementNames()) {
            EElement element = pool.getElement(name);
            StringBuilder out = new StringBuilder();
            for (Output output : element.getOutputs()) {
                if (out.length() > 0) out.append("\n");
                out.append(output.name() + ":" + output.activity().getCanonicalName());
            }
            StringBuilder trig = new StringBuilder();
            for (Trigger trigger : element.getTriggers()) {
                if (trig.length() > 0) trig.append("\n");
                trig.append(
                        (trigger.abort() ? "" : "+")
                                + trigger.type()
                                + ":"
                                + trigger.activity().getCanonicalName());
            }
            String type = "";
            if (element.is(APool.class)) type += "Pool\n";
            if (element.is(AEvent.class)) type += "Event\n";
            if (element.is(ASwimlane.class)) type += "Swimlane\n";
            if (element.is(AProcess.class)) type += "Process\n";
            if (element.is(ACondition.class)) type += "Condition\n";

            if (element.is(AUserTask.class)) type += "UserTask\n";
            else if (element.is(AServiceTask.class)) type += "ServiceTask\n";
            else if (element.is(ATask.class)) type += "Task\n";

            if (element.is(AExclusiveGateway.class)) type += "ExclusiveGateway\n";
            else if (element.is(AParallelGateway.class)) type += "ParallelGateway\n";
            else if (element.is(AGateway.class)) type += "Gateway\n";

            if (element.is(AStartPoint.class)) type += "StartPoint\n";
            else if (element.is(AEndPoint.class)) type += "EndPoint\n";
            else if (element.is(APoint.class)) type += "Point\n";

            type = type.trim();
            if (type.length() == 0) type = "Element";

            table.addRowValues(
                    type,
                    element.getName(),
                    element.getCanonicalName(),
                    element.getSwimlane() == null
                            ? "none"
                            : element.getSwimlane().getCanonicalName(),
                    out,
                    trig);
        }
        table.print(System.out);

        return null;
    }

    private EProcess findProcess(String string) throws MException {
        if (!string.startsWith("bpm://")) string = "bpm://" + string;
        ReactiveAdmin api = M.l(ReactiveAdmin.class);
        EProcess process = null;
        MUri uri = MUri.toUri(string);
        try {
            process = api.getEngine().getProcess(uri);
        } catch (Throwable t) {
            System.out.println("Deployed process not found: " + t);
        }
        if (process == null) {
            ProcessLoader loader = api.getProcessLoader(uri.getLocation());
            JavaPackageProcessProvider provider = new JavaPackageProcessProvider();
            provider.addProcess(loader, MString.beforeLastIndex(uri.getLocation(), '.'));
            process = provider.getProcess(uri.getLocation());
        }
        return process;
    }

    public EPool getPool(EProcess process, MUri uri) throws NotFoundException {
        String poolName = uri.getPath();
        if (MString.isEmpty(poolName)) {
            poolName = process.getProcessDescription().defaultPool().getCanonicalName();
            if (poolName.equals(NoPool.class.getCanonicalName())) poolName = null;
        }
        if (MString.isEmpty(poolName))
            throw new NotFoundException("default pool not found for process", uri);

        EPool pool = process.getPool(poolName);
        return pool;
    }
}
