/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import org.onlab.metrics.MetricsService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.core.MetricsHelper;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetworkMastershipStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.event.AbstractVirtualListenerManager;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipStoreDelegate;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.slf4j.Logger;
import com.codahale.metrics.Timer;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.metrics.MetricsUtil.startTimer;
import static org.onlab.metrics.MetricsUtil.stopTimer;
import static org.slf4j.LoggerFactory.getLogger;

public class VirtualNetworkMastershipManager
        extends AbstractVirtualListenerManager<MastershipEvent, MastershipListener>
        implements MastershipService, MastershipAdminService, MastershipTermService,
        MetricsHelper {

    private static final String NODE_ID_NULL = "Node ID cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String ROLE_NULL = "Mastership role cannot be null";

    private final Logger log = getLogger(getClass());

    protected ClusterService clusterService;

    VirtualNetworkMastershipStore store;
    MastershipStoreDelegate storeDelegate;

    private NodeId localNodeId;
    private Timer requestRoleTimer;

    /**
     * Creates a new VirtualNetworkMastershipManager object.
     *
     * @param manager virtual network manager service
     * @param networkId virtual network identifier
     */
    public VirtualNetworkMastershipManager(VirtualNetworkService manager, NetworkId networkId) {
        super(manager, networkId, MastershipEvent.class);

        clusterService = serviceDirectory.get(ClusterService.class);

        store = serviceDirectory.get(VirtualNetworkMastershipStore.class);
        this.storeDelegate = new InternalDelegate();
        store.setDelegate(networkId, this.storeDelegate);

        requestRoleTimer = createTimer("Virtual-mastership", "requestRole", "responseTime");
        localNodeId = clusterService.getLocalNode().id();
    }

    @Override
    public CompletableFuture<Void> setRole(NodeId nodeId, DeviceId deviceId,
                                           MastershipRole role) {
        checkNotNull(nodeId, NODE_ID_NULL);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(role, ROLE_NULL);

        CompletableFuture<MastershipEvent> eventFuture = null;

        switch (role) {
            case MASTER:
                eventFuture = store.setMaster(networkId, nodeId, deviceId);
                break;
            case STANDBY:
                eventFuture = store.setStandby(networkId, nodeId, deviceId);
                break;
            case NONE:
                eventFuture = store.relinquishRole(networkId, nodeId, deviceId);
                break;
            default:
                log.info("Unknown role; ignoring");
                return CompletableFuture.completedFuture(null);
        }

        return eventFuture.thenAccept(this::post).thenApply(v -> null);
    }

    @Override
    public MastershipRole getLocalRole(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);

        return store.getRole(networkId, localNodeId, deviceId);
    }

    @Override
    public CompletableFuture<MastershipRole> requestRoleFor(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);

        final Timer.Context timer = startTimer(requestRoleTimer);
        return store.requestRole(networkId, deviceId)
                .whenComplete((result, error) -> stopTimer(timer));
    }

    @Override
    public CompletableFuture<Void> relinquishMastership(DeviceId deviceId) {
        return store.relinquishRole(networkId, localNodeId, deviceId)
                .thenAccept(this::post)
                .thenApply(v -> null);
    }

    @Override
    public NodeId getMasterFor(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);

        return store.getMaster(networkId, deviceId);
    }

    @Override
    public RoleInfo getNodesFor(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);

        return store.getNodes(networkId, deviceId);
    }

    @Override
    public Set<DeviceId> getDevicesOf(NodeId nodeId) {
        checkNotNull(nodeId, NODE_ID_NULL);

        return store.getDevices(networkId, nodeId);
    }

    @Override
    public MastershipTerm getMastershipTerm(DeviceId deviceId) {
        return store.getTermFor(networkId, deviceId);
    }

    @Override
    public MetricsService metricsService() {
        //TODO: support metric service for virtual network
        log.warn("Currently, virtual network does not support metric service.");
        return null;
    }

    @Override
    public void balanceRoles() {
        //FIXME: More advanced logic for balancing virtual network roles.
        List<ControllerNode> nodes = clusterService.getNodes().stream()
                .filter(n -> clusterService.getState(n.id())
                        .equals(ControllerNode.State.ACTIVE))
                .collect(Collectors.toList());

        nodes.sort(Comparator.comparing(ControllerNode::id));

        //Pick a node using network Id,
        NodeId masterNode = nodes.get((int) ((networkId.id() - 1) % nodes.size())).id();

        List<CompletableFuture<Void>> setRoleFutures = Lists.newLinkedList();
        for (VirtualDevice device : manager.getVirtualDevices(networkId)) {
            setRoleFutures.add(setRole(masterNode, device.id(), MastershipRole.MASTER));
        }

        CompletableFuture<Void> balanceRolesFuture = CompletableFuture.allOf(
                setRoleFutures.toArray(new CompletableFuture[setRoleFutures.size()]));

        Futures.getUnchecked(balanceRolesFuture);
    }

    public class InternalDelegate implements MastershipStoreDelegate {
        @Override
        public void notify(MastershipEvent event) {
            post(event);
        }
    }
}
