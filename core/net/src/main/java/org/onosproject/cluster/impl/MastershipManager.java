/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.cluster.impl;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.metrics.MetricsService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.core.MetricsHelper;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipStore;
import org.onosproject.mastership.MastershipStoreDelegate;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.onlab.metrics.MetricsUtil.startTimer;
import static org.onlab.metrics.MetricsUtil.stopTimer;
import static org.onosproject.cluster.ControllerNode.State.ACTIVE;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppPermission.Type.*;



@Component(immediate = true)
@Service
public class MastershipManager
    extends AbstractListenerManager<MastershipEvent, MastershipListener>
    implements MastershipService, MastershipAdminService, MastershipTermService,
               MetricsHelper {

    private static final String NODE_ID_NULL = "Node ID cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String ROLE_NULL = "Mastership role cannot be null";

    private final Logger log = getLogger(getClass());

    private final MastershipStoreDelegate delegate = new InternalDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;

    private NodeId localNodeId;
    private Timer requestRoleTimer;

    @Activate
    public void activate() {
        requestRoleTimer = createTimer("Mastership", "requestRole", "responseTime");
        localNodeId = clusterService.getLocalNode().id();
        eventDispatcher.addSink(MastershipEvent.class, listenerRegistry);
        store.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(MastershipEvent.class);
        store.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public CompletableFuture<Void> setRole(NodeId nodeId, DeviceId deviceId, MastershipRole role) {
        checkNotNull(nodeId, NODE_ID_NULL);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(role, ROLE_NULL);

        CompletableFuture<MastershipEvent> eventFuture = null;

        switch (role) {
            case MASTER:
                eventFuture = store.setMaster(nodeId, deviceId);
                break;
            case STANDBY:
                eventFuture = store.setStandby(nodeId, deviceId);
                break;
            case NONE:
                eventFuture = store.relinquishRole(nodeId, deviceId);
                break;
            default:
                log.info("Unknown role; ignoring");
                return CompletableFuture.completedFuture(null);
        }

        return eventFuture.thenAccept(this::post)
                          .thenApply(v -> null);
    }

    @Override
    public MastershipRole getLocalRole(DeviceId deviceId) {
        checkPermission(CLUSTER_READ);

        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getRole(clusterService.getLocalNode().id(), deviceId);
    }

    @Override
    public CompletableFuture<Void> relinquishMastership(DeviceId deviceId) {
        checkPermission(CLUSTER_WRITE);
        return store.relinquishRole(localNodeId, deviceId)
                    .thenAccept(this::post)
                    .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<MastershipRole> requestRoleFor(DeviceId deviceId) {
        checkPermission(CLUSTER_WRITE);

        checkNotNull(deviceId, DEVICE_ID_NULL);
        final Context timer = startTimer(requestRoleTimer);
        return store.requestRole(deviceId).whenComplete((result, error) -> stopTimer(timer));

    }

    @Override
    public NodeId getMasterFor(DeviceId deviceId) {
        checkPermission(CLUSTER_READ);

        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getMaster(deviceId);
    }

    @Override
    public Set<DeviceId> getDevicesOf(NodeId nodeId) {
        checkPermission(CLUSTER_READ);

        checkNotNull(nodeId, NODE_ID_NULL);
        return store.getDevices(nodeId);
    }

    @Override
    public RoleInfo getNodesFor(DeviceId deviceId) {
        checkPermission(CLUSTER_READ);

        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getNodes(deviceId);
    }

    @Override
    public MastershipTerm getMastershipTerm(DeviceId deviceId) {
        return store.getTermFor(deviceId);
    }

    @Override
    public MetricsService metricsService() {
        return metricsService;
    }

    @Override
    public void balanceRoles() {
        List<ControllerNode> nodes = newArrayList(clusterService.getNodes());
        Map<ControllerNode, Set<DeviceId>> controllerDevices = new HashMap<>();
        int deviceCount = 0;

        // Create buckets reflecting current ownership.
        for (ControllerNode node : nodes) {
            if (clusterService.getState(node.id()) == ACTIVE) {
                Set<DeviceId> devicesOf = new HashSet<>(getDevicesOf(node.id()));
                deviceCount += devicesOf.size();
                controllerDevices.put(node, devicesOf);
                log.info("Node {} has {} devices.", node.id(), devicesOf.size());
            }
        }

        // Now re-balance the buckets until they are roughly even.
        List<CompletableFuture<Void>> balanceBucketsFutures = Lists.newLinkedList();
        int rounds = controllerDevices.keySet().size();
        for (int i = 0; i < rounds; i++) {
            // Iterate over the buckets and find the smallest and the largest.
            ControllerNode smallest = findBucket(true, controllerDevices);
            ControllerNode largest = findBucket(false, controllerDevices);
            balanceBucketsFutures.add(balanceBuckets(smallest, largest, controllerDevices, deviceCount));
        }
        CompletableFuture<Void> balanceRolesFuture = CompletableFuture.allOf(
                balanceBucketsFutures.toArray(new CompletableFuture[balanceBucketsFutures.size()]));

        Futures.getUnchecked(balanceRolesFuture);
    }

    private ControllerNode findBucket(boolean min,
                                      Map<ControllerNode, Set<DeviceId>>  controllerDevices) {
        int xSize = min ? Integer.MAX_VALUE : -1;
        ControllerNode xNode = null;
        for (ControllerNode node : controllerDevices.keySet()) {
            int size = controllerDevices.get(node).size();
            if ((min && size < xSize) || (!min && size > xSize)) {
                xSize = size;
                xNode = node;
            }
        }
        return xNode;
    }

    private CompletableFuture<Void> balanceBuckets(ControllerNode smallest, ControllerNode largest,
                                Map<ControllerNode, Set<DeviceId>>  controllerDevices,
                                int deviceCount) {
        Collection<DeviceId> minBucket = controllerDevices.get(smallest);
        Collection<DeviceId> maxBucket = controllerDevices.get(largest);
        int bucketCount = controllerDevices.keySet().size();

        int delta = (maxBucket.size() - minBucket.size()) / 2;
        delta = Math.min(deviceCount / bucketCount, delta);

        List<CompletableFuture<Void>> setRoleFutures = Lists.newLinkedList();

        if (delta > 0) {
            log.info("Attempting to move {} nodes from {} to {}...", delta,
                     largest.id(), smallest.id());

            int i = 0;
            Iterator<DeviceId> it = maxBucket.iterator();
            while (it.hasNext() && i < delta) {
                DeviceId deviceId = it.next();
                log.info("Setting {} as the master for {}", smallest.id(), deviceId);
                setRoleFutures.add(setRole(smallest.id(), deviceId, MASTER));
                controllerDevices.get(smallest).add(deviceId);
                it.remove();
                i++;
            }
        }

        return CompletableFuture.allOf(setRoleFutures.toArray(new CompletableFuture[setRoleFutures.size()]));
    }


    public class InternalDelegate implements MastershipStoreDelegate {
        @Override
        public void notify(MastershipEvent event) {
            post(event);
        }
    }

}
