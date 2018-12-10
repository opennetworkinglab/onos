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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Port;

import java.util.Optional;

import static org.onosproject.cli.AbstractShellCommand.get;
import static org.onosproject.openstacknetworking.api.Constants.UNSUPPORTED_VENDOR;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getIntfNameFromPciAddress;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;

/**
 * When SR-IOV-based VM is instantiated while the ovsdb connection to the device is lost,
 * VM is instantiated but the related VF port can't be added.
 * After recovering ovsdb connection, you can manually add VF ports by this CLI.
 */
@Service
@Command(scope = "onos", name = "openstack-direct-port-add",
        description = "Manually adds OpenStack direct ports to the device")
public class OpenstackDirectPortAddCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "port ID", description = "port ID", required = true)
    @Completion(DirectPortListCompleter.class)
    private String portId = null;

    @Override
    protected void doExecute() {
        OpenstackNetworkService osNetService = get(OpenstackNetworkService.class);
        OpenstackNodeService osNodeService = get(OpenstackNodeService.class);
        DeviceService deviceService = get(DeviceService.class);

        Port port = osNetService.port(portId);
        if (port == null) {
            log.error("There's no port that matches the port ID {}", portId);
            return;
        }

        Optional<OpenstackNode> osNode = osNodeService.completeNodes(COMPUTE).stream()
                .filter(node -> node.hostname().equals(port.getHostId()))
                .findAny();
        if (!osNode.isPresent()) {
            log.error("There's no openstackNode that matches hostname {}",
                    port.getHostId());
            return;
        }

        String intfName = getIntfNameFromPciAddress(port);
        if (intfName == null) {
            log.error("Failed to retrieve interface name from a port {}", portId);
            return;
        } else if (intfName.equals(UNSUPPORTED_VENDOR)) {
            return;
        }

        if (OpenstackNetworkingUtil.hasIntfAleadyInDevice(osNode.get().intgBridge(),
                intfName, deviceService)) {
            log.error("Interface {} is already added to the device {}", osNode.get().intgBridge());
            return;
        }

        log.info("Adding interface {} to the device {}..", intfName,
                osNode.get().intgBridge());

        osNodeService.addVfPort(osNode.get(), intfName);
    }
}
