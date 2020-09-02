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
                    node.intgBridgeName(),
                    intgBridge.id(),
                    deviceService.isAvailable(intgBridge.id()),
                    intgBridge.annotations());
            printPortState(deviceService, node.intgBridge(), node.intgBridgePortName());
            printPortState(deviceService, node.intgBridge(), node.intgToExtPatchPortName());
            printPortState(deviceService, node.intgBridge(), node.intgToLocalPatchPortName());
        } else {
            print("%s %s=%s is not available",
                    MSG_ERROR,
                    node.intgBridgeName(),
                    node.intgBridge());
        }

        print("");
        print("[External Bridge Status]");
        Device extBridge = deviceService.getDevice(node.extBridge());
        if (extBridge != null) {
            print("%s %s=%s available=%s %s",
                    deviceService.isAvailable(extBridge.id()) ? MSG_OK : MSG_ERROR,
                    node.extBridgeName(),
                    extBridge.id(),
                    deviceService.isAvailable(extBridge.id()),
                    extBridge.annotations());
            printPortState(deviceService, node.extBridge(), node.extToIntgPatchPortName());
        } else {
            print("%s %s=%s is not available",
                    MSG_ERROR,
                    node.extBridgeName(),
                    node.extBridge());
        }

        print("");
        print("[Local Bridge Status]");
        Device localBridge = deviceService.getDevice(node.localBridge());
        if (localBridge != null) {
            print("%s %s=%s available=%s %s",
                    deviceService.isAvailable(localBridge.id()) ? MSG_OK : MSG_ERROR,
                    node.localBridgeName(),
                    localBridge.id(),
                    deviceService.isAvailable(localBridge.id()),
                    localBridge.annotations());
            printPortState(deviceService, node.localBridge(), node.localToIntgPatchPortName());
        }

        print("");
        print("[Tunnel Bridge Status]");
        Device tunBridge = deviceService.getDevice(node.tunBridge());
        if (tunBridge != null) {
            print("%s %s=%s available=%s %s",
                    deviceService.isAvailable(tunBridge.id()) ? MSG_OK : MSG_ERROR,
                    node.tunBridgeName(),
                    tunBridge.id(),
                    deviceService.isAvailable(tunBridge.id()),
                    tunBridge.annotations());
            printPortState(deviceService, node.tunBridge(), node.tunToIntgPatchPortName());

            if (node.dataIp() != null) {
                printPortState(deviceService, node.tunBridge(), node.vxlanPortName());
                printPortState(deviceService, node.tunBridge(), node.grePortName());
                printPortState(deviceService, node.tunBridge(), node.genevePortName());
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
