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
import org.onlab.packet.IpAddress;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstacknode.api.OpenstackNodeStore;
import org.onosproject.openstacknode.api.OpenstackNodeStoreDelegate;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.TpPort.tpPort;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.openstacknode.api.NodeState.COMPLETE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.CONTROLLER;
import static org.onosproject.openstacknode.impl.OsgiPropertyConstants.OVSDB_PORT;
import static org.onosproject.openstacknode.impl.OsgiPropertyConstants.OVSDB_PORT_NUM_DEFAULT;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.addOrRemoveSystemInterface;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.genDpid;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.isOvsdbConnected;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service administering the inventory of openstack nodes.
 */
@Component(
    immediate = true,
    service = { OpenstackNodeService.class, OpenstackNodeAdminService.class },
    property = {
        OVSDB_PORT + ":Integer=" + OVSDB_PORT_NUM_DEFAULT
    }
)
public class OpenstackNodeManager
        extends ListenerRegistry<OpenstackNodeEvent, OpenstackNodeListener>
        implements OpenstackNodeService, OpenstackNodeAdminService {

    private final Logger log = getLogger(getClass());

    private static final String MSG_NODE = "OpenStack node %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String DEVICE_ID_COUNTER_NAME = "device-id-counter";

    private static final String ERR_NULL_NODE = "OpenStack node cannot be null";
    private static final String ERR_NULL_HOSTNAME = "OpenStack node hostname cannot be null";
    private static final String ERR_NULL_DEVICE_ID = "OpenStack node device ID cannot be null";

    private static final String NOT_DUPLICATED_MSG = "% cannot be duplicated";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeStore osNodeStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OvsdbController ovsdbController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    /** OVSDB server listen port. */
    private int ovsdbPortNum = OVSDB_PORT_NUM_DEFAULT;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final OpenstackNodeStoreDelegate delegate = new InternalNodeStoreDelegate();

    private AtomicCounter deviceIdCounter;

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);
        osNodeStore.setDelegate(delegate);

        leadershipService.runForLeadership(appId.name());

        deviceIdCounter = storageService.getAtomicCounter(DEVICE_ID_COUNTER_NAME);

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
        if (!Objects.equals(updatedOvsdbPort, ovsdbPortNum)) {
            ovsdbPortNum = updatedOvsdbPort;
        }

        log.info("Modified");
    }

    @Override
    public void createNode(OpenstackNode osNode) {
        checkNotNull(osNode, ERR_NULL_NODE);

        OpenstackNode updatedNode;

        if (osNode.intgBridge() == null && osNode.type() != CONTROLLER) {
            String deviceIdStr = genDpid(deviceIdCounter.incrementAndGet());
            checkNotNull(deviceIdStr, ERR_NULL_DEVICE_ID);
            updatedNode = osNode.updateIntbridge(DeviceId.deviceId(deviceIdStr));
            checkArgument(!hasIntgBridge(updatedNode.intgBridge(), updatedNode.hostname()),
                                NOT_DUPLICATED_MSG, updatedNode.intgBridge());
        } else {
            updatedNode = osNode;
            checkArgument(!hasIntgBridge(updatedNode.intgBridge(), updatedNode.hostname()),
                                NOT_DUPLICATED_MSG, updatedNode.intgBridge());
        }

        osNodeStore.createNode(updatedNode);

        log.info(String.format(MSG_NODE, osNode.hostname(), MSG_CREATED));
    }

    @Override
    public void updateNode(OpenstackNode osNode) {
        checkNotNull(osNode, ERR_NULL_NODE);

        OpenstackNode updatedNode;

        OpenstackNode existingNode = osNodeStore.node(osNode.hostname());
        checkNotNull(existingNode, ERR_NULL_NODE);

        DeviceId existDeviceId = osNodeStore.node(osNode.hostname()).intgBridge();

        if (vlanIntfChanged(existingNode, osNode) ||
                physicalIntfChanged(existingNode, osNode) ||
                dpdkIntfChanged(existingNode, osNode)) {

            removeNode(osNode.hostname());

            //we wait 1 second for ovsdb client completely to do removal job
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Exception occurred because of {}", e);
            }

            if (!intfsRemovedFromExistNode(existingNode)) {
                log.error("Updated node failed because intfs of existingNode {} " +
                                "are not removed properly", existingNode.toString());
                return;
            }

            createNode(osNode);
            return;
        }

        if (osNode.intgBridge() == null && osNode.type() != CONTROLLER) {
            updatedNode = osNode.updateIntbridge(existDeviceId);
            checkArgument(!hasIntgBridge(updatedNode.intgBridge(), updatedNode.hostname()),
                    NOT_DUPLICATED_MSG, updatedNode.intgBridge());
        } else {
            updatedNode = osNode;
            checkArgument(!hasIntgBridge(updatedNode.intgBridge(), updatedNode.hostname()),
                    NOT_DUPLICATED_MSG, updatedNode.intgBridge());
        }

        osNodeStore.updateNode(updatedNode);

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
    public OpenstackNode node(IpAddress mgmtIp) {
        return osNodeStore.nodes().stream()
                .filter(osNode -> Objects.equals(osNode.managementIp(), mgmtIp))
                .findFirst().orElse(null);
    }

    @Override
    public void addVfPort(OpenstackNode osNode, String portName) {
        log.trace("addVfPort called");

        connectSwitch(osNode);

        addOrRemoveSystemInterface(osNode, INTEGRATION_BRIDGE, portName,
                                    deviceService, true);
    }

    @Override
    public void removeVfPort(OpenstackNode osNode, String portName) {
        log.trace("removeVfPort called");

        connectSwitch(osNode);

        addOrRemoveSystemInterface(osNode, INTEGRATION_BRIDGE, portName,
                                    deviceService, false);
    }

    private boolean intfsRemovedFromExistNode(OpenstackNode osNode) {
        if (osNode.vlanIntf() != null &&
                !intfRemoved(osNode.vlanIntf(), osNode.intgBridge())) {
            return false;
        }

        if (osNode.phyIntfs().stream().anyMatch(phyInterface ->
                !intfRemoved(phyInterface.intf(), osNode.intgBridge()))) {
            return false;
        }

        if (osNode.dpdkConfig() != null &&
                osNode.dpdkConfig().dpdkIntfs().stream().anyMatch(dpdkInterface ->
                        !intfRemoved(dpdkInterface.intf(), osNode.intgBridge()))) {
            return false;
        }

        return true;
    }

    private boolean intfRemoved(String intf, DeviceId deviceId) {
        return !deviceService.getPorts(deviceId).stream()
                .anyMatch(port -> port.annotations().value(PORT_NAME).equals(intf));
    }

    private boolean vlanIntfChanged(OpenstackNode oldNode, OpenstackNode newNode) {
        return !Objects.equals(oldNode.vlanIntf(), newNode.vlanIntf());
    }

    private boolean physicalIntfChanged(OpenstackNode oldNode, OpenstackNode newNode) {
        return !Objects.equals(oldNode.phyIntfs(), newNode.phyIntfs());
    }

    private boolean dpdkIntfChanged(OpenstackNode oldNode, OpenstackNode newNode) {
        return !Objects.equals(oldNode.dpdkConfig(), newNode.dpdkConfig());
    }

    private void connectSwitch(OpenstackNode osNode) {
        if (!isOvsdbConnected(osNode, ovsdbPortNum, ovsdbController, deviceService)) {
            log.warn("There's no ovsdb connection with the device {}. Try to connect the device...",
                    osNode.ovsdb().toString());
            try {
                ovsdbController.connect(osNode.managementIp(), tpPort(ovsdbPortNum));
            } catch (Exception e) {
                log.error("Failed to connect to the openstackNode via ovsdb " +
                                "protocol because of exception {}", e);
            }
        }
    }

    private boolean hasIntgBridge(DeviceId deviceId, String hostname) {
        Optional<OpenstackNode> existNode = osNodeStore.nodes().stream()
                .filter(n -> n.type() != CONTROLLER)
                .filter(n -> !n.hostname().equals(hostname))
                .filter(n -> n.intgBridge().equals(deviceId))
                .findFirst();

        return existNode.isPresent();
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
