/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortAdminService;

import static org.onosproject.openstacknetworking.api.InstancePort.State.INACTIVE;

/**
 * Purges existing instance ports.
 */
@Command(scope = "onos", name = "purge-instance-ports",
        description = "Purges existing instance ports created by OpenStack networking app")
public class PurgeInstancePortsCommand extends AbstractShellCommand {

    @Option(name = "-a", aliases = "--all", description = "All of instance ports",
            required = false, multiValued = false)
    private boolean isAll = false;

    @Option(name = "-i", aliases = "--inactive",
            description = "Instance ports in inactive state",
            required = false, multiValued = false)
    private boolean isInactive = false;

    @Argument(index = 0, name = "portIds", description = "Instance Port IDs",
            required = false, multiValued = true)
    private String[] portIds = null;

    @Override
    protected void execute() {
        InstancePortAdminService service = get(InstancePortAdminService.class);

        if ((!isAll && !isInactive && portIds == null) ||
                (isAll && isInactive) ||
                (isInactive && portIds != null) ||
                (portIds != null && isAll)) {
            print("Please specify one of portIds, --all, and --inactive options.");
            return;
        }

        if (isAll) {
            portIds = service.instancePorts().stream()
                             .map(InstancePort::portId).toArray(String[]::new);
        } else if (isInactive) {
            portIds = service.instancePorts().stream()
                             .filter(p -> p.state() == INACTIVE)
                             .map(InstancePort::portId).toArray(String[]::new);
        }

        for (String portId : portIds) {
            service.removeInstancePort(portId);
            print("Instance port %s has been removed!", portId);
        }
        print("Done.");
    }
}
