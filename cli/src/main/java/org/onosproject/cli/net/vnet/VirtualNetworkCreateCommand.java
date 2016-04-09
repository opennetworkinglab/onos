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

package org.onosproject.cli.net.vnet;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;

/**
 * Creates a new virtual network.
 */
@Command(scope = "onos", name = "vnet-create",
        description = "Creates a new virtual network.")
public class VirtualNetworkCreateCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "id", description = "Tenant ID",
            required = true, multiValued = false)
    String id = null;

    @Override
    protected void execute() {
        VirtualNetworkAdminService service = get(VirtualNetworkAdminService.class);
        service.createVirtualNetwork(TenantId.tenantId(id));
        print("Virtual network successfully created.");
    }
}
