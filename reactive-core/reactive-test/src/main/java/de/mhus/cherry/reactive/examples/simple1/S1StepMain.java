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

import java.util.Date;

import de.mhus.cherry.reactive.examples.simple1.events.S1EventExternal;
import de.mhus.cherry.reactive.examples.simple1.events.S1EventMessage;
import de.mhus.cherry.reactive.examples.simple1.events.S1EventSignal;
import de.mhus.cherry.reactive.examples.simple1.exclusive.S1GatewayExclusive;
import de.mhus.cherry.reactive.examples.simple1.forms.S1UserForm01;
import de.mhus.cherry.reactive.examples.simple1.parallel.S1GatewayParallel1;
import de.mhus.cherry.reactive.examples.simple1.parallel.S1GatewayParallel2;
import de.mhus.cherry.reactive.examples.simple1.trigger.S1StepTrigger;
import de.mhus.cherry.reactive.examples.simple1.trigger.S1StepTriggerTimer;
import de.mhus.cherry.reactive.model.annotations.ActivityDescription;
import de.mhus.cherry.reactive.model.annotations.Output;
import de.mhus.cherry.reactive.model.annotations.Trigger;
import de.mhus.cherry.reactive.model.annotations.Trigger.TYPE;
import de.mhus.cherry.reactive.model.errors.TaskException;
import de.mhus.cherry.reactive.model.util.IndexValuesProvider;
import de.mhus.cherry.reactive.util.bpmn2.RServiceTask;
import de.mhus.lib.core.MDate;

@ActivityDescription(
		displayName="Main Cross Road",
		description="The main node to fork for all test cases",
		outputs = {
				@Output(name="step2",activity=S1Step2.class),
				@Output(name="step3",activity=S1Step3.class),
				@Output(name="user",activity=S1UserStep.class),
				@Output(name="external",activity=S1EventExternal.class),
				@Output(name="message",activity=S1EventMessage.class),
				@Output(name="signal",activity=S1EventSignal.class),
				@Output(name="exclusive",activity=S1GatewayExclusive.class),
				@Output(name="trigger",activity=S1StepTrigger.class),
				@Output(name="triggertimer",activity=S1StepTriggerTimer.class),
				@Output(name="parallel1",activity=S1GatewayParallel1.class),
				@Output(name="parallel2",activity=S1GatewayParallel2.class),
				@Output(name="form01",activity=S1UserForm01.class)
				},
		lane = S1Lane1.class,
		triggers = {
				@Trigger(type=TYPE.DEFAULT_ERROR,activity=S1TheEnd.class),
				@Trigger(type=TYPE.ERROR,activity=S1TheEnd.class, name="error1")
		},
		indexDisplayNames = {
				"Name",
				"Executed",
				"Date"
		}
		)
public class S1StepMain extends RServiceTask<S1Pool> implements IndexValuesProvider {

	@SuppressWarnings("unused")
	private String localText;
	
	@Override
	public String doExecute()  throws Exception {
		
		switch( getPool().getText1() ) {
		case "error1":
			throw new TaskException("F** Exception", "error1");
		case "fatal":
			throw new Exception("F** Excption");
		case "second":
			return "step2";
		case "third":
			return "step3";
		case "Moin":
			return RETRY;
		default:
			return getPool().getText1();
		}
	}

	@Override
	public String[] createIndexValues(boolean init) {
		return new String[] {"Main", getPool().getText1(), MDate.toFileFormat(new Date())};
	}

}