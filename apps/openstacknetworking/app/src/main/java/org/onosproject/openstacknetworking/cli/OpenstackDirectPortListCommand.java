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
package org.onosproject.openstacknetworking.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;

import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.openstacknetworking.api.Constants.DIRECT;
import static org.onosproject.openstacknetworking.api.Constants.PCISLOT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getIntfNameFromPciAddress;

/**
 * Lists OpenStack direct ports.
 */
@Command(scope = "onos", name = "openstack-direct-ports",
        description = "Lists all OpenStack direct ports")
public class OpenstackDirectPortListCommand extends AbstractShellCommand {
    private static final String UNBOUND = "unbound";
    private static final String FORMAT = "%-40s%-20s%-20s%-20s%-20s%-20s";

    @Override
    protected void execute() {
        OpenstackNetworkService service = AbstractShellCommand.get(OpenstackNetworkService.class);

        List<Port> ports = service.ports().stream()

                .filter(port -> port.getvNicType().equals(DIRECT))
                .filter(port -> port.isAdminStateUp() && !port.getVifType().equals(UNBOUND))
                .collect(Collectors.toList());


        print(FORMAT, "ID", "Network", "MAC", "FIXED IPs", "PCI Slot", "Interface");
        for (Port port: ports) {
            List<String> fixedIps = port.getFixedIps().stream()
                    .map(IP::getIpAddress)
                    .collect(Collectors.toList());

            Network osNet = service.network(port.getNetworkId());
            print(FORMAT, port.getId(),
                    osNet.getName(),
                    port.getMacAddress(),
                    fixedIps.isEmpty() ? "" : fixedIps,
                    port.getProfile() == null ? "" : port.getProfile().get(PCISLOT).toString(),
                    getIntfNameFromPciAddress(port));
        }
    }
}
