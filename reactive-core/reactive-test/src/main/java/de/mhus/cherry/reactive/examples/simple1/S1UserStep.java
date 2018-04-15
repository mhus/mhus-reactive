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
package de.mhus.cherry.reactive.examples.simple1;

import de.mhus.cherry.reactive.model.annotations.ActivityDescription;
import de.mhus.cherry.reactive.model.annotations.Output;
import de.mhus.cherry.reactive.model.annotations.PropertyDescription;
import de.mhus.cherry.reactive.model.util.UserForm;
import de.mhus.cherry.reactive.util.bpmn2.RUserTask;
import de.mhus.lib.annotations.generic.Public;
import de.mhus.lib.core.M;
import de.mhus.lib.core.definition.DefAttribute;
import de.mhus.lib.errors.MException;
import de.mhus.lib.form.Item;
import de.mhus.lib.form.definition.FmCombobox;
import de.mhus.lib.form.definition.FmReadOnly;
import de.mhus.lib.form.definition.FmText;

@ActivityDescription(
		outputs = @Output(activity=S1TheEnd.class), 
		lane = S1Lane1.class
		)
public class S1UserStep extends RUserTask<S1Pool> {

	@PropertyDescription
	private String text3 = "text3";
	@PropertyDescription
	private String option = "1";
	@PropertyDescription(persistent=false)
	@Public(name="option.items")
	private Item[] optionOptions = new Item[] {
			new Item("1","One"),
			new Item("2","Two"),
	};
	@SuppressWarnings("unchecked")
	@Override
	public UserForm createForm() {
		return new UserForm().add(
			new DefAttribute("showInformation", true),
			new FmText(M.n(S1Pool::getText1), "Text1", "", new FmReadOnly()),
			new FmText(M.n(S1Pool::getText2), "Text2", ""),
			new FmText(M.n(S1UserStep::getText3), "Text3", ""),
			new FmCombobox("option", "Option", "Sample Option with options")
		);
	}

	@Override
	public String[] createIndexValues(boolean init) {
		return null;
	}

	@Override
	protected void doSubmit() throws MException {
		
	}

	public String getText3() {
		return text3;
	}

}