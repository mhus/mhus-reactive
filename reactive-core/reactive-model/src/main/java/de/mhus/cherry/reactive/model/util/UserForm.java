/**
 * Copyright 2018 Mike Hummel
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mhus.cherry.reactive.model.util;

import de.mhus.lib.core.definition.DefRoot;
import de.mhus.lib.core.definition.IDefDefinition;
import de.mhus.lib.errors.MException;

public class UserForm {

    DefRoot root = new DefRoot();

    public UserForm add(IDefDefinition... components) {
        root.addDefinition(components);
        return this;
    }

    @Override
    public String toString() {
        return root.toString();
    }

    public UserForm build() throws MException {
        root.build();
        return this;
    }

    public DefRoot getRoot() {
        return root;
    }
}
