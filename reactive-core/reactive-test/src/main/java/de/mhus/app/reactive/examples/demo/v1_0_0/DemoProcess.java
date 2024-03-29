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
package de.mhus.app.reactive.examples.demo.v1_0_0;

import org.osgi.service.component.annotations.Component;

import de.mhus.app.reactive.model.activity.AProcess;
import de.mhus.app.reactive.model.annotations.ProcessDescription;
import de.mhus.app.reactive.util.bpmn2.RProcess;

@Component(service = AProcess.class)
@ProcessDescription(version = "1.0.0")
public class DemoProcess extends RProcess {}
