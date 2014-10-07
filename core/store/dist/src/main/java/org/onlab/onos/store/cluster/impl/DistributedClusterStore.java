package org.onlab.onos.store.cluster.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableSet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterStore;
import org.onlab.onos.cluster.ClusterStoreDelegate;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.AbstractStore;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationAdminService;
import org.onlab.onos.store.cluster.messaging.impl.ClusterCommunicationManager;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.onlab.onos.cluster.ControllerNode.State;
import static org.onlab.packet.IpPrefix.valueOf;

/**
 * Distributed implementation of the cluster nodes store.
 */
//@Component(immediate = true)
//@Service
public class DistributedClusterStore
        extends AbstractStore<ClusterEvent, ClusterStoreDelegate>
        implements ClusterStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DefaultControllerNode localNode;
    private final Map<NodeId, DefaultControllerNode> nodes = new ConcurrentHashMap<>();
    private final Map<NodeId, State> states = new ConcurrentHashMap<>();
    private final Cache<NodeId, ControllerNode> livenessCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(ClusterCommunicationManager.HEART_BEAT_INTERVAL_MILLIS * 3, TimeUnit.MILLISECONDS)
            .removalListener(new LivenessCacheRemovalListener()).build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterCommunicationAdminService clusterCommunicationAdminService;

    private final ClusterNodesDelegate nodesDelegate = new InnerNodesDelegate();

    @Activate
    public void activate() throws IOException {
        loadClusterDefinition();
        establishSelfIdentity();

        // Start-up the comm service and prime it with the loaded nodes.
        clusterCommunicationAdminService.initialize(localNode, nodesDelegate);
        for (DefaultControllerNode node : nodes.values()) {
            clusterCommunicationAdminService.addNode(node);
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
        clusterCommunicationAdminService.addNode(node);
        return node;
    }

    @Override
    public void removeNode(NodeId nodeId) {
        if (nodeId.equals(localNode.id())) {
            nodes.clear();
            nodes.put(localNode.id(), localNode);

        } else {
            // Remove the other node.
            DefaultControllerNode node = nodes.remove(nodeId);
            if (node != null) {
                clusterCommunicationAdminService.removeNode(node);
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
            livenessCache.put(nodeId, node);
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

    private class LivenessCacheRemovalListener implements RemovalListener<NodeId, ControllerNode> {

        @Override
        public void onRemoval(RemovalNotification<NodeId, ControllerNode> entry) {
            NodeId nodeId = entry.getKey();
            log.warn("Failed to receive heartbeats from controller: " + nodeId);
            nodesDelegate.nodeVanished(nodeId);
        }
    }
}
