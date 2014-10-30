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
package org.onlab.onos.store.mastership.impl;

import static org.onlab.onos.mastership.MastershipEvent.Type.MASTER_CHANGED;
import static org.apache.commons.lang3.concurrent.ConcurrentUtils.putIfAbsent;

import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.cluster.RoleInfo;
import org.onlab.onos.mastership.MastershipEvent;
import org.onlab.onos.mastership.MastershipStore;
import org.onlab.onos.mastership.MastershipStoreDelegate;
import org.onlab.onos.mastership.MastershipTerm;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.store.hz.AbstractHazelcastStore;
import org.onlab.onos.store.hz.SMap;
import org.onlab.onos.store.serializers.KryoNamespaces;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.util.KryoNamespace;

import com.google.common.collect.ImmutableSet;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.MapEvent;

import static org.onlab.onos.net.MastershipRole.*;

/**
 * Distributed implementation of the mastership store. The store is
 * responsible for the master selection process.
 */
@Component(immediate = true)
@Service
public class DistributedMastershipStore
extends AbstractHazelcastStore<MastershipEvent, MastershipStoreDelegate>
implements MastershipStore {

    //initial term/TTL value
    private static final Integer INIT = 0;

    //device to node roles
    protected SMap<DeviceId, RoleValue> roleMap;
    //devices to terms
    protected SMap<DeviceId, Integer> terms;
    //last-known cluster size, used for tie-breaking when partitioning occurs
    protected IAtomicLong clusterSize;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Override
    @Activate
    public void activate() {
        super.activate();

        this.serializer = new KryoSerializer() {
            @Override
            protected void setupKryoPool() {
                serializerPool = KryoNamespace.newBuilder()
                        .register(KryoNamespaces.API)

                        .register(RoleValue.class, new RoleValueSerializer())
                        .build()
                        .populate(1);
            }
        };

        roleMap = new SMap<>(theInstance.<byte[], byte[]>getMap("nodeRoles"), this.serializer);
        roleMap.addEntryListener((new RemoteMasterShipEventHandler()), true);
        terms = new SMap<>(theInstance.<byte[], byte[]>getMap("terms"), this.serializer);
        clusterSize = theInstance.getAtomicLong("clustersize");

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public MastershipRole getRole(NodeId nodeId, DeviceId deviceId) {
        final RoleValue roleInfo = getRoleValue(deviceId);
        if (roleInfo.contains(MASTER, nodeId)) {
            return MASTER;
        }
        if (roleInfo.contains(STANDBY, nodeId)) {
            return STANDBY;
        }
        return NONE;
    }

    @Override
    public MastershipEvent setMaster(NodeId nodeId, DeviceId deviceId) {

        MastershipRole role = getRole(nodeId, deviceId);
        roleMap.lock(deviceId);
        try {
            RoleValue rv = getRoleValue(deviceId);
            switch (role) {
                case MASTER:
                    //reinforce mastership
                    rv.reassign(nodeId, STANDBY, NONE);
                    roleMap.put(deviceId, rv);
                    return null;
                case STANDBY:
                case NONE:
                    NodeId current = rv.get(MASTER);
                    if (current != null) {
                        //backup and replace current master
                        rv.reassign(current, NONE, STANDBY);
                        rv.replace(current, nodeId, MASTER);
                    } else {
                        //no master before so just add.
                        rv.add(MASTER, nodeId);
                    }
                    rv.reassign(nodeId, STANDBY, NONE);
                    roleMap.put(deviceId, rv);
                    updateTerm(deviceId);
                    return new MastershipEvent(MASTER_CHANGED, deviceId, rv.roleInfo());
                default:
                    log.warn("unknown Mastership Role {}", role);
                    return null;
            }
        } finally {
            roleMap.unlock(deviceId);
        }
    }

    @Override
    public NodeId getMaster(DeviceId deviceId) {
        return getNode(MASTER, deviceId);
    }


    @Override
    public RoleInfo getNodes(DeviceId deviceId) {
        roleMap.lock(deviceId);
        try {
            RoleValue rv = getRoleValue(deviceId);
            return rv.roleInfo();
        } finally {
            roleMap.unlock(deviceId);
        }
    }

    @Override
    public Set<DeviceId> getDevices(NodeId nodeId) {
        ImmutableSet.Builder<DeviceId> builder = ImmutableSet.builder();

        for (Map.Entry<DeviceId, RoleValue> el : roleMap.entrySet()) {
            if (nodeId.equals(el.getValue().get(MASTER))) {
                builder.add(el.getKey());
            }
        }

        return builder.build();
    }

    @Override
    public MastershipRole requestRole(DeviceId deviceId) {
        NodeId local = clusterService.getLocalNode().id();

        roleMap.lock(deviceId);
        try {
            RoleValue rv = getRoleValue(deviceId);
            MastershipRole role = getRole(local, deviceId);
            switch (role) {
                case MASTER:
                    rv.reassign(local, STANDBY, NONE);
                    terms.putIfAbsent(deviceId, INIT);
                    roleMap.put(deviceId, rv);
                    break;
                case STANDBY:
                    rv.reassign(local, NONE, STANDBY);
                    roleMap.put(deviceId, rv);
                    terms.putIfAbsent(deviceId, INIT);
                    break;
                case NONE:
                    //either we're the first standby, or first to device.
                    //for latter, claim mastership.
                    if (rv.get(MASTER) == null) {
                        rv.add(MASTER, local);
                        rv.reassign(local, STANDBY, NONE);
                        updateTerm(deviceId);
                        role = MastershipRole.MASTER;
                    } else {
                        rv.add(STANDBY, local);
                        rv.reassign(local, NONE, STANDBY);
                        role = MastershipRole.STANDBY;
                    }
                    roleMap.put(deviceId, rv);
                    break;
                default:
                    log.warn("unknown Mastership Role {}", role);
            }
            return role;
        } finally {
            roleMap.unlock(deviceId);
        }
    }

    @Override
    public MastershipTerm getTermFor(DeviceId deviceId) {
        RoleValue rv = getRoleValue(deviceId);
        if ((rv.get(MASTER) == null) || (terms.get(deviceId) == null)) {
            return null;
        }
        return MastershipTerm.of(rv.get(MASTER), terms.get(deviceId));
    }

    @Override
    public MastershipEvent setStandby(NodeId nodeId, DeviceId deviceId) {
        MastershipEvent event = null;

        roleMap.lock(deviceId);
        try {
            RoleValue rv = getRoleValue(deviceId);
            MastershipRole role = getRole(nodeId, deviceId);
            switch (role) {
                case MASTER:
                    event = reelect(nodeId, deviceId, rv);
                    //fall through to reinforce role
                case STANDBY:
                    //fall through to reinforce role
                case NONE:
                    rv.reassign(nodeId, NONE, STANDBY);
                    roleMap.put(deviceId, rv);
                    break;
                default:
                    log.warn("unknown Mastership Role {}", role);
            }
            return event;
        } finally {
            roleMap.unlock(deviceId);
        }
    }

    @Override
    public MastershipEvent relinquishRole(NodeId nodeId, DeviceId deviceId) {
        MastershipEvent event = null;

        roleMap.lock(deviceId);
        try {
            RoleValue rv = getRoleValue(deviceId);
            MastershipRole role = getRole(nodeId, deviceId);
            switch (role) {
                case MASTER:
                    event = reelect(nodeId, deviceId, rv);
                    if (event != null) {
                        updateTerm(deviceId);
                    }
                    //fall through to reinforce relinquishment
                case STANDBY:
                    //fall through to reinforce relinquishment
                case NONE:
                    rv.reassign(nodeId, STANDBY, NONE);
                    roleMap.put(deviceId, rv);
                    break;
                default:
                log.warn("unknown Mastership Role {}", role);
            }
            return event;
        } finally {
            roleMap.unlock(deviceId);
        }
    }

    //helper to fetch a new master candidate for a given device.
    private MastershipEvent reelect(
            NodeId current, DeviceId deviceId, RoleValue rv) {

        //if this is an queue it'd be neater.
        NodeId backup = null;
        for (NodeId n : rv.nodesOfRole(STANDBY)) {
            if (!current.equals(n)) {
                backup = n;
                break;
            }
        }

        if (backup == null) {
            log.info("{} giving up and going to NONE for {}", current, deviceId);
            rv.remove(MASTER, current);
            return null;
        } else {
            log.info("{} trying to pass mastership for {} to {}", current, deviceId, backup);
            rv.replace(current, backup, MASTER);
            rv.reassign(backup, STANDBY, NONE);
            return new MastershipEvent(MASTER_CHANGED, deviceId, rv.roleInfo());
        }
    }

    //return the RoleValue structure for a device, or create one
    private RoleValue getRoleValue(DeviceId deviceId) {
        RoleValue value = roleMap.get(deviceId);
        if (value == null) {
            value = new RoleValue();
            RoleValue concurrentlyAdded = roleMap.putIfAbsent(deviceId, value);
            if (concurrentlyAdded != null) {
                return concurrentlyAdded;
            }
        }
        return value;
    }

    //get first applicable node out of store-unique structure.
    private NodeId getNode(MastershipRole role, DeviceId deviceId) {
        RoleValue value = roleMap.get(deviceId);
        if (value != null) {
            return value.get(role);
        }
        return null;
    }

    //adds or updates term information.
    private void updateTerm(DeviceId deviceId) {
        Integer term = terms.get(deviceId);
        if (term == null) {
            term = terms.putIfAbsent(deviceId, INIT);
            if (term == null) {
                // initial term set successfully
                return;
            }
            // concurrent initialization detected,
            // fall through to try incrementing
        }
        Integer nextTerm = term + 1;
        boolean success = terms.replace(deviceId, term, nextTerm);
        while (!success) {
            term = terms.get(deviceId);
            if (term == null) {
                // something is very wrong, but write something to avoid
                // infinite loop.
                log.warn("Term info for {} disappeared.", deviceId);
                term = putIfAbsent(terms, deviceId, nextTerm);
            }
            nextTerm = term + 1;
            success = terms.replace(deviceId, term, nextTerm);
        }
    }

    private class RemoteMasterShipEventHandler implements EntryListener<DeviceId, RoleValue> {

        @Override
        public void entryAdded(EntryEvent<DeviceId, RoleValue> event) {
            entryUpdated(event);
        }

        @Override
        public void entryRemoved(EntryEvent<DeviceId, RoleValue> event) {
        }

        @Override
        public void entryUpdated(EntryEvent<DeviceId, RoleValue> event) {
            notifyDelegate(new MastershipEvent(
                    MASTER_CHANGED, event.getKey(), event.getValue().roleInfo()));
        }

        @Override
        public void entryEvicted(EntryEvent<DeviceId, RoleValue> event) {
        }

        @Override
        public void mapEvicted(MapEvent event) {
        }

        @Override
        public void mapCleared(MapEvent event) {
        }
    }

}
