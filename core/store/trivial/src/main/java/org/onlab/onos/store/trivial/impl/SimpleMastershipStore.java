package org.onlab.onos.store.trivial.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.onlab.onos.cluster.RoleInfo;
import org.onlab.onos.mastership.MastershipEvent;
import org.onlab.onos.mastership.MastershipStore;
import org.onlab.onos.mastership.MastershipStoreDelegate;
import org.onlab.onos.mastership.MastershipTerm;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.store.AbstractStore;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

import static org.onlab.onos.mastership.MastershipEvent.Type.*;

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
    public MastershipEvent setMaster(NodeId nodeId, DeviceId deviceId) {
        MastershipRole role = getRole(nodeId, deviceId);

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

        return new MastershipEvent(MASTER_CHANGED, deviceId,
                new RoleInfo(nodeId, Lists.newLinkedList(backups)));
    }

    @Override
    public NodeId getMaster(DeviceId deviceId) {
        return masterMap.get(deviceId);
    }

    @Override
    public RoleInfo getNodes(DeviceId deviceId) {
        List<NodeId> nodes = new ArrayList<>();
        nodes.addAll(backups);

        return new RoleInfo(masterMap.get(deviceId), nodes);
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
        //query+possible reelection
        NodeId node = instance.id();
        MastershipRole role = getRole(node, deviceId);

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
                        role = MastershipRole.MASTER;
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
                    role = MastershipRole.MASTER;
                }
                break;
            default:
                log.warn("unknown Mastership Role {}", role);
        }
        return role;
    }

    @Override
    public MastershipRole getRole(NodeId nodeId, DeviceId deviceId) {
        //just query
        NodeId current = masterMap.get(deviceId);
        MastershipRole role;

        if (current == null) {
            if (backups.contains(nodeId)) {
                role = MastershipRole.STANDBY;
            } else {
                role = MastershipRole.NONE;
            }
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
        if ((masterMap.get(deviceId) == null) ||
                (termMap.get(deviceId) == null)) {
            return null;
        }
        return MastershipTerm.of(
                masterMap.get(deviceId), termMap.get(deviceId).get());
    }

    @Override
    public MastershipEvent setStandby(NodeId nodeId, DeviceId deviceId) {
        MastershipRole role = getRole(nodeId, deviceId);
        synchronized (this) {
            switch (role) {
                case MASTER:
                    NodeId backup = reelect(nodeId);
                    if (backup == null) {
                        masterMap.remove(deviceId);
                    } else {
                        masterMap.put(deviceId, backup);
                        termMap.get(deviceId).incrementAndGet();
                        return new MastershipEvent(MASTER_CHANGED, deviceId,
                                new RoleInfo(backup, Lists.newLinkedList(backups)));
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
        backups.remove(backup);
        return backup;
    }

    @Override
    public MastershipEvent relinquishRole(NodeId nodeId, DeviceId deviceId) {
        MastershipRole role = getRole(nodeId, deviceId);
        synchronized (this) {
            switch (role) {
                case MASTER:
                    NodeId backup = reelect(nodeId);
                    backups.remove(nodeId);
                    if (backup == null) {
                        masterMap.remove(deviceId);
                    } else {
                        masterMap.put(deviceId, backup);
                        termMap.get(deviceId).incrementAndGet();
                        return new MastershipEvent(MASTER_CHANGED, deviceId,
                                new RoleInfo(backup, Lists.newLinkedList(backups)));
                    }
                case STANDBY:
                    backups.remove(nodeId);
                case NONE:
                default:
                    log.warn("unknown Mastership Role {}", role);
            }
        }
        return null;
    }

}
