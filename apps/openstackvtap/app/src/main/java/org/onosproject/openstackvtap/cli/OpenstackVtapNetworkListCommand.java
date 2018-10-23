/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.cli;

import com.google.common.collect.ImmutableSet;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstackvtap.api.OpenstackVtapAdminService;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lists openstack vtap networks.
 */
@Service
@Command(scope = "onos", name = "openstack-vtap-network-list",
        description = "OpenstackVtap network list")
public class OpenstackVtapNetworkListCommand extends AbstractShellCommand {

    private final OpenstackVtapAdminService osVtapAdminService = get(OpenstackVtapAdminService.class);
    private final OpenstackNodeService osNodeService = get(OpenstackNodeService.class);

    private static final String FORMAT = "mode [%s], networkId [%d], serverIp [%s]";
    private static final String FORMAT_NODES = "   openstack nodes: %s";

    @Override
    protected void doExecute() {
        OpenstackVtapNetwork vtapNetwork = osVtapAdminService.getVtapNetwork();
        if (vtapNetwork != null) {
            print(FORMAT,
                    vtapNetwork.mode().toString(),
                    vtapNetwork.networkId() != null ? vtapNetwork.networkId() : "N/A",
                    vtapNetwork.serverIp().toString());
            print(FORMAT_NODES, osNodeNames(osVtapAdminService.getVtapNetworkDevices()));
        }
    }

    private Set<String> osNodeNames(Set<DeviceId> deviceIds) {
        if (deviceIds == null) {
            return ImmutableSet.of();
        } else {
            return deviceIds.parallelStream()
                    .map(osNodeService::node)
                    .filter(Objects::nonNull)
                    .map(OpenstackNode::hostname)
                    .collect(Collectors.toSet());
        }
    }

}
