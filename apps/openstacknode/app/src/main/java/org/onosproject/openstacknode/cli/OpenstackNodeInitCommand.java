/*
 * Copyright 2016-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeService;

/**
 * Initializes nodes for OpenStack node service.
 */
@Service
@Command(scope = "onos", name = "openstack-node-init",
        description = "Initializes nodes for OpenStack node service")
public class OpenstackNodeInitCommand extends AbstractShellCommand {

    @Option(name = "-a", aliases = "--all", description = "Apply this command to all nodes",
            required = false, multiValued = false)
    private boolean isAll = false;

    @Option(name = "-i", aliases = "--incomplete",
            description = "Apply this command to incomplete nodes",
            required = false, multiValued = false)
    private boolean isIncomplete = false;

    @Argument(index = 0, name = "hostnames", description = "Hostname(s) to apply this command",
            required = false, multiValued = true)
    @Completion(OpenstackHostnameCompleter.class)
    private String[] hostnames = null;

    @Override
    protected void doExecute() {
        OpenstackNodeService osNodeService = get(OpenstackNodeService.class);
        OpenstackNodeAdminService osNodeAdminService = get(OpenstackNodeAdminService.class);

        if (isAll && isIncomplete) {
            print("Please specify one of hostname, --all, and --incomplete options.");
            return;
        }

        if (isAll) {
            hostnames = osNodeService.nodes().stream()
                    .map(OpenstackNode::hostname).toArray(String[]::new);
        } else if (isIncomplete) {
            hostnames = osNodeService.nodes().stream()
                    .filter(osNode -> osNode.state() != NodeState.COMPLETE)
                    .map(OpenstackNode::hostname).toArray(String[]::new);
        }

        if (hostnames == null) {
            print("Please specify one of hostname, --all, and --incomplete options.");
            return;
        }

        for (String hostname : hostnames) {
            OpenstackNode osNode = osNodeService.node(hostname);
            if (osNode == null) {
                print("Unable to find %s", hostname);
                continue;
            }
            print("Initializing %s", hostname);
            OpenstackNode updated = osNode.updateState(NodeState.INIT);
            osNodeAdminService.updateNode(updated);
        }
        print("Done.");
    }
}
