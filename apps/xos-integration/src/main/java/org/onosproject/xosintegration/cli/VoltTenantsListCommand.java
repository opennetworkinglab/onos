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

import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.xosintegration.VoltTenant;
import org.onosproject.xosintegration.VoltTenantService;

/**
 * CLI command for listing VOLT tenant objects.
 */

/**
 * CLI command to list the existing tenants.
 */
@Command(scope = "onos", name = "tenants",
        description = "Lists the inventory of VOLT tenants and their contents")
public class VoltTenantsListCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "tenantId",
            description = "Tenant ID",
            required = false, multiValued = false)
    private String tenantId = null;

    @Override
    protected void execute() {
        VoltTenantService service = get(VoltTenantService.class);

        if (tenantId != null) {
            VoltTenant tenant = service.getTenant(Long.parseLong(tenantId));
            if (tenant != null) {
                print(tenant.toString());
            } else {
                error("Tenant not found {}", tenantId);
            }
        } else {
            Set<VoltTenant> tenants = service.getAllTenants();
            for (VoltTenant tenant : tenants) {
                print(tenant.toString());
            }
        }
    }

}
