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
import org.onosproject.openstacknode.OpenstackNode;
import org.onosproject.openstacknode.OpenstackNodeService;

import java.util.NoSuchElementException;

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
        OpenstackNodeService nodeService = AbstractShellCommand.get(OpenstackNodeService.class);

        for (String hostname : hostnames) {
            OpenstackNode node;
            try {
                node = nodeService.nodes()
                        .stream()
                        .filter(n -> n.hostname().equals(hostname))
                        .findFirst().get();
            } catch (NoSuchElementException e) {
                print("Unable to find %s", hostname);
                continue;
            }

            nodeService.addOrUpdateNode(node);
        }
    }
}
