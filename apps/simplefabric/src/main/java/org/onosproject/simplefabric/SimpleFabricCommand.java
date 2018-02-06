/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.simplefabric;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;

/**
 * CLI to interact with the SIMPLE_FABRIC application.
 */
@Command(scope = "onos", name = "simpleFabric",
         description = "Manages the SimpleFabric application")
public class SimpleFabricCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "command",
              description = "Command: show|intents|reactive-intents|refresh|flush",
              required = true, multiValued = false)
    String command = null;

    @Override
    protected void execute() {

        SimpleFabricService simpleFabric = get(SimpleFabricService.class);

        if (command == null) {
            print("command not found", command);
            return;
        }
        switch (command) {
        case "show":
            simpleFabric.dumpToStream("show", System.out);
            break;
        case "intents":
            simpleFabric.dumpToStream("intents", System.out);
            break;
        case "reactive-intents":
            simpleFabric.dumpToStream("reactive-intents", System.out);
            break;
        case "refresh":
            simpleFabric.triggerRefresh();
            System.out.println("simple fabric refresh triggered");
            break;
        case "flush":
            simpleFabric.triggerFlush();
            System.out.println("simple fabric flush triggered");
            break;
        default:
            System.out.println("unknown command: " + command);
            break;
        }
    }

}

