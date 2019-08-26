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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;

import static org.onosproject.k8snode.api.Constants.EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.GENEVE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.GRE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_TO_EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_TO_LOCAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.LOCAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.LOCAL_TO_INTEGRATION_BRIDGE;
import static org.onosproject.k8snode.api.Constants.PHYSICAL_EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.VXLAN_TUNNEL;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * Checks detailed node init state.
 */
@Service
@Command(scope = "onos", name = "k8s-node-check",
        description = "Shows detailed kubernetes node init state")
public class K8sNodeCheckCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "hostname", description = "Hostname",
            required = true, multiValued = false)
    @Completion(K8sHostnameCompleter.class)
    private String hostname = null;

    private static final String MSG_OK = "OK";
    private static final String MSG_ERROR = "ERROR";

    @Override
    protected void doExecute() {
        K8sNodeService nodeService = get(K8sNodeService.class);
        DeviceService deviceService = get(DeviceService.class);

        K8sNode node = nodeService.node(hostname);
        if (node == null) {
            print("Cannot find %s from registered nodes", hostname);
            return;
        }

        print("[Integration Bridge Status]");
        Device intgBridge = deviceService.getDevice(node.intgBridge());
        if (intgBridge != null) {
            print("%s %s=%s available=%s %s",
                    deviceService.isAvailable(intgBridge.id()) ? MSG_OK : MSG_ERROR,
                    INTEGRATION_BRIDGE,
                    intgBridge.id(),
                    deviceService.isAvailable(intgBridge.id()),
                    intgBridge.annotations());
            printPortState(deviceService, node.intgBridge(), INTEGRATION_BRIDGE);
            printPortState(deviceService, node.intgBridge(), INTEGRATION_TO_EXTERNAL_BRIDGE);
            printPortState(deviceService, node.intgBridge(), INTEGRATION_TO_LOCAL_BRIDGE);
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

        print("[External Bridge Status]");
        Device extBridge = deviceService.getDevice(node.extBridge());
        if (extBridge != null) {
            print("%s %s=%s available=%s %s",
                    deviceService.isAvailable(extBridge.id()) ? MSG_OK : MSG_ERROR,
                    EXTERNAL_BRIDGE,
                    extBridge.id(),
                    deviceService.isAvailable(extBridge.id()),
                    extBridge.annotations());
            printPortState(deviceService, node.extBridge(), EXTERNAL_BRIDGE);
            printPortState(deviceService, node.extBridge(), PHYSICAL_EXTERNAL_BRIDGE);
        } else {
            print("%s %s=%s is not available",
                    MSG_ERROR,
                    EXTERNAL_BRIDGE,
                    node.extBridge());
        }

        print("[Local Bridge Status]");
        Device localBridge = deviceService.getDevice(node.localBridge());
        if (localBridge != null) {
            print("%s %s=%s available=%s %s",
                    deviceService.isAvailable(localBridge.id()) ? MSG_OK : MSG_ERROR,
                    LOCAL_BRIDGE,
                    localBridge.id(),
                    deviceService.isAvailable(localBridge.id()),
                    localBridge.annotations());
            printPortState(deviceService, node.localBridge(), LOCAL_BRIDGE);
            printPortState(deviceService, node.localBridge(), LOCAL_TO_INTEGRATION_BRIDGE);
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
