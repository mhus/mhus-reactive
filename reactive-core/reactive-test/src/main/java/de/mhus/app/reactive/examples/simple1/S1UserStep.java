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
package de.mhus.app.reactive.examples.simple1;

import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.annotations.PropertyDescription;
import de.mhus.app.reactive.util.bpmn2.RUserTask;
import de.mhus.lib.annotations.generic.Public;
import de.mhus.lib.basics.consts.GenerateConst;
import de.mhus.lib.core.IProperties;
import de.mhus.lib.core.M;
import de.mhus.lib.core.MProperties;
import de.mhus.lib.core.definition.DefAttribute;
import de.mhus.lib.core.definition.DefRoot;
import de.mhus.lib.errors.MException;
import de.mhus.lib.form.Item;
import de.mhus.lib.form.definition.FaReadOnly;
import de.mhus.lib.form.definition.FmCombobox;
import de.mhus.lib.form.definition.FmText;

@ActivityDescription(outputs = @Output(activity = S1TheEnd.class), lane = S1Lane1.class)
@GenerateConst
public class S1UserStep extends RUserTask<S1Pool> {

    @PropertyDescription private String text3 = "text3";
    @PropertyDescription private String option = "1";

    @PropertyDescription(persistent = false)
    @Public(name = "option.items")
    private Item[] optionOptions =
            new Item[] {
                new Item("1", "One"), new Item("2", "Two"),
            };

    @Override
    public String[] createIndexValues(boolean init) {
        return null;
    }

    @Override
    protected void doSubmit() throws MException {}

    public String getText3() {
        return text3;
    }

    @Override
    public MProperties doAction(IProperties values, String action) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DefRoot getForm() {
        return new DefRoot(
                new DefAttribute("showInformation", true),
                new FmText(M.n(_S1Pool._TEXT1), "Text1", "", new FaReadOnly()),
                new FmText(M.n(_S1Pool._TEXT2), "Text2", ""),
                //	new FmText(M.n(S1UserStep_.FIELD_TEXT3), "Text3", ""),
                new FmCombobox("option", "Option", "Sample Option with options"));
    }
}
