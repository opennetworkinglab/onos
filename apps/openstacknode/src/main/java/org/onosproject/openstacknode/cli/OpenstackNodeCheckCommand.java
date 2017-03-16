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
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.openstacknode.OpenstackNode;
import org.onosproject.openstacknode.OpenstackNodeService;

import java.util.Optional;

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

        print("[Integration Bridge Status]");
        Device device = deviceService.getDevice(node.intBridge());
        if (device != null) {
            print("%s %s=%s available=%s %s",
                    deviceService.isAvailable(device.id()) ? MSG_OK : MSG_NO,
                    INTEGRATION_BRIDGE,
                    device.id(),
                    deviceService.isAvailable(device.id()),
                    device.annotations());

            node.dataIp().ifPresent(ip -> print(getPortState(deviceService, node.intBridge(), DEFAULT_TUNNEL)));
            node.vlanPort().ifPresent(p -> print(getPortState(deviceService, node.intBridge(), p)));
        } else {
            print("%s %s=%s is not available",
                    MSG_NO,
                    INTEGRATION_BRIDGE,
                    node.intBridge());
        }

        if (node.type().equals(GATEWAY)) {
            print(getPortState(deviceService, node.intBridge(), PATCH_INTG_BRIDGE));

            print("%n[Router Bridge Status]");
            device = deviceService.getDevice(node.ovsdbId());
            if (device == null || !device.is(BridgeConfig.class)) {
                print("%s %s=%s is not available(unable to connect OVSDB)",
                      MSG_NO,
                      ROUTER_BRIDGE,
                      node.intBridge());
            } else {
                BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
                boolean available = bridgeConfig.getBridges().stream()
                        .filter(bridge -> bridge.name().equals(ROUTER_BRIDGE))
                        .findAny()
                        .isPresent();

                print("%s %s=%s available=%s",
                      available ? MSG_OK : MSG_NO,
                      ROUTER_BRIDGE,
                      node.routerBridge().get(),
                      available);

                print(getPortStateOvsdb(deviceService, node.ovsdbId(), PATCH_ROUT_BRIDGE));
                print(getPortStateOvsdb(deviceService, node.ovsdbId(), node.uplink().get()));
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

    private String getPortStateOvsdb(DeviceService deviceService, DeviceId deviceId, String portName) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null || !device.is(BridgeConfig.class)) {
            return String.format("%s %s does not exist(unable to connect OVSDB)",
                                 MSG_NO, portName);
        }

        BridgeConfig bridgeConfig =  device.as(BridgeConfig.class);
        Optional<PortDescription> port = bridgeConfig.getPorts().stream()
                .filter(p -> p.annotations().value(PORT_NAME).contains(portName))
                .findAny();

        if (port.isPresent()) {
            return String.format("%s %s portNum=%s enabled=%s %s",
                                 port.get().isEnabled() ? MSG_OK : MSG_NO,
                                 portName,
                                 port.get().portNumber(),
                                 port.get().isEnabled() ? Boolean.TRUE : Boolean.FALSE,
                                 port.get().annotations());
        } else {
            return String.format("%s %s does not exist", MSG_NO, portName);
        }
    }
}
