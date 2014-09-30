package org.onlab.onos.store.cluster.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
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
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.onlab.onos.cluster.ControllerNode.State;
import static org.onlab.packet.IpPrefix.valueOf;

/**
 * Distributed implementation of the cluster nodes store.
 */
@Component(immediate = true)
@Service
public class DistributedClusterStore
        extends AbstractStore<ClusterEvent, ClusterStoreDelegate>
        implements ClusterStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DefaultControllerNode localNode;
    private final Map<NodeId, DefaultControllerNode> nodes = new ConcurrentHashMap<>();
    private final Map<NodeId, State> states = new ConcurrentHashMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterCommunicationAdminService communicationAdminService;

    private final ClusterNodesDelegate nodesDelegate = new InnerNodesDelegate();

    @Activate
    public void activate() {
        loadClusterDefinition();
        establishSelfIdentity();

        // Start-up the comm service and prime it with the loaded nodes.
        communicationAdminService.startUp(localNode, nodesDelegate);
        for (DefaultControllerNode node : nodes.values()) {
            communicationAdminService.addNode(node);
        }
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    /**
     * Loads the cluster definition file.
     */
    private void loadClusterDefinition() {
        ClusterDefinitionStore cds = new ClusterDefinitionStore("../config/cluster.json");
        try {
            Set<DefaultControllerNode> storedNodes = cds.read();
            for (DefaultControllerNode node : storedNodes) {
                nodes.put(node.id(), node);
            }
        } catch (IOException e) {
            log.error("Unable to read cluster definitions", e);
        }
    }

    /**
     * Determines who the local controller node is.
     */
    private void establishSelfIdentity() {
        // Establishes the controller's own identity.
        IpPrefix ip = valueOf(System.getProperty("onos.ip", "127.0.1.1"));
        localNode = nodes.get(new NodeId(ip.toString()));

        // As a fall-back, let's make sure we at least know who we are.
        if (localNode == null) {
            localNode = new DefaultControllerNode(new NodeId(ip.toString()), ip);
            nodes.put(localNode.id(), localNode);
        }
        states.put(localNode.id(), State.ACTIVE);
    }

    @Override
    public ControllerNode getLocalNode() {
        return localNode;
    }

    @Override
    public Set<ControllerNode> getNodes() {
        ImmutableSet.Builder<ControllerNode> builder = ImmutableSet.builder();
        return builder.addAll(nodes.values()).build();
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        return nodes.get(nodeId);
    }

    @Override
    public State getState(NodeId nodeId) {
        State state = states.get(nodeId);
        return state == null ? State.INACTIVE : state;
    }

    @Override
    public ControllerNode addNode(NodeId nodeId, IpPrefix ip, int tcpPort) {
        DefaultControllerNode node = new DefaultControllerNode(nodeId, ip, tcpPort);
        nodes.put(nodeId, node);
        communicationAdminService.addNode(node);
        return node;
    }

    @Override
    public void removeNode(NodeId nodeId) {
        if (nodeId.equals(localNode.id())) {
            // FIXME: this is still broken
            // We are being ejected from the cluster, so remove all other nodes.
            communicationAdminService.clearAllNodesAndStreams();
            nodes.clear();
        } else {
            // Remove the other node.
            DefaultControllerNode node = nodes.remove(nodeId);
            if (node != null) {
                communicationAdminService.removeNode(node);
            }
        }
    }

    // Entity to handle back calls from the connection manager.
    private class InnerNodesDelegate implements ClusterNodesDelegate {
        @Override
        public DefaultControllerNode nodeDetected(NodeId nodeId, IpPrefix ip, int tcpPort) {
            DefaultControllerNode node = nodes.get(nodeId);
            if (node == null) {
                node = (DefaultControllerNode) addNode(nodeId, ip, tcpPort);
            }
            states.put(nodeId, State.ACTIVE);
            return node;
        }
        @Override
        public void nodeVanished(NodeId nodeId) {
            states.put(nodeId, State.INACTIVE);
        }

        @Override
        public void nodeRemoved(NodeId nodeId) {
            removeNode(nodeId);
        }
    }

}
