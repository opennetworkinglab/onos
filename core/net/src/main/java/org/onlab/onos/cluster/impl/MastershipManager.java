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
package org.onlab.onos.cluster.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.cluster.RoleInfo;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.mastership.MastershipAdminService;
import org.onlab.onos.mastership.MastershipEvent;
import org.onlab.onos.mastership.MastershipListener;
import org.onlab.onos.mastership.MastershipService;
import org.onlab.onos.mastership.MastershipStore;
import org.onlab.onos.mastership.MastershipStoreDelegate;
import org.onlab.onos.mastership.MastershipTerm;
import org.onlab.onos.mastership.MastershipTermService;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.slf4j.Logger;

@Component(immediate = true)
@Service
public class MastershipManager
implements MastershipService, MastershipAdminService {

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

    private ClusterEventListener clusterListener = new InternalClusterEventListener();

    @Activate
    public void activate() {
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
        return store.requestRole(deviceId);
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
    public MastershipTermService requestTermService() {
        return new InternalMastershipTermService();
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

    // FIXME: provide wiring to allow events to be triggered by changes within the store

    // Posts the specified event to the local event dispatcher.
    private void post(MastershipEvent event) {
        if (event != null && eventDispatcher != null) {
            eventDispatcher.post(event);
        }
    }

    private class InternalMastershipTermService implements MastershipTermService {

        @Override
        public MastershipTerm getMastershipTerm(DeviceId deviceId) {
            return store.getTermFor(deviceId);
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
                //FIXME: worry about addition when the time comes
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
//            else {
                //FIXME: break tie for equal-sized clusters, by number of
                //       connected switches, then masters, then nodeId hash
                //       problem is, how do we get at channel info cleanly here?
                //       Also, what's the time hit for a distributed store look-up
                //       versus channel re-negotiation? bet on the latter being worse.

//            }
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
