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

import de.mhus.app.reactive.model.engine.EngineConst;
import de.mhus.app.reactive.osgi.ReactiveAdmin;
import de.mhus.lib.core.IProperties;
import de.mhus.lib.core.M;
import de.mhus.lib.core.MProperties;
import de.mhus.osgi.api.karaf.AbstractCmd;

@Command(scope = "reactive", name = "pstart", description = "Start case")
@Service
public class CmdProcessStart extends AbstractCmd {

    @Argument(
            index = 0,
            name = "uri",
            required = true,
            description = "Process uri",
            multiValued = false)
    String uri;

    @Argument(
            index = 1,
            name = "parameters",
            required = false,
            description = "Parameters",
            multiValued = true)
    String[] parameters;

    @Override
    public Object execute2() throws Exception {

        if (uri.startsWith(EngineConst.SCHEME_REACTIVE + ":")) {
            MProperties properties = IProperties.explodeToMProperties(parameters);

            ReactiveAdmin api = M.l(ReactiveAdmin.class);
            api.getEngine().start(uri, properties);
        } else {
            System.out.println("Unknown schema: " + uri);
        }
        return null;
    }
}
