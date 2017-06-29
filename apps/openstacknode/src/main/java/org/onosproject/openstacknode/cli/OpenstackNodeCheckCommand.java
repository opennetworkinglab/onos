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
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;

import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknode.api.Constants.*;
import static org.onosproject.openstacknode.api.OpenstackNode.NetworkMode.VLAN;
import static org.onosproject.openstacknode.api.OpenstackNode.NetworkMode.VXLAN;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;

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
    private static final String BUCKET_FORMAT =
            "   bucket=%s, bytes=%s, packets=%s, actions=%s";

    @Override
    protected void execute() {
        OpenstackNodeService osNodeService = AbstractShellCommand.get(OpenstackNodeService.class);
        DeviceService deviceService = AbstractShellCommand.get(DeviceService.class);
        GroupService groupService = AbstractShellCommand.get(GroupService.class);

        OpenstackNode osNode = osNodeService.node(hostname);
        if (osNode == null) {
            print("Cannot find %s from registered nodes", hostname);
            return;
        }

        print("[Integration Bridge Status]");
        Device device = deviceService.getDevice(osNode.intgBridge());
        if (device != null) {
            print("%s %s=%s available=%s %s",
                    deviceService.isAvailable(device.id()) ? MSG_OK : MSG_NO,
                    INTEGRATION_BRIDGE,
                    device.id(),
                    deviceService.isAvailable(device.id()),
                    device.annotations());
            if (osNode.dataIp() != null) {
                printPortState(deviceService, osNode.intgBridge(), DEFAULT_TUNNEL);
            }
            if (osNode.vlanIntf() != null) {
                printPortState(deviceService, osNode.intgBridge(), osNode.vlanIntf());
            }
            printGatewayGroupState(osNodeService, groupService, osNode);
        } else {
            print("%s %s=%s is not available",
                    MSG_NO,
                    INTEGRATION_BRIDGE,
                    osNode.intgBridge());
        }
    }

    private void printPortState(DeviceService deviceService, DeviceId deviceId, String portName) {
        Port port = deviceService.getPorts(deviceId).stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(portName) &&
                        p.isEnabled())
                .findAny().orElse(null);

        if (port != null) {
            print("%s %s portNum=%s enabled=%s %s",
                    port.isEnabled() ? MSG_OK : MSG_NO,
                    portName,
                    port.number(),
                    port.isEnabled() ? Boolean.TRUE : Boolean.FALSE,
                    port.annotations());
        } else {
            print("%s %s does not exist", MSG_NO, portName);
        }
    }

    private void printGatewayGroupState(OpenstackNodeService osNodeService,
                                        GroupService groupService, OpenstackNode osNode) {
        if (osNode.type() == GATEWAY) {
            return;
        }
        if (osNodeService.completeNodes(GATEWAY).isEmpty()) {
            print("N/A No complete state gateway nodes exist");
            return;
        }
        if (osNode.dataIp() != null) {
            Group osGroup = groupService.getGroup(osNode.intgBridge(),
                    osNode.gatewayGroupKey(VXLAN));
            if (osGroup == null || osGroup.state() != Group.GroupState.ADDED) {
                print("%s VXLAN gateway group does not exist", MSG_NO);
            } else {
                print("%s VXLAN group 0x%s added", MSG_OK, Integer.toHexString(osGroup.id().id()));
                int i = 0;
                for (GroupBucket bucket : osGroup.buckets().buckets()) {
                    print(BUCKET_FORMAT, ++i, bucket.bytes(), bucket.packets(),
                            bucket.treatment().allInstructions());
                }
            }
        }
        if (osNode.vlanIntf() != null) {
            Group osGroup = groupService.getGroup(osNode.intgBridge(),
                    osNode.gatewayGroupKey(VLAN));
            if (osGroup == null || osGroup.state() != Group.GroupState.ADDED) {
                print("\n%s VLAN gateway group does not exist", MSG_NO);
            } else {
                print("\n%s VLAN group 0x%s added", MSG_OK, Integer.toHexString(osGroup.id().id()));
                int i = 0;
                for (GroupBucket bucket : osGroup.buckets().buckets()) {
                    print(BUCKET_FORMAT, ++i, bucket.bytes(), bucket.packets(),
                            bucket.treatment().allInstructions());
                }
            }
        }
    }
}
