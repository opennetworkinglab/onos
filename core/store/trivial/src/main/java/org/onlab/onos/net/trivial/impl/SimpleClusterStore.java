package org.onlab.onos.net.trivial.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterStore;
import org.onlab.onos.cluster.ClusterStoreDelegate;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.AbstractStore;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of infrastructure DEVICES using trivial in-memory
 * structures implementation.
 */
@Component(immediate = true)
@Service
public class SimpleClusterStore
        extends AbstractStore<ClusterEvent, ClusterStoreDelegate>
        implements ClusterStore {

    public static final IpPrefix LOCALHOST = IpPrefix.valueOf("127.0.0.1");

    private final Logger log = getLogger(getClass());

    private ControllerNode instance;

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
    public ControllerNode getLocalNode() {
        return instance;
    }

    @Override
    public Set<ControllerNode> getNodes() {
        return ImmutableSet.of(instance);
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        return instance.id().equals(nodeId) ? instance : null;
    }

    @Override
    public ControllerNode.State getState(NodeId nodeId) {
        return ControllerNode.State.ACTIVE;
    }

    @Override
    public void removeNode(NodeId nodeId) {
    }

}
