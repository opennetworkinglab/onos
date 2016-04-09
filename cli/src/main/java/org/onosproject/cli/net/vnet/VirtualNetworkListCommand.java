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
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.utils.Comparators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Lists all virtual networks for the tenant ID.
 */
@Command(scope = "onos", name = "vnets",
        description = "Lists all virtual networks.")
public class VirtualNetworkListCommand extends AbstractShellCommand {

    private static final String FMT_VIRTUAL_NETWORK =
            "tenantId=%s, networkId=%s";

    @Override
    protected void execute() {

        getSortedVirtualNetworks().forEach(this::printVirtualNetwork);
    }

    /**
     * Returns the list of virtual networks sorted using the tenant identifier.
     *
     * @return sorted virtual network list
     */
    private List<VirtualNetwork> getSortedVirtualNetworks() {
        VirtualNetworkService service = get(VirtualNetworkService.class);
        VirtualNetworkAdminService adminService = get(VirtualNetworkAdminService.class);

        List<VirtualNetwork> virtualNetworks = new ArrayList<>();

        Set<TenantId> tenantSet = adminService.getTenantIds();
        tenantSet.forEach(tenantId -> virtualNetworks.addAll(service.getVirtualNetworks(tenantId)));

        Collections.sort(virtualNetworks, Comparators.VIRTUAL_NETWORK_COMPARATOR);
        return virtualNetworks;
    }

    /**
     * Prints out each virtual network.
     *
     * @param virtualNetwork virtual network
     */
    private void printVirtualNetwork(VirtualNetwork virtualNetwork) {
        print(FMT_VIRTUAL_NETWORK, virtualNetwork.tenantId(), virtualNetwork.id());
    }
}

