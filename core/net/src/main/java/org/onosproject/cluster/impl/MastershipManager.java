/*
 * Copyright 2014 Open Networking Laboratory
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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.metrics.MetricsService;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.core.MetricsHelper;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
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
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.onlab.metrics.MetricsUtil.startTimer;
import static org.onlab.metrics.MetricsUtil.stopTimer;
import static org.onosproject.cluster.ControllerNode.State.ACTIVE;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
@Service
public class MastershipManager
    implements MastershipService, MastershipAdminService, MastershipTermService,
               MetricsHelper {

    private static final String NODE_ID_NULL = "Node ID cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String ROLE_NULL = "Mastership role cannot be null";

    private final Logger log = getLogger(getClass());

    protected final AbstractListenerRegistry<MastershipEvent, MastershipListener>
    listenerRegistry = new AbstractListenerRegistry<>();

    private final MastershipStoreDelegate delegate = new InternalDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;

    private ClusterEventListener clusterListener = new InternalClusterEventListener();
    private Timer requestRoleTimer;

    @Activate
    public void activate() {
        requestRoleTimer = createTimer("Mastership", "requestRole", "responseTime");

        eventDispatcher.addSink(MastershipEvent.class, listenerRegistry);
        clusterService.addListener(clusterListener);
        store.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(MastershipEvent.class);
        clusterService.removeListener(clusterListener);
        store.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void setRole(NodeId nodeId, DeviceId deviceId, MastershipRole role) {
        checkNotNull(nodeId, NODE_ID_NULL);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(role, ROLE_NULL);

        MastershipEvent event = null;

        switch (role) {
            case MASTER:
                event = store.setMaster(nodeId, deviceId);
                break;
            case STANDBY:
                event = store.setStandby(nodeId, deviceId);
                break;
            case NONE:
                event = store.relinquishRole(nodeId, deviceId);
                break;
            default:
                log.info("Unknown role; ignoring");
                return;
        }

        if (event != null) {
            post(event);
        }
    }

    @Override
    public MastershipRole getLocalRole(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getRole(clusterService.getLocalNode().id(), deviceId);
    }

    @Override
    public void relinquishMastership(DeviceId deviceId) {
        MastershipEvent event = null;
        event = store.relinquishRole(
                clusterService.getLocalNode().id(), deviceId);
        if (event != null) {
            post(event);
        }
    }

    @Override
    public MastershipRole requestRoleFor(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        final Context timer = startTimer(requestRoleTimer);
        try {
            return store.requestRole(deviceId);
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public NodeId getMasterFor(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getMaster(deviceId);
    }

    @Override
    public Set<DeviceId> getDevicesOf(NodeId nodeId) {
        checkNotNull(nodeId, NODE_ID_NULL);
        return store.getDevices(nodeId);
    }

    @Override
    public RoleInfo getNodesFor(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getNodes(deviceId);
    }

    @Override
    public MastershipTerm getMastershipTerm(DeviceId deviceId) {
        return store.getTermFor(deviceId);
    }

    @Override
    public void addListener(MastershipListener listener) {
        checkNotNull(listener);
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(MastershipListener listener) {
        checkNotNull(listener);
        listenerRegistry.removeListener(listener);
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
        int rounds = controllerDevices.keySet().size();
        for (int i = 0; i < rounds; i++) {
            // Iterate over the buckets and find the smallest and the largest.
            ControllerNode smallest = findBucket(true, controllerDevices);
            ControllerNode largest = findBucket(false, controllerDevices);
            balanceBuckets(smallest, largest, controllerDevices, deviceCount);
        }
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

    private void balanceBuckets(ControllerNode smallest, ControllerNode largest,
                                Map<ControllerNode, Set<DeviceId>>  controllerDevices,
                                int deviceCount) {
        Collection<DeviceId> minBucket = controllerDevices.get(smallest);
        Collection<DeviceId> maxBucket = controllerDevices.get(largest);
        int bucketCount = controllerDevices.keySet().size();

        int delta = (maxBucket.size() - minBucket.size()) / 2;
        delta = Math.min(deviceCount / bucketCount, delta);

        if (delta > 0) {
            log.info("Attempting to move {} nodes from {} to {}...", delta,
                     largest.id(), smallest.id());

            int i = 0;
            Iterator<DeviceId> it = maxBucket.iterator();
            while (it.hasNext() && i < delta) {
                DeviceId deviceId = it.next();
                log.info("Setting {} as the master for {}", smallest.id(), deviceId);
                setRole(smallest.id(), deviceId, MASTER);
                controllerDevices.get(smallest).add(deviceId);
                it.remove();
                i++;
            }
        }
    }


    // Posts the specified event to the local event dispatcher.
    private void post(MastershipEvent event) {
        if (event != null && eventDispatcher != null) {
            eventDispatcher.post(event);
        }
    }

    //callback for reacting to cluster events
    private class InternalClusterEventListener implements ClusterEventListener {

        // A notion of a local maximum cluster size, used to tie-break.
        // Think of a better way to do this.
        private AtomicInteger clusterSize;

        InternalClusterEventListener() {
            clusterSize = new AtomicInteger(0);
        }

        @Override
        public void event(ClusterEvent event) {
            switch (event.type()) {
                case INSTANCE_ADDED:
                case INSTANCE_ACTIVATED:
                    clusterSize.incrementAndGet();
                    log.info("instance {} added/activated", event.subject());
                    break;
                case INSTANCE_REMOVED:
                case INSTANCE_DEACTIVATED:
                    ControllerNode node = event.subject();

                    if (node.equals(clusterService.getLocalNode())) {
                        //If we are in smaller cluster, relinquish and return
                        for (DeviceId device : getDevicesOf(node.id())) {
                            if (!isInMajority()) {
                                //own DeviceManager should catch event and tell switch
                                store.relinquishRole(node.id(), device);
                            }
                        }
                        log.info("broke off from cluster, relinquished devices");
                        break;
                    }

                    // if we are the larger one and the removed node(s) are brain dead,
                    // force relinquish on behalf of disabled node.
                    // check network channel to do this?
                    for (DeviceId device : getDevicesOf(node.id())) {
                        //some things to check:
                        // 1. we didn't break off as well while we're at it
                        // 2. others don't pile in and try too - maybe a lock
                        if (isInMajority()) {
                            store.relinquishRole(node.id(), device);
                        }
                    }
                    clusterSize.decrementAndGet();
                    log.info("instance {} removed/deactivated", event.subject());
                    break;
                default:
                    log.warn("unknown cluster event {}", event);
            }
        }

        private boolean isInMajority() {
            if (clusterService.getNodes().size() > (clusterSize.intValue() / 2)) {
                return true;
            }

             //FIXME: break tie for equal-sized clusters,

            return false;
        }

    }

    public class InternalDelegate implements MastershipStoreDelegate {

        @Override
        public void notify(MastershipEvent event) {
            log.trace("dispatching mastership event {}", event);
            eventDispatcher.post(event);
        }

    }

}
