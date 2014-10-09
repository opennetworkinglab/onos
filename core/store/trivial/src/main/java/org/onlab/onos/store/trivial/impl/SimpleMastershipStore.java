package org.onlab.onos.store.trivial.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceMastershipEvent;
import org.onlab.onos.net.device.DeviceMastershipRole;
import org.onlab.onos.net.device.DeviceMastershipStore;
import org.onlab.onos.net.device.DeviceMastershipStoreDelegate;
import org.onlab.onos.net.device.DeviceMastershipTerm;
import org.onlab.onos.store.AbstractStore;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;

import static org.onlab.onos.net.device.DeviceMastershipEvent.Type.*;

/**
 * Manages inventory of controller mastership over devices using
 * trivial, non-distributed in-memory structures implementation.
 */
@Component(immediate = true)
@Service
public class SimpleMastershipStore
        extends AbstractStore<DeviceMastershipEvent, DeviceMastershipStoreDelegate>
        implements DeviceMastershipStore {

    private final Logger log = getLogger(getClass());

    public static final IpPrefix LOCALHOST = IpPrefix.valueOf("127.0.0.1");

    private ControllerNode instance =
            new DefaultControllerNode(new NodeId("local"), LOCALHOST);

    //devices mapped to their masters, to emulate multiple nodes
    protected final Map<DeviceId, NodeId> masterMap = new HashMap<>();
    //emulate backups with pile of nodes
    protected final Set<NodeId> backups = new HashSet<>();
    //terms
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
    public DeviceMastershipEvent setMaster(NodeId nodeId, DeviceId deviceId) {
        DeviceMastershipRole role = getRole(nodeId, deviceId);

        synchronized (this) {
            switch (role) {
                case MASTER:
                    return null;
                case STANDBY:
                    masterMap.put(deviceId, nodeId);
                    termMap.get(deviceId).incrementAndGet();
                    backups.add(nodeId);
                    break;
                case NONE:
                    masterMap.put(deviceId, nodeId);
                    termMap.put(deviceId, new AtomicInteger());
                    backups.add(nodeId);
                    break;
                default:
                    log.warn("unknown Mastership Role {}", role);
                    return null;
            }
        }

        return new DeviceMastershipEvent(MASTER_CHANGED, deviceId, nodeId);
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
    public DeviceMastershipRole requestRole(DeviceId deviceId) {
        //query+possible reelection
        NodeId node = instance.id();
        DeviceMastershipRole role = getRole(node, deviceId);

        switch (role) {
            case MASTER:
                break;
            case STANDBY:
                synchronized (this) {
                    //try to "re-elect", since we're really not distributed
                    NodeId rel = reelect(node);
                    if (rel == null) {
                        masterMap.put(deviceId, node);
                        termMap.put(deviceId, new AtomicInteger());
                        role = DeviceMastershipRole.MASTER;
                    }
                    backups.add(node);
                }
                break;
            case NONE:
                //first to get to it, say we are master
                synchronized (this) {
                    masterMap.put(deviceId, node);
                    termMap.put(deviceId, new AtomicInteger());
                    backups.add(node);
                    role = DeviceMastershipRole.MASTER;
                }
                break;
            default:
                log.warn("unknown Mastership Role {}", role);
        }
        return role;
    }

    @Override
    public DeviceMastershipRole getRole(NodeId nodeId, DeviceId deviceId) {
        //just query
        NodeId current = masterMap.get(deviceId);
        DeviceMastershipRole role;

        if (current == null) {
            if (backups.contains(nodeId)) {
                role = DeviceMastershipRole.STANDBY;
            } else {
                role = DeviceMastershipRole.NONE;
            }
        } else {
            if (current.equals(nodeId)) {
                role = DeviceMastershipRole.MASTER;
            } else {
                role = DeviceMastershipRole.STANDBY;
            }
        }
        return role;
    }

    @Override
    public DeviceMastershipTerm getTermFor(DeviceId deviceId) {
        if ((masterMap.get(deviceId) == null) ||
                (termMap.get(deviceId) == null)) {
            return null;
        }
        return DeviceMastershipTerm.of(
                masterMap.get(deviceId), termMap.get(deviceId).get());
    }

    @Override
    public DeviceMastershipEvent setStandby(NodeId nodeId, DeviceId deviceId) {
        DeviceMastershipRole role = getRole(nodeId, deviceId);
        synchronized (this) {
            switch (role) {
                case MASTER:
                    NodeId backup = reelect(nodeId);
                    if (backup == null) {
                        masterMap.remove(deviceId);
                    } else {
                        masterMap.put(deviceId, backup);
                        termMap.get(deviceId).incrementAndGet();
                        return new DeviceMastershipEvent(MASTER_CHANGED, deviceId, backup);
                    }
                case STANDBY:
                case NONE:
                    if (!termMap.containsKey(deviceId)) {
                        termMap.put(deviceId, new AtomicInteger());
                    }
                    backups.add(nodeId);
                    break;
                default:
                    log.warn("unknown Mastership Role {}", role);
            }
        }
        return null;
    }

    //dumbly selects next-available node that's not the current one
    //emulate leader election
    private NodeId reelect(NodeId nodeId) {
        NodeId backup = null;
        for (NodeId n : backups) {
            if (!n.equals(nodeId)) {
                backup = n;
                break;
            }
        }
        return backup;
    }

    @Override
    public DeviceMastershipEvent relinquishRole(NodeId nodeId, DeviceId deviceId) {
        return setStandby(nodeId, deviceId);
    }

}
