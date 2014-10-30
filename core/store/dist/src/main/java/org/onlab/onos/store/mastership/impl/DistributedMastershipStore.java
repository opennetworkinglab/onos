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

import java.util.HashSet;
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
        final RoleValue roleInfo = roleMap.get(deviceId);
        if (roleInfo != null) {
            return roleInfo.getRole(nodeId);
        }
        return NONE;
    }

    @Override
    public MastershipEvent setMaster(NodeId newMaster, DeviceId deviceId) {

        roleMap.lock(deviceId);
        try {
            final RoleValue rv = getRoleValue(deviceId);
            final MastershipRole currentRole = rv.getRole(newMaster);
            switch (currentRole) {
                case MASTER:
                    //reinforce mastership
                    // RoleInfo integrity check
                    boolean modified = rv.reassign(newMaster, STANDBY, NONE);
                    if (modified) {
                        roleMap.put(deviceId, rv);
                        // should never reach here.
                        log.warn("{} was in both MASTER and STANDBY for {}", newMaster, deviceId);
                        // trigger BACKUPS_CHANGED?
                    }
                    return null;
                case STANDBY:
                case NONE:
                    final NodeId currentMaster = rv.get(MASTER);
                    if (currentMaster != null) {
                        // place current master in STANDBY
                        rv.reassign(currentMaster, NONE, STANDBY);
                        rv.replace(currentMaster, newMaster, MASTER);
                    } else {
                        //no master before so just add.
                        rv.add(MASTER, newMaster);
                    }
                    // remove newMaster from STANDBY
                    rv.reassign(newMaster, STANDBY, NONE);
                    updateTerm(deviceId);
                    roleMap.put(deviceId, rv);
                    return new MastershipEvent(MASTER_CHANGED, deviceId, rv.roleInfo());
                default:
                    log.warn("unknown Mastership Role {}", currentRole);
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
        RoleValue rv = roleMap.get(deviceId);
        if (rv != null) {
            return rv.roleInfo();
        } else {
            return new RoleInfo();
        }
    }

    @Override
    public Set<DeviceId> getDevices(NodeId nodeId) {
        Set<DeviceId> devices = new HashSet<>();

        for (Map.Entry<DeviceId, RoleValue> el : roleMap.entrySet()) {
            if (nodeId.equals(el.getValue().get(MASTER))) {
                devices.add(el.getKey());
            }
        }

        return devices;
    }

    @Override
    public MastershipRole requestRole(DeviceId deviceId) {

        // if no master => become master
        // if there already exists a master:
        //     if I was the master return MASTER
        //     else put myself in STANDBY and return STANDBY

        final NodeId local = clusterService.getLocalNode().id();
        boolean modified = false;
        roleMap.lock(deviceId);
        try {
            final RoleValue rv = getRoleValue(deviceId);
            if (rv.get(MASTER) == null) {
                // there's no master become one
                // move out from STANDBY
                rv.reassign(local, STANDBY, NONE);
                rv.add(MASTER, local);

                updateTerm(deviceId);
                roleMap.put(deviceId, rv);
                return MASTER;
            }
            final MastershipRole currentRole = rv.getRole(local);
            switch (currentRole) {
                case MASTER:
                    // RoleInfo integrity check
                    modified = rv.reassign(local, STANDBY, NONE);
                    if (modified) {
                        log.warn("{} was in both MASTER and STANDBY for {}", local, deviceId);
                        // should never reach here,
                        // but heal if we happened to be there
                        roleMap.put(deviceId, rv);
                        // trigger BACKUPS_CHANGED?
                    }
                    return currentRole;
                case STANDBY:
                    // RoleInfo integrity check
                    modified = rv.reassign(local, NONE, STANDBY);
                    if (modified) {
                        log.warn("{} was in both NONE and STANDBY for {}", local, deviceId);
                        // should never reach here,
                        // but heal if we happened to be there
                        roleMap.put(deviceId, rv);
                        // trigger BACKUPS_CHANGED?
                    }
                    return currentRole;
                case NONE:
                    rv.reassign(local, NONE, STANDBY);
                    roleMap.put(deviceId, rv);
                    // TODO: notifyDelegate BACKUPS_CHANGED
                    return STANDBY;
                default:
                    log.warn("unknown Mastership Role {}", currentRole);
            }
            return currentRole;
        } finally {
            roleMap.unlock(deviceId);
        }
    }

    @Override
    public MastershipTerm getTermFor(DeviceId deviceId) {
        // term information and role must be read atomically
        // acquiring write lock for the device
        roleMap.lock(deviceId);
        try {
            RoleValue rv = getRoleValue(deviceId);
            final Integer term = terms.get(deviceId);
            final NodeId master = rv.get(MASTER);
            if ((master == null) || (term == null)) {
                return null;
            }
            return MastershipTerm.of(master, term);
        } finally {
            roleMap.unlock(deviceId);
        }
    }

    @Override
    public MastershipEvent setStandby(NodeId nodeId, DeviceId deviceId) {
        // if nodeId was MASTER, rotate STANDBY
        // if nodeId was STANDBY no-op
        // if nodeId was NONE, add to STANDBY

        roleMap.lock(deviceId);
        try {
            final RoleValue rv = getRoleValue(deviceId);
            final MastershipRole currentRole = getRole(nodeId, deviceId);
            switch (currentRole) {
                case MASTER:
                    NodeId newMaster = reelect(nodeId, deviceId, rv);
                    rv.reassign(nodeId, NONE, STANDBY);
                    if (newMaster != null) {
                        updateTerm(deviceId);
                        roleMap.put(deviceId, rv);
                        return new MastershipEvent(MASTER_CHANGED, deviceId, rv.roleInfo());
                    } else {
                        // no master candidate
                        roleMap.put(deviceId, rv);
                        // FIXME: Should there be new event type?
                        // or should we issue null Master event?
                        return null;
                    }
                case STANDBY:
                    return null;
                case NONE:
                    rv.reassign(nodeId, NONE, STANDBY);
                    roleMap.put(deviceId, rv);
                    // TODO: BACKUPS_CHANGED?
                    return null;
                default:
                    log.warn("unknown Mastership Role {}", currentRole);
            }
            return null;
        } finally {
            roleMap.unlock(deviceId);
        }
    }

    @Override
    public MastershipEvent relinquishRole(NodeId nodeId, DeviceId deviceId) {
        // relinquishRole is basically set to None

        // If nodeId was master reelect next and remove nodeId
        // else remove from STANDBY

        roleMap.lock(deviceId);
        try {
            final RoleValue rv = getRoleValue(deviceId);
            final MastershipRole currentRole = rv.getRole(nodeId);
            switch (currentRole) {
                case MASTER:
                    NodeId newMaster = reelect(nodeId, deviceId, rv);
                    if (newMaster != null) {
                        updateTerm(deviceId);
                        roleMap.put(deviceId, rv);
                        return new MastershipEvent(MASTER_CHANGED, deviceId, rv.roleInfo());
                    } else {
                        // no master candidate
                        roleMap.put(deviceId, rv);
                        // Should there be new event type?
                        return null;
                    }
                case STANDBY:
                    //fall through to reinforce relinquishment
                case NONE:
                    boolean modified = rv.reassign(nodeId, STANDBY, NONE);
                    if (modified) {
                        roleMap.put(deviceId, rv);
                        // TODO: BACKUPS_CHANGED?
                        return null;
                    }
                    return null;
                default:
                log.warn("unknown Mastership Role {}", currentRole);
            }
            return null;
        } finally {
            roleMap.unlock(deviceId);
        }
    }

    // TODO: Consider moving this to RoleValue method
    //helper to fetch a new master candidate for a given device.
    private NodeId reelect(
            NodeId current, DeviceId deviceId, RoleValue rv) {

        //if this is an queue it'd be neater.
        NodeId candidate = null;
        for (NodeId n : rv.nodesOfRole(STANDBY)) {
            if (!current.equals(n)) {
                candidate = n;
                break;
            }
        }

        if (candidate == null) {
            log.info("{} giving up and going to NONE for {}", current, deviceId);
            rv.remove(MASTER, current);
            // master did change, but there is no master candidate.
            return null;
        } else {
            log.info("{} trying to pass mastership for {} to {}", current, deviceId, candidate);
            rv.replace(current, candidate, MASTER);
            rv.reassign(candidate, STANDBY, NONE);
            return candidate;
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
    // must be guarded by roleMap.lock(deviceId)
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
