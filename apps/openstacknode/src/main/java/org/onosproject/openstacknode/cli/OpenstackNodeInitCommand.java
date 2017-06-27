/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.openstacknode.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeService;

/**
 * Initializes nodes for OpenStack node service.
 */
@Command(scope = "onos", name = "openstack-node-init",
        description = "Initializes nodes for OpenStack node service")
public class OpenstackNodeInitCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "hostnames", description = "Hostname(s)",
            required = true, multiValued = true)
    private String[] hostnames = null;

    @Override
    protected void execute() {
        OpenstackNodeService osNodeService =
                AbstractShellCommand.get(OpenstackNodeService.class);
        OpenstackNodeAdminService osNodeAdminService =
                AbstractShellCommand.get(OpenstackNodeAdminService.class);

        for (String hostname : hostnames) {
            OpenstackNode osNode = osNodeService.node(hostname);
            if (osNode == null) {
                print("Unable to find %s", hostname);
                continue;
            }
            OpenstackNode updated = osNode.updateState(NodeState.INIT);
            osNodeAdminService.updateNode(updated);
        }
    }
}
