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
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists all virtual links for the network ID.
 */
@Command(scope = "onos", name = "vnet-links",
        description = "Lists all virtual links in a virtual network.")
public class VirtualLinkListCommand extends AbstractShellCommand {

    private static final String FMT_VIRTUAL_LINK =
            "src=%s, dst=%s, state=%s, tunnelId=%s";

    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;

    @Override
    protected void execute() {

        getSortedVirtualLinks().forEach(this::printVirtualLink);
    }

    /**
     * Returns the list of virtual links sorted using the device identifier.
     *
     * @return virtual link list
     */
    private List<VirtualLink> getSortedVirtualLinks() {
        VirtualNetworkService service = get(VirtualNetworkService.class);

        List<VirtualLink> virtualLinks = new ArrayList<>();
        virtualLinks.addAll(service.getVirtualLinks(NetworkId.networkId(networkId)));
        return virtualLinks;
    }

    /**
     * Prints out each virtual link.
     *
     * @param virtualLink virtual link
     */
    private void printVirtualLink(VirtualLink virtualLink) {
        print(FMT_VIRTUAL_LINK, virtualLink.src().toString(), virtualLink.dst().toString(),
              virtualLink.state(),
              virtualLink.tunnelId() == null ? null : virtualLink.tunnelId().toString());
    }
}
