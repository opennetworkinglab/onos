/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeAdminService;
import org.onosproject.kubevirtnode.api.KubevirtNodeEvent;
import org.onosproject.kubevirtnode.api.KubevirtNodeListener;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.kubevirtnode.api.KubevirtNodeState;
import org.onosproject.kubevirtnode.api.KubevirtNodeStore;
import org.onosproject.kubevirtnode.api.KubevirtNodeStoreDelegate;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
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
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.kubevirtnode.api.Constants.TUNNEL_BRIDGE;
import static org.onosproject.kubevirtnode.impl.OsgiPropertyConstants.OVSDB_PORT;
import static org.onosproject.kubevirtnode.impl.OsgiPropertyConstants.OVSDB_PORT_NUM_DEFAULT;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.genDpidFromName;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service administering the inventory of kubevirt nodes.
 */
@Component(
        immediate = true,
        service = {KubevirtNodeService.class, KubevirtNodeAdminService.class},
        property = {
                OVSDB_PORT + ":Integer=" + OVSDB_PORT_NUM_DEFAULT
        }
)
public class KubevirtNodeManager
        extends ListenerRegistry<KubevirtNodeEvent, KubevirtNodeListener>
        implements KubevirtNodeService, KubevirtNodeAdminService {

    private final Logger log = getLogger(getClass());

    private static final String MSG_NODE = "KubeVirt node %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String DEVICE_ID_COUNTER_NAME = "device-id-counter";

    private static final String ERR_NULL_NODE = "KubeVirt node cannot be null";
    private static final String ERR_NULL_HOSTNAME = "KubeVirt node hostname cannot be null";
    private static final String ERR_NULL_DEVICE_ID = "KubeVirt node device ID cannot be null";

    private static final String NOT_DUPLICATED_MSG = "% cannot be duplicated";

    private static final String OF_PREFIX = "of:";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeStore nodeStore;

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

    /**
     * OVSDB server listen port.
     */
    private int ovsdbPortNum = OVSDB_PORT_NUM_DEFAULT;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final KubevirtNodeStoreDelegate delegate = new InternalNodeStoreDelegate();

    private AtomicCounter deviceIdCounter;

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);
        nodeStore.setDelegate(delegate);

        leadershipService.runForLeadership(appId.name());

        deviceIdCounter = storageService.getAtomicCounter(DEVICE_ID_COUNTER_NAME);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        nodeStore.unsetDelegate(delegate);

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
    public void createNode(KubevirtNode node) {
        checkNotNull(node, ERR_NULL_NODE);

        KubevirtNode intNode;
        KubevirtNode tunNode;

        if (node.intgBridge() == null) {
            String deviceIdStr = genDpidFromName(INTEGRATION_BRIDGE + "-" + node.hostname());
            checkNotNull(deviceIdStr, ERR_NULL_DEVICE_ID);
            intNode = node.updateIntgBridge(DeviceId.deviceId(deviceIdStr));
            checkArgument(!hasIntgBridge(intNode.intgBridge(), intNode.hostname()),
                    NOT_DUPLICATED_MSG, intNode.intgBridge());
        } else {
            intNode = node;
            checkArgument(!hasIntgBridge(intNode.intgBridge(), intNode.hostname()),
                    NOT_DUPLICATED_MSG, intNode.intgBridge());
        }

        if (node.tunBridge() == null) {
            String deviceIdStr = genDpidFromName(TUNNEL_BRIDGE + "-" + node.hostname());
            checkNotNull(deviceIdStr, ERR_NULL_DEVICE_ID);
            tunNode = intNode.updateTunBridge(DeviceId.deviceId(deviceIdStr));
            checkArgument(!hasTunBridge(tunNode.tunBridge(), tunNode.hostname()),
                    NOT_DUPLICATED_MSG, tunNode.tunBridge());
        } else {
            tunNode = intNode;
            checkArgument(!hasTunBridge(tunNode.tunBridge(), tunNode.hostname()),
                    NOT_DUPLICATED_MSG, tunNode.tunBridge());
        }

        nodeStore.createNode(tunNode);

        log.info(String.format(MSG_NODE, tunNode.hostname(), MSG_CREATED));
    }

    @Override
    public void updateNode(KubevirtNode node) {
        checkNotNull(node, ERR_NULL_NODE);

        KubevirtNode intNode;
        KubevirtNode tunNode;

        KubevirtNode existingNode = nodeStore.node(node.hostname());
        checkNotNull(existingNode, ERR_NULL_NODE);

        DeviceId existIntgBridge = nodeStore.node(node.hostname()).intgBridge();

        if (node.intgBridge() == null) {
            intNode = node.updateIntgBridge(existIntgBridge);
            checkArgument(!hasIntgBridge(intNode.intgBridge(), intNode.hostname()),
                    NOT_DUPLICATED_MSG, intNode.intgBridge());
        } else {
            intNode = node;
            checkArgument(!hasIntgBridge(intNode.intgBridge(), intNode.hostname()),
                    NOT_DUPLICATED_MSG, intNode.intgBridge());
        }

        DeviceId existTunBridge = nodeStore.node(node.hostname()).tunBridge();
        if (intNode.tunBridge() == null) {
            tunNode = intNode.updateTunBridge(existTunBridge);
            checkArgument(!hasTunBridge(tunNode.tunBridge(), tunNode.hostname()),
                    NOT_DUPLICATED_MSG, tunNode.tunBridge());
        } else {
            tunNode = intNode;
            checkArgument(!hasTunBridge(tunNode.tunBridge(), tunNode.hostname()),
                    NOT_DUPLICATED_MSG, tunNode.tunBridge());
        }
        nodeStore.updateNode(tunNode);

        log.info(String.format(MSG_NODE, tunNode.hostname(), MSG_UPDATED));
    }

    @Override
    public KubevirtNode removeNode(String hostname) {
        checkArgument(!Strings.isNullOrEmpty(hostname), ERR_NULL_HOSTNAME);
        KubevirtNode node = nodeStore.removeNode(hostname);
        log.info(String.format(MSG_NODE, hostname, MSG_REMOVED));
        return node;
    }

    @Override
    public Set<KubevirtNode> nodes() {
        return nodeStore.nodes();
    }

    @Override
    public Set<KubevirtNode> nodes(KubevirtNode.Type type) {
        Set<KubevirtNode> nodes = nodeStore.nodes().stream()
                .filter(node -> Objects.equals(node.type(), type))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(nodes);
    }

    @Override
    public Set<KubevirtNode> completeNodes() {
        Set<KubevirtNode> nodes = nodeStore.nodes().stream()
                .filter(node -> node.state() == KubevirtNodeState.COMPLETE)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(nodes);
    }

    @Override
    public Set<KubevirtNode> completeNodes(KubevirtNode.Type type) {
        Set<KubevirtNode> nodes = nodeStore.nodes().stream()
                .filter(node -> node.type() == type &&
                        node.state() == KubevirtNodeState.COMPLETE)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(nodes);
    }

    @Override
    public KubevirtNode node(String hostname) {
        return nodeStore.node(hostname);
    }

    @Override
    public KubevirtNode node(DeviceId deviceId) {
        return nodeStore.nodes().stream()
                .filter(node -> Objects.equals(node.intgBridge(), deviceId) ||
                        Objects.equals(node.ovsdb(), deviceId))
                .findFirst().orElse(null);
    }

    @Override
    public KubevirtNode node(IpAddress mgmtIp) {
        return nodeStore.nodes().stream()
                .filter(node -> Objects.equals(node.managementIp(), mgmtIp))
                .findFirst().orElse(null);
    }

    @Override
    public boolean hasNode(String hostname) {
        return nodeStore.nodes().stream().anyMatch(n -> n.hostname().equals(hostname));
    }

    @Override
    public KubevirtNode nodeByTunBridge(DeviceId deviceId) {
        return nodeStore.nodes().stream()
                .filter(node -> Objects.equals(node.tunBridge(), deviceId))
                .findFirst().orElse(null);
    }

    @Override
    public KubevirtNode nodeByPhyBridge(DeviceId deviceId) {
        return nodeStore.nodes().stream()
                .filter(node -> hasPhyBridge(node, deviceId))
                .findAny()
                .orElse(null);
    }

    private boolean hasPhyBridge(KubevirtNode node, DeviceId deviceId) {
        return node.phyIntfs().stream()
                .filter(phyIntf -> phyIntf.physBridge().equals(deviceId))
                .findAny()
                .isPresent();
    }

    private boolean hasIntgBridge(DeviceId deviceId, String hostname) {
        Optional<KubevirtNode> existNode = nodeStore.nodes().stream()
                .filter(n -> !n.hostname().equals(hostname))
                .filter(n -> deviceId.equals(n.intgBridge()))
                .findFirst();

        return existNode.isPresent();
    }

    private boolean hasTunBridge(DeviceId deviceId, String hostname) {
        Optional<KubevirtNode> existNode = nodeStore.nodes().stream()
                .filter(n -> !n.hostname().equals(hostname))
                .filter(n -> deviceId.equals(n.tunBridge()))
                .findFirst();

        return existNode.isPresent();
    }

    private class InternalNodeStoreDelegate implements KubevirtNodeStoreDelegate {

        @Override
        public void notify(KubevirtNodeEvent event) {
            if (event != null) {
                log.trace("send kubevirt node event {}", event);
                process(event);
            }
        }
    }
}
