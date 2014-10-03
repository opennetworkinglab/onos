package org.onlab.onos.store.cluster.impl;

import static org.onlab.onos.cluster.MastershipEvent.Type.MASTER_CHANGED;

import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.MastershipEvent;
import org.onlab.onos.cluster.MastershipStore;
import org.onlab.onos.cluster.MastershipStoreDelegate;
import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.store.common.AbstractHazelcastStore;

import com.google.common.collect.ImmutableSet;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;

/**
 * Distributed implementation of the cluster nodes store.
 */
@Component(immediate = true)
@Service
public class DistributedMastershipStore
extends AbstractHazelcastStore<MastershipEvent, MastershipStoreDelegate>
implements MastershipStore {

    //arbitrary lock name
    private static final String LOCK = "lock";
    //initial term value
    private static final Integer INIT = 0;
    //placeholder non-null value
    private static final Byte NIL = 0x0;

    //devices to masters
    protected IMap<byte[], byte[]> rawMasters;
    //devices to terms
    protected IMap<byte[], Integer> rawTerms;
    //collection of nodes. values are ignored, as it's used as a makeshift 'set'
    protected IMap<byte[], Byte> backups;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    //FIXME: need to guarantee that this will be met, sans circular dependencies
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected DeviceService deviceService;

    @Override
    @Activate
    public void activate() {
        super.activate();

        rawMasters = theInstance.getMap("masters");
        rawTerms = theInstance.getMap("terms");
        backups = theInstance.getMap("backups");

        rawMasters.addEntryListener(new RemoteMasterShipEventHandler(), true);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public MastershipEvent setMaster(NodeId nodeId, DeviceId deviceId) {
        byte [] did = serialize(deviceId);
        byte [] nid = serialize(nodeId);

        ILock lock = theInstance.getLock(LOCK);
        lock.lock();
        try {
            MastershipRole role = getRole(nodeId, deviceId);
            Integer term = rawTerms.get(did);
            switch (role) {
                case MASTER:
                    return null;
                case STANDBY:
                    rawMasters.put(did, nid);
                    rawTerms.put(did, ++term);
                    backups.putIfAbsent(nid, NIL);
                    break;
                case NONE:
                    rawMasters.put(did, nid);
                    //new switch OR state transition after being orphaned
                    if (term == null) {
                        rawTerms.put(did, INIT);
                    } else {
                        rawTerms.put(did, ++term);
                    }
                    backups.put(nid, NIL);
                    break;
                default:
                    log.warn("unknown Mastership Role {}", role);
                    return null;
            }
            return new MastershipEvent(MASTER_CHANGED, deviceId, nodeId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public NodeId getMaster(DeviceId deviceId) {
        return deserialize(rawMasters.get(serialize(deviceId)));
    }

    @Override
    public Set<DeviceId> getDevices(NodeId nodeId) {
        ImmutableSet.Builder<DeviceId> builder = ImmutableSet.builder();

        for (Map.Entry<byte[], byte[]> entry : rawMasters.entrySet()) {
            if (nodeId.equals(deserialize(entry.getValue()))) {
                builder.add((DeviceId) deserialize(entry.getKey()));
            }
        }

        return builder.build();
    }

    @Override
    public MastershipRole requestRole(DeviceId deviceId) {
        // first to empty slot for device in master map is MASTER
        // depending on how backups are organized, might need to trigger election
        // so only controller doesn't set itself to backup for another device
        byte [] did = serialize(deviceId);
        NodeId local = clusterService.getLocalNode().id();
        byte [] lnid = serialize(local);

        ILock lock = theInstance.getLock(LOCK);
        lock.lock();
        try {
            MastershipRole role = getRole(local, deviceId);
            switch (role) {
                case MASTER:
                    break;
                case STANDBY:
                    backups.put(lnid, NIL);
                    rawTerms.putIfAbsent(did, INIT);
                    break;
                case NONE:
                    rawMasters.put(did, lnid);
                    rawTerms.putIfAbsent(did, INIT);
                    backups.put(lnid, NIL);
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
    public MastershipRole getRole(NodeId nodeId, DeviceId deviceId) {
        byte[] did = serialize(deviceId);

        NodeId current = deserialize(rawMasters.get(did));
        MastershipRole role = null;

        if (current == null) {
            //IFF no controllers have claimed mastership over it
            role = MastershipRole.NONE;
        } else {
            if (current.equals(nodeId)) {
                role = MastershipRole.MASTER;
            } else {
                role = MastershipRole.STANDBY;
            }
        }

        return role;
    }

    @Override
    public MastershipTerm getTermFor(DeviceId deviceId) {
        byte[] did = serialize(deviceId);

        if ((rawMasters.get(did) == null) ||
                (rawTerms.get(did) == null)) {
            return null;
        }
        return MastershipTerm.of(
                (NodeId) deserialize(rawMasters.get(did)), rawTerms.get(did));
    }

    @Override
    public MastershipEvent unsetMaster(NodeId nodeId, DeviceId deviceId) {
        byte [] did = serialize(deviceId);

        ILock lock = theInstance.getLock(LOCK);
        lock.lock();
        try {
            MastershipRole role = getRole(nodeId, deviceId);
            switch (role) {
                case MASTER:
                    //hand off device to another
                    NodeId backup = reelect(nodeId, deviceId);
                    if (backup == null) {
                        //goes back to NONE
                        rawMasters.remove(did);
                    } else {
                        //goes to STANDBY for local, MASTER for someone else
                        Integer term = rawTerms.get(did);
                        rawMasters.put(did, serialize(backup));
                        rawTerms.put(did, ++term);
                        return new MastershipEvent(MASTER_CHANGED, deviceId, backup);
                    }
                case STANDBY:
                case NONE:
                    break;
                default:
                    log.warn("unknown Mastership Role {}", role);
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    //helper for "re-electing" a new master for a given device
    private NodeId reelect(NodeId current, DeviceId deviceId) {

        for (byte [] node : backups.keySet()) {
            NodeId nid = deserialize(node);
            //if a device dies we shouldn't pick another master for it.
            if (!current.equals(nid) && (deviceService.isAvailable(deviceId))) {
                return nid;
            }
        }
        return null;
    }

    //adds node to pool(s) of backup
    private void backup(NodeId nodeId, DeviceId deviceId) {
        //TODO might be useful to isolate out this function and reelect() if we
        //get more backup/election schemes
    }

    private class RemoteMasterShipEventHandler extends RemoteEventHandler<DeviceId, NodeId> {

        @Override
        protected void onAdd(DeviceId deviceId, NodeId nodeId) {
            //only addition indicates a change in mastership
            notifyDelegate(new MastershipEvent(MASTER_CHANGED, deviceId, nodeId));
        }

        @Override
        protected void onRemove(DeviceId deviceId, NodeId nodeId) {
            //notifyDelegate(new MastershipEvent(MASTER_CHANGED, deviceId, nodeId));
        }

        @Override
        protected void onUpdate(DeviceId deviceId, NodeId oldNodeId, NodeId nodeId) {
            //notifyDelegate(new MastershipEvent(MASTER_CHANGED, deviceId, nodeId));
        }
    }

}
