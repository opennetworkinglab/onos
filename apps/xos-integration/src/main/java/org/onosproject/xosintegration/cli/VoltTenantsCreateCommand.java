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
package org.onosproject.xosintegration.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.xosintegration.VoltTenant;
import org.onosproject.xosintegration.VoltTenantService;

/**
 * CLI command to create a new tenant.
 */
@Command(scope = "onos", name = "add-tenant",
        description = "Lists the inventory of VOLT tenants and their contents")
public class VoltTenantsCreateCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "service specific ID",
            description = "service specific ID",
            required = true, multiValued = false)
    String serviceSpecificId;

    @Argument(index = 1, name = "vlan ID",
            description = "vlan ID",
            required = true, multiValued = false)
    String vlanId;

    @Argument(index = 2, name = "port",
            description = "Port",
            required = true, multiValued = false)
    String port;

    @Override
    protected void execute() {
        VoltTenantService service = get(VoltTenantService.class);

        VoltTenant newTenant = VoltTenant.builder()
                .withServiceSpecificId(serviceSpecificId)
                .withVlanId(vlanId)
                .withPort(ConnectPoint.deviceConnectPoint(port))
                .build();

        service.addTenant(newTenant);
    }
}
