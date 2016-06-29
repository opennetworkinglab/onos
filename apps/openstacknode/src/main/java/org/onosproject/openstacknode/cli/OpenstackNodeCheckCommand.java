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

package org.onosproject.openstacknode.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknode.OpenstackNode;
import org.onosproject.openstacknode.OpenstackNodeService;

import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknode.Constants.*;
import static org.onosproject.openstacknode.OpenstackNodeService.NodeType.GATEWAY;

/**
 * Checks detailed node init state.
 */
@Command(scope = "onos", name = "openstack-node-check",
        description = "Shows detailed node init state")
public class OpenstackNodeCheckCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "hostname", description = "Hostname",
            required = true, multiValued = false)
    private String hostname = null;

    private static final String MSG_OK = "OK";
    private static final String MSG_NO = "NO";

    @Override
    protected void execute() {
        OpenstackNodeService nodeService = AbstractShellCommand.get(OpenstackNodeService.class);
        DeviceService deviceService = AbstractShellCommand.get(DeviceService.class);

        OpenstackNode node = nodeService.nodes()
                .stream()
                .filter(n -> n.hostname().equals(hostname))
                .findFirst()
                .orElse(null);

        if (node == null) {
            print("Cannot find %s from registered nodes", hostname);
            return;
        }

        print("%n[Integration Bridge Status]");
        Device device = deviceService.getDevice(node.intBridge());
        if (device != null) {
            print("%s %s=%s available=%s %s",
                    deviceService.isAvailable(device.id()) ? MSG_OK : MSG_NO,
                    INTEGRATION_BRIDGE,
                    device.id(),
                    deviceService.isAvailable(device.id()),
                    device.annotations());

            print(getPortState(deviceService, node.intBridge(), DEFAULT_TUNNEL));
        } else {
            print("%s %s=%s is not available",
                    MSG_NO,
                    INTEGRATION_BRIDGE,
                    node.intBridge());
        }

        if (node.type().equals(GATEWAY)) {
            print("%n[Router Bridge Status]");
            device = deviceService.getDevice(node.routerBridge().get());
            if (device != null) {
                print("%s %s=%s available=%s %s",
                        deviceService.isAvailable(device.id()) ? MSG_OK : MSG_NO,
                        ROUTER_BRIDGE,
                        device.id(),
                        deviceService.isAvailable(device.id()),
                        device.annotations());

                print(getPortState(deviceService, node.routerBridge().get(), PATCH_ROUT_BRIDGE));
                print(getPortState(deviceService, node.intBridge(), PATCH_INTG_BRIDGE));
            } else {
                print("%s %s=%s is not available",
                        MSG_NO,
                        ROUTER_BRIDGE,
                        node.intBridge());
            }
        }
    }

    private String getPortState(DeviceService deviceService, DeviceId deviceId, String portName) {
        Port port = deviceService.getPorts(deviceId).stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(portName) &&
                        p.isEnabled())
                .findAny().orElse(null);

        if (port != null) {
            return String.format("%s %s portNum=%s enabled=%s %s",
                    port.isEnabled() ? MSG_OK : MSG_NO,
                    portName,
                    port.number(),
                    port.isEnabled() ? Boolean.TRUE : Boolean.FALSE,
                    port.annotations());
        } else {
            return String.format("%s %s does not exist", MSG_NO, portName);
        }
    }
}
