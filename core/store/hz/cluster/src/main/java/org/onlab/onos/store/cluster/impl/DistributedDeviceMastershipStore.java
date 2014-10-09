package org.onlab.onos.store.cluster.impl;

import static org.onlab.onos.net.device.DeviceMastershipEvent.Type.MASTER_CHANGED;

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
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.device.DeviceMastershipEvent;
import org.onlab.onos.net.device.DeviceMastershipStore;
import org.onlab.onos.net.device.DeviceMastershipStoreDelegate;
import org.onlab.onos.net.device.DeviceMastershipTerm;
import org.onlab.onos.store.common.AbstractHazelcastStore;

import com.google.common.collect.ImmutableSet;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MultiMap;

/**
 * Distributed implementation of the mastership store. The store is
 * responsible for the master selection process.
 */
@Component(immediate = true)
@Service
public class DistributedDeviceMastershipStore
extends AbstractHazelcastStore<DeviceMastershipEvent, DeviceMastershipStoreDelegate>
implements DeviceMastershipStore {

    //arbitrary lock name
    private static final String LOCK = "lock";
    //initial term/TTL value
    private static final Integer INIT = 0;

    //devices to masters
    protected IMap<byte[], byte[]> masters;
    //devices to terms
    protected IMap<byte[], Integer> terms;

    //re-election related, disjoint-set structures:
    //device-nodes multiset of available nodes
    protected MultiMap<byte[], byte[]> standbys;
    //device-nodes multiset for nodes that have given up on device
    protected MultiMap<byte[], byte[]> unusable;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Override
    @Activate
    public void activate() {
        super.activate();

        masters = theInstance.getMap("masters");
        terms = theInstance.getMap("terms");
        standbys = theInstance.getMultiMap("backups");
        unusable = theInstance.getMultiMap("unusable");

        masters.addEntryListener(new RemoteMasterShipEventHandler(), true);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public MastershipRole getRole(NodeId nodeId, DeviceId deviceId) {
        byte[] did = serialize(deviceId);
        byte[] nid = serialize(nodeId);

        NodeId current = deserialize(masters.get(did));
        if (current == null) {
            if (standbys.containsEntry(did, nid)) {
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
    public DeviceMastershipEvent setMaster(NodeId nodeId, DeviceId deviceId) {
        byte [] did = serialize(deviceId);
        byte [] nid = serialize(nodeId);

        ILock lock = theInstance.getLock(LOCK);
        lock.lock();
        try {
            MastershipRole role = getRole(nodeId, deviceId);
            switch (role) {
                case MASTER:
                    //reinforce mastership
                    evict(nid, did);
                    return null;
                case STANDBY:
                    //make current master standby
                    byte [] current = masters.get(did);
                    if (current != null) {
                        backup(current, did);
                    }
                    //assign specified node as new master
                    masters.put(did, nid);
                    evict(nid, did);
                    updateTerm(did);
                    return new DeviceMastershipEvent(MASTER_CHANGED, deviceId, nodeId);
                case NONE:
                    masters.put(did, nid);
                    evict(nid, did);
                    updateTerm(did);
                    return new DeviceMastershipEvent(MASTER_CHANGED, deviceId, nodeId);
                default:
                    log.warn("unknown Mastership Role {}", role);
                    return null;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public NodeId getMaster(DeviceId deviceId) {
        return deserialize(masters.get(serialize(deviceId)));
    }

    @Override
    public Set<DeviceId> getDevices(NodeId nodeId) {
        ImmutableSet.Builder<DeviceId> builder = ImmutableSet.builder();

        for (Map.Entry<byte[], byte[]> entry : masters.entrySet()) {
            if (nodeId.equals(deserialize(entry.getValue()))) {
                builder.add((DeviceId) deserialize(entry.getKey()));
            }
        }

        return builder.build();
    }

    @Override
    public MastershipRole requestRole(DeviceId deviceId) {
        NodeId local = clusterService.getLocalNode().id();
        byte [] did = serialize(deviceId);
        byte [] lnid = serialize(local);

        ILock lock = theInstance.getLock(LOCK);
        lock.lock();
        try {
            MastershipRole role = getRole(local, deviceId);
            switch (role) {
                case MASTER:
                    evict(lnid, did);
                    break;
                case STANDBY:
                    backup(lnid, did);
                    terms.putIfAbsent(did, INIT);
                    break;
                case NONE:
                    //claim mastership
                    masters.put(did, lnid);
                    evict(lnid, did);
                    updateTerm(did);
                    role = MastershipRole.MASTER;
                    break;
                default:
                    log.warn("unknown Mastership Role {}", role);
            }
            return role;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public DeviceMastershipTerm getTermFor(DeviceId deviceId) {
        byte[] did = serialize(deviceId);
        if ((masters.get(did) == null) ||
                (terms.get(did) == null)) {
            return null;
        }
        return DeviceMastershipTerm.of(
                (NodeId) deserialize(masters.get(did)), terms.get(did));
    }

    @Override
    public DeviceMastershipEvent setStandby(NodeId nodeId, DeviceId deviceId) {
        byte [] did = serialize(deviceId);
        byte [] nid = serialize(nodeId);
        DeviceMastershipEvent event = null;

        ILock lock = theInstance.getLock(LOCK);
        lock.lock();
        try {
            MastershipRole role = getRole(nodeId, deviceId);
            switch (role) {
                case MASTER:
                    event = reelect(nodeId, deviceId);
                    backup(nid, did);
                    break;
                case STANDBY:
                    //fall through to reinforce role
                case NONE:
                    backup(nid, did);
                    break;
                default:
                    log.warn("unknown Mastership Role {}", role);
            }
            return event;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public DeviceMastershipEvent relinquishRole(NodeId nodeId, DeviceId deviceId) {
        byte [] did = serialize(deviceId);
        byte [] nid = serialize(nodeId);
        DeviceMastershipEvent event = null;

        ILock lock = theInstance.getLock(LOCK);
        lock.lock();
        try {
            MastershipRole role = getRole(nodeId, deviceId);
            switch (role) {
                case MASTER:
                    event = reelect(nodeId, deviceId);
                    evict(nid, did);
                    break;
                case STANDBY:
                    //fall through to reinforce relinquishment
                case NONE:
                    evict(nid, did);
                    break;
                default:
                log.warn("unknown Mastership Role {}", role);
            }
            return event;
        } finally {
            lock.unlock();
        }
    }

    //helper to fetch a new master candidate for a given device.
    private DeviceMastershipEvent reelect(NodeId current, DeviceId deviceId) {
        byte [] did = serialize(deviceId);
        byte [] nid = serialize(current);

        //if this is an queue it'd be neater.
        byte [] backup = null;
        for (byte [] n : standbys.get(serialize(deviceId))) {
            if (!current.equals(deserialize(n))) {
                backup = n;
                break;
            }
        }

        if (backup == null) {
            masters.remove(did, nid);
            return null;
        } else {
            masters.put(did, backup);
            evict(backup, did);
            Integer term = terms.get(did);
            terms.put(did, ++term);
            return new DeviceMastershipEvent(
                    MASTER_CHANGED, deviceId, (NodeId) deserialize(backup));
        }
    }

    //adds node to pool(s) of backups and moves them from unusable.
    private void backup(byte [] nodeId, byte [] deviceId) {
        if (!standbys.containsEntry(deviceId, nodeId)) {
            standbys.put(deviceId, nodeId);
        }
        if (unusable.containsEntry(deviceId, nodeId)) {
            unusable.remove(deviceId, nodeId);
        }
    }

    //adds node to unusable and evicts it from backup pool.
    private void evict(byte [] nodeId, byte [] deviceId) {
        if (!unusable.containsEntry(deviceId, nodeId)) {
            unusable.put(deviceId, nodeId);
        }
        if (standbys.containsEntry(deviceId, nodeId)) {
            standbys.remove(deviceId, nodeId);
        }
    }

    //adds or updates term information.
    private void updateTerm(byte [] deviceId) {
        Integer term = terms.get(deviceId);
        if (term == null) {
            terms.put(deviceId, INIT);
        } else {
            terms.put(deviceId, ++term);
        }
    }

    private class RemoteMasterShipEventHandler extends RemoteEventHandler<DeviceId, NodeId> {

        @Override
        protected void onAdd(DeviceId deviceId, NodeId nodeId) {
            notifyDelegate(new DeviceMastershipEvent(MASTER_CHANGED, deviceId, nodeId));
        }

        @Override
        protected void onRemove(DeviceId deviceId, NodeId nodeId) {
            //notifyDelegate(new MastershipEvent(MASTER_CHANGED, deviceId, nodeId));
        }

        @Override
        protected void onUpdate(DeviceId deviceId, NodeId oldNodeId, NodeId nodeId) {
            //only addition indicates a change in mastership
            //notifyDelegate(new MastershipEvent(MASTER_CHANGED, deviceId, nodeId));
        }
    }

}
