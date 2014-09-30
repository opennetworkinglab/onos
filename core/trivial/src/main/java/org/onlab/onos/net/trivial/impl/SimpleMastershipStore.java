package org.onlab.onos.net.trivial.impl;

import static org.slf4j.LoggerFactory.getLogger;

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

import com.google.common.collect.Lists;

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
    protected final Map<DeviceId, NodeId> masterMap = new HashMap<>();
    //emulate backups
    protected final Map<DeviceId, List<NodeId>> backupMap = new HashMap<>();
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

        NodeId current = masterMap.get(deviceId);
        List<NodeId> backups = backupMap.get(deviceId);

        if (current == null) {
            if (backups == null) {
                //add new mapping to everything
                synchronized (this) {
                    masterMap.put(deviceId, nodeId);
                    backups = Lists.newLinkedList();
                    backupMap.put(deviceId, backups);
                    termMap.put(deviceId, new AtomicInteger());
                }
            } else {
                //set master to new node and remove from backups if there
                synchronized (this) {
                    masterMap.put(deviceId, nodeId);
                    backups.remove(nodeId);
                    termMap.get(deviceId).incrementAndGet();
                }
            }
        } else if (current.equals(nodeId)) {
            return null;
        } else {
            //add current to backup, set master to new node
            masterMap.put(deviceId, nodeId);
            backups.add(current);
            backups.remove(nodeId);
            termMap.get(deviceId).incrementAndGet();
        }

        updateStandby(nodeId, deviceId);
        return new MastershipEvent(MASTER_CHANGED, deviceId, nodeId);
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
        NodeId current = masterMap.get(deviceId);
        List<NodeId> backups = backupMap.get(deviceId);

        if (current == null) {
            //masterMap or backup doesn't contain device. Say new node is MASTER
            if (backups == null) {
                synchronized (this) {
                    masterMap.put(deviceId, nodeId);
                    backups = Lists.newLinkedList();
                    backupMap.put(deviceId, backups);
                    termMap.put(deviceId, new AtomicInteger());
                }
                updateStandby(nodeId, deviceId);
                return MastershipRole.MASTER;
            }

            //device once existed, but got removed, and is now getting a backup.
            if (!backups.contains(nodeId)) {
                synchronized (this) {
                    backups.add(nodeId);
                    termMap.put(deviceId, new AtomicInteger());
                }
                updateStandby(nodeId, deviceId);
            }

        } else if (current.equals(nodeId)) {
            return  MastershipRole.MASTER;
        } else {
            //once created, a device never has a null backups list.
            if (!backups.contains(nodeId)) {
                //we must have requested STANDBY setting
                synchronized (this) {
                    backups.add(nodeId);
                    termMap.put(deviceId, new AtomicInteger());
                }
                updateStandby(nodeId, deviceId);
            }
        }

        return MastershipRole.STANDBY;
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
    public MastershipEvent unsetMaster(NodeId nodeId, DeviceId deviceId) {
        NodeId node = masterMap.get(deviceId);

        //TODO case where node is completely removed from the cluster?
        if (node.equals(nodeId)) {
            synchronized (this) {
                //pick new node.
                List<NodeId> backups = backupMap.get(deviceId);

                //no backups, so device is hosed
                if (backups.isEmpty()) {
                    masterMap.remove(deviceId);
                    backups.add(nodeId);
                    return null;
                }
                NodeId backup = backups.remove(0);
                masterMap.put(deviceId, backup);
                backups.add(nodeId);
                return new MastershipEvent(MASTER_CHANGED, deviceId, backup);
            }
        }
        return null;
    }

    //add node as STANDBY to maps un-scalably.
    private void updateStandby(NodeId nodeId, DeviceId deviceId) {
        for (Map.Entry<DeviceId, List<NodeId>> e : backupMap.entrySet()) {
            DeviceId dev = e.getKey();
            if (dev.equals(deviceId)) {
                continue;
            }
            synchronized (this) {
                List<NodeId> nodes = e.getValue();
                if (!nodes.contains(nodeId)) {
                    nodes.add(nodeId);
                }
            }
        }
    }

}
