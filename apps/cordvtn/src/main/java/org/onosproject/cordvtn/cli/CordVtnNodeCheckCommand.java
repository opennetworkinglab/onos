/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.cordvtn.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cordvtn.CordVtnNode;
import org.onosproject.cordvtn.CordVtnNodeManager;

/**
 * Checks detailed node init state.
 */
@Command(scope = "onos", name = "cordvtn-node-check",
        description = "Shows detailed node init state")
public class CordVtnNodeCheckCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "hostname", description = "Hostname",
            required = true, multiValued = false)
    private String hostname = null;

    @Override
    protected void execute() {
        CordVtnNodeManager nodeManager = AbstractShellCommand.get(CordVtnNodeManager.class);
        CordVtnNode node = nodeManager.getNodes()
                .stream()
                .filter(n -> n.hostname().equals(hostname))
                .findFirst()
                .orElse(null);

        if (node == null) {
            print("Cannot find %s from registered nodes", hostname);
            return;
        }

        print(nodeManager.checkNodeInitState(node));
    }
}
