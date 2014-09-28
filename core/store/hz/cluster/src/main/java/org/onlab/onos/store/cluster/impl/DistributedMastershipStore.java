package org.onlab.onos.store.cluster.impl;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static org.onlab.onos.cluster.MastershipEvent.Type.MASTER_CHANGED;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.MastershipEvent;
import org.onlab.onos.cluster.MastershipStore;
import org.onlab.onos.cluster.MastershipStoreDelegate;
import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.store.common.AbsentInvalidatingLoadingCache;
import org.onlab.onos.store.common.AbstractHazelcastStore;
import org.onlab.onos.store.common.OptionalCacheLoader;

import com.google.common.base.Optional;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.hazelcast.core.IMap;

/**
 * Distributed implementation of the cluster nodes store.
 */
@Component(immediate = true)
@Service
public class DistributedMastershipStore
extends AbstractHazelcastStore<MastershipEvent, MastershipStoreDelegate>
implements MastershipStore {

    private IMap<byte[], byte[]> rawMasters;
    private LoadingCache<DeviceId, Optional<NodeId>> masters;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Override
    @Activate
    public void activate() {
        super.activate();

        rawMasters = theInstance.getMap("masters");
        OptionalCacheLoader<DeviceId, NodeId> nodeLoader
        = new OptionalCacheLoader<>(storeService, rawMasters);
        masters = new AbsentInvalidatingLoadingCache<>(newBuilder().build(nodeLoader));
        rawMasters.addEntryListener(new RemoteMasterShipEventHandler(masters), true);

        loadMasters();

        log.info("Started");
    }

    private void loadMasters() {
        for (byte[] keyBytes : rawMasters.keySet()) {
            final DeviceId id = deserialize(keyBytes);
            masters.refresh(id);
        }
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public MastershipEvent setMaster(NodeId nodeId, DeviceId deviceId) {
        synchronized (this) {
            NodeId currentMaster = getMaster(deviceId);
            if (Objects.equals(currentMaster, nodeId)) {
                return null;
            }

            // FIXME: for now implementing semantics of setMaster
            rawMasters.put(serialize(deviceId), serialize(nodeId));
            masters.put(deviceId, Optional.of(nodeId));
            return new MastershipEvent(MastershipEvent.Type.MASTER_CHANGED, deviceId, nodeId);
        }
    }

    @Override
    public NodeId getMaster(DeviceId deviceId) {
        return masters.getUnchecked(deviceId).orNull();
    }

    @Override
    public Set<DeviceId> getDevices(NodeId nodeId) {
        ImmutableSet.Builder<DeviceId> builder = ImmutableSet.builder();
        for (Map.Entry<DeviceId, Optional<NodeId>> entry : masters.asMap().entrySet()) {
            if (nodeId.equals(entry.getValue().get())) {
                builder.add(entry.getKey());
            }
        }
        return builder.build();
    }

    @Override
    public MastershipRole requestRole(DeviceId deviceId) {
        // FIXME: for now we are 'selecting' as master whoever asks
        setMaster(clusterService.getLocalNode().id(), deviceId);
        return MastershipRole.MASTER;
    }

    @Override
    public MastershipRole getRole(NodeId nodeId, DeviceId deviceId) {
        NodeId master = masters.getUnchecked(deviceId).orNull();
        return nodeId.equals(master) ? MastershipRole.MASTER : MastershipRole.STANDBY;
    }

    @Override
    public MastershipTerm getTermFor(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    private class RemoteMasterShipEventHandler extends RemoteCacheEventHandler<DeviceId, NodeId> {
        public RemoteMasterShipEventHandler(LoadingCache<DeviceId, Optional<NodeId>> cache) {
            super(cache);
        }

        @Override
        protected void onAdd(DeviceId deviceId, NodeId nodeId) {
            notifyDelegate(new MastershipEvent(MASTER_CHANGED, deviceId, nodeId));
        }

        @Override
        protected void onRemove(DeviceId deviceId, NodeId nodeId) {
            notifyDelegate(new MastershipEvent(MASTER_CHANGED, deviceId, nodeId));
        }

        @Override
        protected void onUpdate(DeviceId deviceId, NodeId oldNodeId, NodeId nodeId) {
            notifyDelegate(new MastershipEvent(MASTER_CHANGED, deviceId, nodeId));
        }
    }

}
