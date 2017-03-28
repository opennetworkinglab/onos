/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.ofagent.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.ofagent.api.OFAgentAdminService;

/**
 * Stops the OFAgent.
 */
@Command(scope = "onos", name = "ofagent-stop", description = "Stops the ofagent")
public class OFAgentStopCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "network", description = "Virtual network ID",
            required = true, multiValued = false)
    private long networkId = NetworkId.NONE.id();

    @Override
    protected void execute() {
        OFAgentAdminService adminService = get(OFAgentAdminService.class);
        adminService.stopAgent(NetworkId.networkId(networkId));
        print("Successfully stopped OFAgent for network %s", networkId);
    }
}
