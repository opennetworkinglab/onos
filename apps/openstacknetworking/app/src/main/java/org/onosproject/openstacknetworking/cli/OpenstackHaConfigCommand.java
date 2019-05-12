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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackHaService;

/**
 * Configures openstack HA.
 */
@Service
@Command(scope = "onos", name = "openstack-ha-config",
        description = "Configure openstack active-standby HA status.")
public class OpenstackHaConfigCommand extends AbstractShellCommand {

    private static final String FLAG_TRUE = "true";
    private static final String FLAG_FALSE = "false";

    @Argument(index = 0, name = "active node", description = "active node",
            required = true, multiValued = false)
    private String active = null;

    @Override
    protected void doExecute() {
        OpenstackHaService service = get(OpenstackHaService.class);

        if (FLAG_TRUE.equalsIgnoreCase(active)) {
            service.setActive(true);
        } else if (FLAG_FALSE.equalsIgnoreCase(active)) {
            service.setActive(false);
        } else {
            error("The input value is not correct");
            return;
        }

        String role = service.isActive() ? "ACTIVE" : "STANDBY";

        print("Node is configured as " + role);
    }
}
