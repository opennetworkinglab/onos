/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;

import static org.onosproject.net.intent.IntentState.WITHDRAWN;

/**
 * Purges all WITHDRAWN intents.
 */
@Command(scope = "onos", name = "purge-intents",
         description = "Purges all WITHDRAWN intents")
public class IntentPurgeCommand extends AbstractShellCommand {
    @Override
    protected void execute() {
        IntentService intentService = get(IntentService.class);
        for (Intent intent: intentService.getIntents()) {
            if (intentService.getIntentState(intent.key()) == WITHDRAWN) {
                intentService.purge(intent);
            }
        }
    }
}
