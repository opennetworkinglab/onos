package org.onlab.onos.store.mastership.impl;

import static org.onlab.onos.mastership.MastershipEvent.Type.MASTER_CHANGED;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
import org.onlab.onos.mastership.MastershipEvent;
import org.onlab.onos.mastership.MastershipStore;
import org.onlab.onos.mastership.MastershipStoreDelegate;
import org.onlab.onos.mastership.MastershipTerm;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.store.common.AbstractHazelcastStore;
import org.onlab.onos.store.common.SMap;
import org.onlab.onos.store.serializers.KryoPoolUtil;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.util.KryoPool;

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    @Activate
    public void activate() {
        super.activate();

        this.serializer = new KryoSerializer() {
            @Override
            protected void setupKryoPool() {
                serializerPool = KryoPool.newBuilder()
                        .register(KryoPoolUtil.API)

                        .register(RoleValue.class, new RoleValueSerializer())
                        .build()
                        .populate(1);
            }
        };

        roleMap = new SMap(theInstance.getMap("nodeRoles"), this.serializer);
        terms = new SMap(theInstance.getMap("terms"), this.serializer);
        clusterSize = theInstance.getAtomicLong("clustersize");
        roleMap.addEntryListener((new RemoteMasterShipEventHandler()), true);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public MastershipRole getRole(NodeId nodeId, DeviceId deviceId) {
        NodeId current = getNode(MASTER, deviceId);
        if (current == null) {
            if (isRole(STANDBY, nodeId, deviceId)) {
                //was previously standby, or set to standby from master
                return MastershipRole.STANDBY;
            } else {
                return MastershipRole.NONE;
            }
        } else {
            if (current.equals(nodeId)) {
                //*should* be in unusable, not always
                return MastershipRole.MASTER;
            } else {
                //may be in backups or unusable from earlier retirement
                return MastershipRole.STANDBY;
            }
        }
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
                    NodeId current = rv.get(MASTER);
                    if (current != null) {
                        //backup and replace current master
                        rv.reassign(nodeId, NONE, STANDBY);
                        rv.replace(current, nodeId, MASTER);
                    } else {
                        //no master before so just add.
                        rv.add(MASTER, nodeId);
                    }
                    rv.reassign(nodeId, STANDBY, NONE);
                    roleMap.put(deviceId, rv);
                    updateTerm(deviceId);
                    return new MastershipEvent(MASTER_CHANGED, deviceId, nodeId);
                case NONE:
                    rv.add(MASTER, nodeId);
                    rv.reassign(nodeId, STANDBY, NONE);
                    roleMap.put(deviceId, rv);
                    updateTerm(deviceId);
                    return new MastershipEvent(MASTER_CHANGED, deviceId, nodeId);
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
    public List<NodeId> getNodes(DeviceId deviceId) {
        List<NodeId> nodes = new LinkedList<>();

        //add current master to head - if there is one.
        roleMap.lock(deviceId);
        try {
            RoleValue rv = getRoleValue(deviceId);
            NodeId master = rv.get(MASTER);
            if (master != null) {
                nodes.add(master);
            }
            //We ignore NONE nodes.
            nodes.addAll(rv.nodesOfRole(STANDBY));
            return Collections.unmodifiableList(nodes);
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
                    roleMap.put(deviceId, rv);
                    break;
                case STANDBY:
                    rv.reassign(local, NONE, STANDBY);
                    roleMap.put(deviceId, rv);
                    terms.putIfAbsent(deviceId, INIT);

                    break;
                case NONE:
                    //claim mastership
                    rv.add(MASTER, local);
                    rv.reassign(local, STANDBY, NONE);
                    roleMap.put(deviceId, rv);
                    updateTerm(deviceId);
                    role = MastershipRole.MASTER;
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
            roleMap.put(deviceId, rv);
            return null;
        } else {
            log.info("{} trying to pass mastership for {} to {}", current, deviceId, backup);
            rv.replace(current, backup, MASTER);
            rv.reassign(backup, STANDBY, NONE);
            roleMap.put(deviceId, rv);
            Integer term = terms.get(deviceId);
            terms.put(deviceId, ++term);
            return new MastershipEvent(MASTER_CHANGED, deviceId, backup);
        }
    }

    //return the RoleValue structure for a device, or create one
    private RoleValue getRoleValue(DeviceId deviceId) {
        RoleValue value = roleMap.get(deviceId);
        if (value == null) {
            value = new RoleValue();
            roleMap.put(deviceId, value);
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

    //check if node is a certain role given a device
    private boolean isRole(
            MastershipRole role, NodeId nodeId, DeviceId deviceId) {
        RoleValue value = roleMap.get(deviceId);
        if (value != null) {
            return value.contains(role, nodeId);
        }
        return false;
    }

    //adds or updates term information.
    private void updateTerm(DeviceId deviceId) {
        terms.lock(deviceId);
        try {
            Integer term = terms.get(deviceId);
            if (term == null) {
                terms.put(deviceId, INIT);
            } else {
                terms.put(deviceId, ++term);
            }
        } finally {
            terms.unlock(deviceId);
        }
    }

    private class RemoteMasterShipEventHandler implements EntryListener<DeviceId, RoleValue> {

        @Override
        public void entryAdded(EntryEvent<DeviceId, RoleValue> event) {
        }

        @Override
        public void entryRemoved(EntryEvent<DeviceId, RoleValue> event) {
        }

        @Override
        public void entryUpdated(EntryEvent<DeviceId, RoleValue> event) {
            NodeId myId = clusterService.getLocalNode().id();
            NodeId node = event.getValue().get(MASTER);
            if (myId.equals(node)) {
                // XXX or do we just let it get sent and caught by ourself?
                return;
            }
            notifyDelegate(new MastershipEvent(
                    MASTER_CHANGED, event.getKey(), event.getValue().get(MASTER)));
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
