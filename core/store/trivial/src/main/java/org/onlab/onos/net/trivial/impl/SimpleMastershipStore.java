package org.onlab.onos.net.trivial.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.MastershipEvent;
import org.onlab.onos.cluster.MastershipStore;
import org.onlab.onos.cluster.MastershipStoreDelegate;
import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.store.AbstractStore;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;

import static org.onlab.onos.cluster.MastershipEvent.Type.*;

/**
 * Manages inventory of controller mastership over devices using
 * trivial, non-distributed in-memory structures implementation.
 */
@Component(immediate = true)
@Service
public class SimpleMastershipStore
        extends AbstractStore<MastershipEvent, MastershipStoreDelegate>
        implements MastershipStore {

    private final Logger log = getLogger(getClass());

    public static final IpPrefix LOCALHOST = IpPrefix.valueOf("127.0.0.1");

    private ControllerNode instance =
            new DefaultControllerNode(new NodeId("local"), LOCALHOST);

    //devices mapped to their masters, to emulate multiple nodes
    protected final ConcurrentMap<DeviceId, NodeId> masterMap =
            new ConcurrentHashMap<>();
    protected final Map<DeviceId, AtomicInteger> termMap = new HashMap<>();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public MastershipEvent setMaster(NodeId nodeId, DeviceId deviceId) {

        NodeId node = masterMap.get(deviceId);
        if (node == null) {
            synchronized (this) {
                masterMap.put(deviceId, nodeId);
                termMap.put(deviceId, new AtomicInteger());
            }
            return new MastershipEvent(MASTER_CHANGED, deviceId, nodeId);
        }

        if (node.equals(nodeId)) {
            return null;
        } else {
            synchronized (this) {
                masterMap.put(deviceId, nodeId);
                termMap.get(deviceId).incrementAndGet();
                return new MastershipEvent(MASTER_CHANGED, deviceId, nodeId);
            }
        }
    }

    @Override
    public NodeId getMaster(DeviceId deviceId) {
        return masterMap.get(deviceId);
    }

    @Override
    public Set<DeviceId> getDevices(NodeId nodeId) {
        Set<DeviceId> ids = new HashSet<>();
        for (Map.Entry<DeviceId, NodeId> d : masterMap.entrySet()) {
            if (d.getValue().equals(nodeId)) {
                ids.add(d.getKey());
            }
        }
        return Collections.unmodifiableSet(ids);
    }

    @Override
    public MastershipRole requestRole(DeviceId deviceId) {
        return getRole(instance.id(), deviceId);
    }

    @Override
    public MastershipRole getRole(NodeId nodeId, DeviceId deviceId) {
        NodeId node = masterMap.get(deviceId);
        MastershipRole role;
        if (node != null) {
            if (node.equals(nodeId)) {
                role = MastershipRole.MASTER;
            } else {
                role = MastershipRole.STANDBY;
            }
        } else {
            //masterMap doesn't contain it.
            role = MastershipRole.MASTER;
            masterMap.put(deviceId, nodeId);
        }
        return role;
    }

    @Override
    public MastershipTerm getTermFor(DeviceId deviceId) {
        if (masterMap.get(deviceId) == null) {
            return null;
        }
        return MastershipTerm.of(
                masterMap.get(deviceId), termMap.get(deviceId).get());
    }

}
