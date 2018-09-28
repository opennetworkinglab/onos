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
package org.onosproject.ofagent.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFAgentAdminService;

/**
 * Removes the existing OFAgent.
 */
@Service
@Command(scope = "onos", name = "ofagent-remove", description = "Removes the ofagent")
public class OFAgentRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "network", description = "Virtual network ID",
            required = true, multiValued = false)
    private long networkId = NetworkId.NONE.id();

    @Override
    protected void doExecute() {
        OFAgentAdminService adminService = get(OFAgentAdminService.class);
        OFAgent removed = adminService.removeAgent(NetworkId.networkId(networkId));
        if (removed != null) {
            print("Successfully removed OFAgent for network %s", networkId);
        } else {
            print("Failed to remove OFAgent for network %s", networkId);
        }
    }
}
