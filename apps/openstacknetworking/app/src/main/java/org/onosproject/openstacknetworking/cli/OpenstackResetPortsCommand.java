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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortAdminService;

import static java.lang.Thread.sleep;
import static org.onosproject.openstacknetworking.api.InstancePort.State.INACTIVE;

/**
 * Purges existing instance ports.
 */
@Service
@Command(scope = "onos", name = "openstack-reset-ports",
        description = "Reset existing instance ports created by OpenStack networking app")
public class OpenstackResetPortsCommand extends AbstractShellCommand {

    @Option(name = "-a", aliases = "--all", description = "All of instance ports",
            required = false, multiValued = false)
    private boolean isAll = false;

    @Option(name = "-i", aliases = "--inactive",
            description = "Instance ports in inactive state",
            required = false, multiValued = false)
    private boolean isInactive = false;

    @Argument(index = 0, name = "portIds", description = "Instance Port IDs",
            required = false, multiValued = true)
    @Completion(InstancePortIdCompleter.class)
    private String[] portIds = null;

    private static final long SLEEP_MS = 1000; // we wait 1s for reset each port

    @Override
    protected void doExecute() {
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
            resetPort(service, portId);
            print("Successfully requested re-installing flow rules for port %s!", portId);
        }
        print("Done.");
    }

    private void resetPort(InstancePortAdminService service, String portId) {
        InstancePort instancePort = service.instancePort(portId);

        service.removeInstancePort(portId);
        try {
            sleep(SLEEP_MS);
        } catch (InterruptedException e) {
            log.error("Exception caused during port synchronization...");
        }
        service.createInstancePort(instancePort);
    }
}
