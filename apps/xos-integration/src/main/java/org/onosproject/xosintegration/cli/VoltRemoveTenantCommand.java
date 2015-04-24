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
import org.onosproject.xosintegration.VoltTenantService;

/**
 * CLI command to remove an existing tenant from the system.
 */
@Command(scope = "onos", name = "remove-tenant",
        description = "Removes a tenant")
public class VoltRemoveTenantCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "tenant",
            description = "Tenant ID",
            required = true, multiValued = false)
    String tenantIdString = null;

    @Override
    protected void execute() {
        VoltTenantService service = get(VoltTenantService.class);

        service.removeTenant(Long.parseLong(tenantIdString));
    }
}
