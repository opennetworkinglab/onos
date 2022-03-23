/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;

import static org.onosproject.kubevirtnode.api.Constants.GENEVE;
import static org.onosproject.kubevirtnode.api.Constants.GRE;
import static org.onosproject.kubevirtnode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.kubevirtnode.api.Constants.STT;
import static org.onosproject.kubevirtnode.api.Constants.TUNNEL_BRIDGE;
import static org.onosproject.kubevirtnode.api.Constants.VXLAN;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * Checks detailed node init state.
 */
@Service
@Command(scope = "onos", name = "kubevirt-check-node",
        description = "Shows detailed kubevirt nodes status")
public class KubevirtCheckNodeCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "hostname", description = "Hostname",
            required = true, multiValued = false)
    @Completion(KubevirtHostnameCompleter.class)
    private String hostname = null;

    private static final String MSG_PASS = "PASS";
    private static final String MSG_FAIL = "FAIL";

    @Override
    protected void doExecute() throws Exception {
        KubevirtNodeService nodeService = get(KubevirtNodeService.class);
        DeviceService deviceService = get(DeviceService.class);

        KubevirtNode node = nodeService.node(hostname);
        if (node == null) {
            print("Cannot find %s from registered nodes", hostname);
            return;
        }

        print("[Integration Bridge Status]");
        Device intgBridge = deviceService.getDevice(node.intgBridge());
        if (intgBridge != null) {
            print("%s %s=%s available=%s %s",
                    deviceService.isAvailable(intgBridge.id()) ? MSG_PASS : MSG_FAIL,
                    INTEGRATION_BRIDGE,
                    intgBridge.id(),
                    deviceService.isAvailable(intgBridge.id()),
                    intgBridge.annotations());
        }

        print("");
        print("[Tunnel Bridge Status]");
        Device tunBridge = deviceService.getDevice(node.tunBridge());
        if (tunBridge != null) {
            print("%s %s=%s available=%s %s",
                    deviceService.isAvailable(tunBridge.id()) ? MSG_PASS : MSG_FAIL,
                    TUNNEL_BRIDGE,
                    tunBridge.id(),
                    deviceService.isAvailable(tunBridge.id()),
                    tunBridge.annotations());

            if (node.dataIp() != null) {
                printPortState(deviceService, node.tunBridge(), VXLAN);
                printPortState(deviceService, node.tunBridge(), GRE);
                printPortState(deviceService, node.tunBridge(), GENEVE);
                printPortState(deviceService, node.tunBridge(), STT);
            }
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
                    port.isEnabled() ? MSG_PASS : MSG_FAIL,
                    portName,
                    port.number(),
                    port.isEnabled() ? Boolean.TRUE : Boolean.FALSE,
                    port.annotations());
        } else {
            print("%s %s does not exist", MSG_FAIL, portName);
        }
    }
}
