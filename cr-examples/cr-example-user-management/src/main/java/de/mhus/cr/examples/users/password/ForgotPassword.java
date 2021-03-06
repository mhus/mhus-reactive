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
package de.mhus.cr.examples.users.password;

import java.util.Map;

import de.mhus.app.reactive.model.annotations.PoolDescription;
import de.mhus.app.reactive.model.annotations.PropertyDescription;
import de.mhus.app.reactive.util.bpmn2.RPool;
import de.mhus.cr.examples.users.ReadActor;

@PoolDescription(displayName = "Forgot Password", actorRead = ReadActor.class)
public class ForgotPassword extends RPool<ForgotPassword> {

    @PropertyDescription(displayName = "EMail", writable = false, initial = true)
    private String email;

    @Override
    protected void checkInputParameters(Map<String, Object> parameters) throws Exception {}

    @Override
    public String[] createIndexValues(boolean init) {
        return null;
    }

    public String getEmail() {
        return email;
    }
}
