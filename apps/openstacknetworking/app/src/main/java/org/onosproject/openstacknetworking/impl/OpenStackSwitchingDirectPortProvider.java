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
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.State;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Objects;
import java.util.Optional;

import static org.onosproject.openstacknetworking.api.Constants.DIRECT;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getIntfNameFromPciAddress;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;

@Component(immediate = true)
public final class OpenStackSwitchingDirectPortProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String OVSDB_PORT = "ovsdbPortNum";
    private static final int DEFAULT_OVSDB_PORT = 6640;
    private static final String UNBOUND = "unbound";
    private static final String PORT_NAME = "portName";

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
    protected OvsdbController ovsdbController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Property(name = OVSDB_PORT, intValue = DEFAULT_OVSDB_PORT,
            label = "OVSDB server listen port")
    private int ovsdbPort = DEFAULT_OVSDB_PORT;

    private final OpenstackNetworkListener openstackNetworkListener = new InternalOpenstackNetworkListener();

    private NodeId localNodeId;
    private ApplicationId appId;

    @Activate
    void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        osNetworkService.addListener(openstackNetworkListener);
        componentConfigService.registerProperties(getClass());

        log.info("Started");
    }

    @Deactivate
    void deactivate() {
        leadershipService.withdraw(appId.name());
        osNetworkService.removeListener(openstackNetworkListener);
        componentConfigService.unregisterProperties(getClass(), false);

        log.info("Stopped");
    }


    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        int updatedOvsdbPort = Tools.getIntegerProperty(properties, OVSDB_PORT);
        if (!Objects.equals(updatedOvsdbPort, ovsdbPort)) {
            ovsdbPort = updatedOvsdbPort;
        }

        log.info("Modified");
    }
    private void processPortAdded(Port port) {
        if (!port.getvNicType().equals(DIRECT)) {
            log.trace("processPortAdded skipped because of unsupported vNicType: {}", port.getvNicType());
            return;
        } else if (!port.isAdminStateUp() || port.getVifType().equals(UNBOUND)) {
            log.trace("processPortAdded skipped because of status: {}, adminStateUp: {}, vifType: {}",
                    port.getState(), port.isAdminStateUp(), port.getVifType());
            return;
        } else {
            InstancePort instancePort = instancePortService.instancePort(port.getId());
            //Skip this if the instance port for the port id is already created.
            if (instancePort != null) {
                return;
            }

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
            }

            log.trace("Retrieved interface name: {}", intfName);

            osNodeService.addVfPort(osNode.get(), intfName);
        }
    }

    private void processPortRemoved(Port port) {
        if (!port.getvNicType().equals(DIRECT)) {
            log.trace("processPortRemoved skipped because of unsupported vNicType: {}", port.getvNicType());
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
}
