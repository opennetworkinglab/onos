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
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists all virtual hosts for the network ID.
 */
@Command(scope = "onos", name = "vnet-hosts",
        description = "Lists all virtual hosts in a virtual network.")
public class VirtualHostListCommand extends AbstractShellCommand {

    private static final String FMT_VIRTUAL_HOST =
            "id=%s, mac=%s, vlan=%s, location=%s, ips=%s";

    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;

    @Override
    protected void execute() {
        getSortedVirtualHosts().forEach(this::printVirtualHost);
    }

    /**
     * Returns the list of virtual hosts sorted using the device identifier.
     *
     * @return virtual host list
     */
    private List<VirtualHost> getSortedVirtualHosts() {
        VirtualNetworkService service = get(VirtualNetworkService.class);

        List<VirtualHost> virtualHosts = new ArrayList<>();
        virtualHosts.addAll(service.getVirtualHosts(NetworkId.networkId(networkId)));
        return virtualHosts;
    }

    /**
     * Prints out each virtual host.
     *
     * @param virtualHost virtual host
     */
    private void printVirtualHost(VirtualHost virtualHost) {
        print(FMT_VIRTUAL_HOST, virtualHost.id().toString(), virtualHost.mac().toString(),
              virtualHost.vlan().toString(), virtualHost.location().toString(),
              virtualHost.ipAddresses().toString());
    }
}
