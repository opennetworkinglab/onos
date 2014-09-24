package org.onlab.onos.net.trivial.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.MastershipEvent;
import org.onlab.onos.cluster.MastershipStore;
import org.onlab.onos.cluster.MastershipStoreDelegate;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.store.AbstractStore;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;

import static org.onlab.onos.cluster.MastershipEvent.Type.*;

/**
 * Manages inventory of controller mastership over devices using
 * trivial in-memory structures implementation.
 */
@Component(immediate = true)
@Service
public class SimpleMastershipStore
        extends AbstractStore<MastershipEvent, MastershipStoreDelegate>
        implements MastershipStore {

    public static final IpPrefix LOCALHOST = IpPrefix.valueOf("127.0.0.1");

    private final Logger log = getLogger(getClass());

    private ControllerNode instance;

    protected final ConcurrentMap<DeviceId, MastershipRole> roleMap =
            new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        instance = new DefaultControllerNode(new NodeId("local"), LOCALHOST);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public MastershipEvent setRole(NodeId nodeId, DeviceId deviceId,
                                   MastershipRole role) {
        if (roleMap.get(deviceId) == null) {
            return null;
        }
        roleMap.put(deviceId, role);
        return new MastershipEvent(MASTER_CHANGED, deviceId, nodeId);
    }

    @Override
    public NodeId getMaster(DeviceId deviceId) {
        return instance.id();
    }

    @Override
    public Set<DeviceId> getDevices(NodeId nodeId) {
        return Collections.unmodifiableSet(roleMap.keySet());
    }

    @Override
    public MastershipRole requestRole(DeviceId deviceId) {
        return getRole(instance.id(), deviceId);
    }

    @Override
    public MastershipRole getRole(NodeId nodeId, DeviceId deviceId) {
        MastershipRole role = roleMap.get(deviceId);
        if (role == null) {
            //say MASTER. If clustered, we'd figure out if anyone's got dibs here.
            role = MastershipRole.MASTER;
            roleMap.put(deviceId, role);
        }
        return role;
    }

}
