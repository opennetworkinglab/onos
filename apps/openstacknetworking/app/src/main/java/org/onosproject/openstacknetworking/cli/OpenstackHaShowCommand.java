/*
 * Copyright 2019-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackHaService;

/**
 * Shows openstack HA status.
 */
@Service
@Command(scope = "onos", name = "openstack-ha-show",
        description = "Show openstack active-standby HA status.")
public class OpenstackHaShowCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-20s%-30s";

    @Override
    protected void doExecute() {
        OpenstackHaService service = get(OpenstackHaService.class);

        print(FORMAT, "Status", "Active Node IP");

        print(FORMAT,
                service.isActive() ? "Active" : "Standby",
                service.getActiveIp() == null ? "None" : service.getActiveIp().toString());
    }
}
