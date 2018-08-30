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
package org.onosproject.openstacknetworking.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static org.onosproject.openstacknetworking.api.Constants.DIRECT;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.UNSUPPORTED_VENDOR;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getIntfNameFromPciAddress;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.hasIntfAleadyInDevice;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.CONTROLLER;

@Component(immediate = true)
public final class OpenStackSwitchingDirectPortProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String UNBOUND = "unbound";
    private static final String PORT_NAME = "portName";
    private static final long SLEEP_MS = 3000;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    private final OpenstackNetworkListener openstackNetworkListener = new InternalOpenstackNetworkListener();
    private final InternalOpenstackNodeListener internalNodeListener = new InternalOpenstackNodeListener();

    private NodeId localNodeId;
    private ApplicationId appId;

    @Activate
    void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        osNetworkService.addListener(openstackNetworkListener);
        osNodeService.addListener(internalNodeListener);

        log.info("Started");
    }

    @Deactivate
    void deactivate() {
        leadershipService.withdraw(appId.name());
        osNetworkService.removeListener(openstackNetworkListener);
        osNodeService.removeListener(internalNodeListener);

        log.info("Stopped");
    }

    private void processPortAdded(Port port) {
        if (!port.getvNicType().equals(DIRECT)) {
            return;
        } else if (!port.isAdminStateUp() || port.getVifType().equals(UNBOUND)) {
            log.trace("processPortAdded skipped because of status: {}, adminStateUp: {}, vifType: {}",
                    port.getState(), port.isAdminStateUp(), port.getVifType());
            return;
        } else {
            Optional<OpenstackNode> osNode = osNodeService.completeNodes(COMPUTE).stream()
                    .filter(node -> node.hostname().equals(port.getHostId()))
                    .findAny();
            if (!osNode.isPresent()) {
                log.error("processPortAdded failed because openstackNode doesn't exist that matches hostname {}",
                        port.getHostId());
                return;
            }
            log.trace("Retrieved openstackNode: {}", osNode.get().toString());

            String intfName = getIntfNameFromPciAddress(port);
            if (intfName == null) {
                log.error("Failed to execute processPortAdded because of null interface name");
                return;
            } else if (intfName.equals(UNSUPPORTED_VENDOR)) {
                log.warn("Failed to execute processPortAdded because of unsupported vendor for ovs-based sr-iov");
                return;
            }
            log.trace("Retrieved interface name: {}", intfName);

            try {
                //If a VF port has been already added to the device for some reason, we remove it first,
                //and the add VF so that other handlers run their logic.
                if (hasIntfAleadyInDevice(osNode.get().intgBridge(),
                        intfName, deviceService)) {
                    log.trace("Device {} has already has VF interface {}, so remove first.",
                            osNode.get().intgBridge(),
                            intfName);
                    osNodeService.removeVfPort(osNode.get(), intfName);
                    //we wait 3000ms because the ovsdb client can't deal with removal/add at the same time.
                    sleep(SLEEP_MS);
                }
            } catch (InterruptedException e) {
                log.error("Exception occurred because of {}", e.toString());
            }

            osNodeService.addVfPort(osNode.get(), intfName);
        }
    }

    private void processPortRemoved(Port port) {
        if (!port.getvNicType().equals(DIRECT)) {
            return;
        } else if (instancePortService.instancePort(port.getId()) == null) {
            log.trace("processPortRemoved skipped because no instance port exist for portId: {}", port.getId());
            return;
        } else {
            InstancePort instancePort = instancePortService.instancePort(port.getId());
            if (instancePort == null) {
                return;
            }
            DeviceId deviceId = instancePort.deviceId();
            if (deviceId == null) {
                return;
            }
            OpenstackNode osNode = osNodeService.node(deviceId);
            if (osNode == null) {
                return;
            }

            Optional<org.onosproject.net.Port> removedPort = deviceService.getPorts(deviceId).stream()
                    .filter(p -> Objects.equals(p.number(), instancePort.portNumber()))
                    .findAny();

            if (!removedPort.isPresent()) {
                log.error("Failed to execute processPortAdded because port number doesn't exist");
                return;
            }

            String intfName = removedPort.get().annotations().value(PORT_NAME);

            if (intfName == null) {
                log.error("Failed to execute processPortAdded because of null interface name");
                return;
            }
            log.trace("Retrieved interface name: {}", intfName);

            osNodeService.removeVfPort(osNode, intfName);
        }
    }

    private class InternalOpenstackNetworkListener implements OpenstackNetworkListener {
        @Override
        public boolean isRelevant(OpenstackNetworkEvent event) {
            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            if (!Objects.equals(localNodeId, leader)) {
                return false;
            }
            return true;
        }

        @Override
        public void event(OpenstackNetworkEvent event) {
            switch (event.type()) {
                case OPENSTACK_PORT_UPDATED:
                    if (event.port().getState() == State.DOWN) {
                        processPortRemoved(event.port());
                    } else {
                        processPortAdded(event.port());
                    }
                    break;
                case OPENSTACK_PORT_REMOVED:
                    processPortRemoved(event.port());
                    break;
                default:
                    break;

            }
        }
    }

    private class InternalOpenstackNodeListener implements OpenstackNodeListener {

        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {

            if (event.subject().type() == CONTROLLER) {
                return false;
            }
            // do not allow to proceed without mastership
            Device device = deviceService.getDevice(event.subject().intgBridge());
            if (device == null) {
                return false;
            }
            return mastershipService.isLocalMaster(device.id());
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();

            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                    log.info("COMPLETE node {} is detected", osNode.hostname());
                    processComputeState(event.subject());

                    break;
                case OPENSTACK_NODE_INCOMPLETE:
                case OPENSTACK_NODE_CREATED:
                case OPENSTACK_NODE_UPDATED:
                case OPENSTACK_NODE_REMOVED:
                    // not reacts to the events other than complete and incomplete states
                    break;
                default:
                    break;
            }
        }

        private void processComputeState(OpenstackNode node) {
            List<Port> ports = osNetworkService.ports().stream()
                    .filter(port -> port.getvNicType().equals(DIRECT))
                    .filter(port -> !port.getVifType().equals(UNBOUND))
                    .filter(port -> port.getHostId().equals(node.hostname()))
                    .collect(Collectors.toList());

            ports.forEach(port -> {
                addIntfToDevice(node, port);
            });
        }

        private void addIntfToDevice(OpenstackNode node, Port port) {
            String intfName = OpenstackNetworkingUtil.getIntfNameFromPciAddress(port);
            if (intfName == null) {
                log.error("Failed to retrieve interface name from a port {}", port.getId());
            } else if (intfName.equals(UNSUPPORTED_VENDOR)) {
                log.warn("Failed to retrieve interface name from a port {} because of unsupported ovs-based sr-iov");
                return;
            }

            if (!hasIntfAleadyInDevice(node.intgBridge(), intfName, deviceService)) {
                log.debug("Port {} is bound to the interface {} but not added in the bridge {}. Adding it..",
                        port.getId(),
                        intfName,
                        node.intgBridge());
                osNodeService.addVfPort(node, intfName);
            }
        }
    }
}
