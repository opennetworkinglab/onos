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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.utils.Comparators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lists all tenants.
 */
@Command(scope = "onos", name = "vnet-tenants",
        description = "Lists all virtual network tenants.")
public class TenantListCommand extends AbstractShellCommand {

    private static final String FMT_TENANT = "tenantId=%s";

    @Override
    protected void execute() {
        VirtualNetworkAdminService service = get(VirtualNetworkAdminService.class);
        List<TenantId> tenants = new ArrayList<>();
        tenants.addAll(service.getTenantIds());
        Collections.sort(tenants, Comparators.TENANT_ID_COMPARATOR);

        tenants.forEach(this::printTenant);
    }

    private void printTenant(TenantId tenantId) {
        print(FMT_TENANT, tenantId.id());
    }
}
