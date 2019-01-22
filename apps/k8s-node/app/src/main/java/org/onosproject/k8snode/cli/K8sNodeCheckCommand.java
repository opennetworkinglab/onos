/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snode.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;

import static org.onosproject.k8snode.api.Constants.GENEVE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.GRE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.k8snode.api.Constants.VXLAN_TUNNEL;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * Checks detailed node init state.
 */
@Command(scope = "onos", name = "k8s-node-check",
        description = "Shows detailed kubernetes node init state")
public class K8sNodeCheckCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "hostname", description = "Hostname",
            required = true, multiValued = false)
    private String hostname = null;

    private static final String MSG_OK = "OK";
    private static final String MSG_ERROR = "ERROR";

    @Override
    protected void execute() {
        K8sNodeService nodeService = get(K8sNodeService.class);
        DeviceService deviceService = get(DeviceService.class);

        K8sNode node = nodeService.node(hostname);
        if (node == null) {
            print("Cannot find %s from registered nodes", hostname);
            return;
        }

        print("[Integration Bridge Status]");
        Device device = deviceService.getDevice(node.intgBridge());
        if (device != null) {
            print("%s %s=%s available=%s %s",
                    deviceService.isAvailable(device.id()) ? MSG_OK : MSG_ERROR,
                    INTEGRATION_BRIDGE,
                    device.id(),
                    deviceService.isAvailable(device.id()),
                    device.annotations());
            if (node.dataIp() != null) {
                printPortState(deviceService, node.intgBridge(), VXLAN_TUNNEL);
                printPortState(deviceService, node.intgBridge(), GRE_TUNNEL);
                printPortState(deviceService, node.intgBridge(), GENEVE_TUNNEL);
            }
        } else {
            print("%s %s=%s is not available",
                    MSG_ERROR,
                    INTEGRATION_BRIDGE,
                    node.intgBridge());
        }
    }

    private void printPortState(DeviceService deviceService,
                                DeviceId deviceId, String portName) {
        Port port = deviceService.getPorts(deviceId).stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(portName) &&
                        p.isEnabled())
                .findAny().orElse(null);

        if (port != null) {
            print("%s %s portNum=%s enabled=%s %s",
                    port.isEnabled() ? MSG_OK : MSG_ERROR,
                    portName,
                    port.number(),
                    port.isEnabled() ? Boolean.TRUE : Boolean.FALSE,
                    port.annotations());
        } else {
            print("%s %s does not exist", MSG_ERROR, portName);
        }
    }
}
