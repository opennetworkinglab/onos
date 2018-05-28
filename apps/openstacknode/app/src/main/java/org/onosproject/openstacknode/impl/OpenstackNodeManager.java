/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknode.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstacknode.api.OpenstackNodeStore;
import org.onosproject.openstacknode.api.OpenstackNodeStoreDelegate;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.TpPort.tpPort;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.openstacknode.api.NodeState.COMPLETE;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.isOvsdbConnected;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service administering the inventory of openstack nodes.
 */
@Service
@Component(immediate = true)
public class OpenstackNodeManager extends ListenerRegistry<OpenstackNodeEvent, OpenstackNodeListener>
        implements OpenstackNodeService, OpenstackNodeAdminService {

    private final Logger log = getLogger(getClass());

    private static final String MSG_NODE = "OpenStack node %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";
    private static final String OVSDB_PORT = "ovsdbPortNum";
    private static final int DEFAULT_OVSDB_PORT = 6640;

    private static final String ERR_NULL_NODE = "OpenStack node cannot be null";
    private static final String ERR_NULL_HOSTNAME = "OpenStack node hostname cannot be null";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeStore osNodeStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController ovsdbController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Property(name = OVSDB_PORT, intValue = DEFAULT_OVSDB_PORT,
            label = "OVSDB server listen port")
    private int ovsdbPort = DEFAULT_OVSDB_PORT;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final OpenstackNodeStoreDelegate delegate = new InternalNodeStoreDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);
        osNodeStore.setDelegate(delegate);

        leadershipService.runForLeadership(appId.name());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osNodeStore.unsetDelegate(delegate);

        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

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

    @Override
    public void createNode(OpenstackNode osNode) {
        checkNotNull(osNode, ERR_NULL_NODE);
        osNodeStore.createNode(osNode);
        log.info(String.format(MSG_NODE, osNode.hostname(), MSG_CREATED));
    }

    @Override
    public void updateNode(OpenstackNode osNode) {
        checkNotNull(osNode, ERR_NULL_NODE);
        osNodeStore.updateNode(osNode);
        log.info(String.format(MSG_NODE, osNode.hostname(), MSG_UPDATED));
    }

    @Override
    public OpenstackNode removeNode(String hostname) {
        checkArgument(!Strings.isNullOrEmpty(hostname), ERR_NULL_HOSTNAME);
        OpenstackNode osNode = osNodeStore.removeNode(hostname);
        log.info(String.format(MSG_NODE, hostname, MSG_REMOVED));
        return osNode;
    }

    @Override
    public Set<OpenstackNode> nodes() {
        return osNodeStore.nodes();
    }

    @Override
    public Set<OpenstackNode> nodes(OpenstackNode.NodeType type) {
        Set<OpenstackNode> osNodes = osNodeStore.nodes().stream()
                .filter(osNode -> Objects.equals(osNode.type(), type))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osNodes);
    }

    @Override
    public Set<OpenstackNode> completeNodes() {
        Set<OpenstackNode> osNodes = osNodeStore.nodes().stream()
                .filter(osNode -> Objects.equals(osNode.state(), COMPLETE))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osNodes);
    }

    @Override
    public Set<OpenstackNode> completeNodes(OpenstackNode.NodeType type) {
        Set<OpenstackNode> osNodes = osNodeStore.nodes().stream()
                .filter(osNode -> osNode.type() == type &&
                        Objects.equals(osNode.state(), COMPLETE))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osNodes);
    }

    @Override
    public OpenstackNode node(String hostname) {
        return osNodeStore.node(hostname);
    }

    @Override
    public OpenstackNode node(DeviceId deviceId) {
        return osNodeStore.nodes().stream()
                .filter(osNode -> Objects.equals(osNode.intgBridge(), deviceId) ||
                        Objects.equals(osNode.ovsdb(), deviceId))
                .findFirst().orElse(null);
    }

    @Override
    public void addVfPort(OpenstackNode osNode, String portName) {
        log.trace("addVfPort called");

        if (!isOvsdbConnected(osNode, ovsdbPort, ovsdbController, deviceService)) {
            log.warn("There's no ovsdb connection with the device {}. Try to connect the device...",
                    osNode.ovsdb().toString());
            try {
                ovsdbController.connect(osNode.managementIp(), tpPort(ovsdbPort));
            } catch (Exception e) {
                log.error("Failed to connect to the openstackNode via ovsdb protocol because of exception {}",
                        e.toString());
            }
        }

        addOrRemoveSystemInterface(osNode, INTEGRATION_BRIDGE, portName, true);
    }

    @Override
    public void removeVfPort(OpenstackNode osNode, String portName) {
        log.trace("removeVfPort called");

        if (!isOvsdbConnected(osNode, ovsdbPort, ovsdbController, deviceService)) {
            log.warn("There's no ovsdb connection with the device {}. Try to connect the device...",
                    osNode.ovsdb().toString());
            try {
                ovsdbController.connect(osNode.managementIp(), tpPort(ovsdbPort));
            } catch (Exception e) {
                log.error("Failed to connect to the openstackNode via ovsdb protocol because of exception {}",
                        e.toString());
            }
        }

        addOrRemoveSystemInterface(osNode, INTEGRATION_BRIDGE, portName, false);
    }

    private void addOrRemoveSystemInterface(OpenstackNode osNode, String bridgeName, String intfName,
                                            boolean addOrRemove) {
        Device device = deviceService.getDevice(osNode.ovsdb());
        if (device == null || !device.is(BridgeConfig.class)) {
            log.info("device is null or this device if not ovsdb device");
            return;
        }
        BridgeConfig bridgeConfig =  device.as(BridgeConfig.class);
        if (addOrRemove) {
            bridgeConfig.addPort(BridgeName.bridgeName(bridgeName), intfName);
        } else {
            bridgeConfig.deletePort(BridgeName.bridgeName(bridgeName), intfName);
        }
    }

    private class InternalNodeStoreDelegate implements OpenstackNodeStoreDelegate {

        @Override
        public void notify(OpenstackNodeEvent event) {
            if (event != null) {
                log.trace("send openstack node event {}", event);
                process(event);
            }
        }
    }
}
