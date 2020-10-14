/**
 * Copyright (C) 2020 Mike Hummel (mh@mhus.de)
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
package de.mhus.cr.examples.users.register;

import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.util.activity.RServicePostNextTask;

@ActivityDescription(outputs = @Output(activity = WaitForConfirm.class))
public class SendConfirmMail extends RServicePostNextTask<RegisterUser> {

    @Override
    public void doExecute(PNode next) throws Exception {

        log().i("Send Confirm Mail External " + next.getId());
        log().i("Execute External Signal: bpme://" + next.getId());
    }
}
