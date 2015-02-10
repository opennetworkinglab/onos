/*
 * Copyright 2014 Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;

import java.math.BigInteger;

/**
 * Removes host-to-host connectivity intent.
 */
@Command(scope = "onos", name = "remove-intent",
         description = "Removes the specified intent")
public class IntentRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "id", description = "Intent ID",
              required = true, multiValued = false)
    String id = null;

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);

        if (id.startsWith("0x")) {
            id = id.replaceFirst("0x", "");
        }

        Key key = Key.of(new BigInteger(id, 16).longValue(), appId());
        Intent intent = service.getIntent(key);
        if (intent != null) {
            service.withdraw(intent);
        }
    }
}
