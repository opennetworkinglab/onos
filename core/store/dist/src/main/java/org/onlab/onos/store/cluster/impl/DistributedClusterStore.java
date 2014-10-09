package org.onlab.onos.store.cluster.impl;

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
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.ClusterMessageHandler;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.cluster.messaging.impl.ClusterMessageSerializer;
import org.onlab.onos.store.cluster.messaging.impl.MessageSubjectSerializer;
import org.onlab.onos.store.serializers.KryoPoolUtil;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoPool;
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
//@Component(immediate = true)
//@Service
public class DistributedClusterStore
        extends AbstractStore<ClusterEvent, ClusterStoreDelegate>
        implements ClusterStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ControllerNode localNode;
    private final Map<NodeId, ControllerNode> nodes = new ConcurrentHashMap<>();
    private final Map<NodeId, State> states = new ConcurrentHashMap<>();

    private static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoPool.newBuilder()
                    .register(KryoPoolUtil.API)
                    .register(ClusterMessage.class, new ClusterMessageSerializer())
                    .register(ClusterMembershipEvent.class)
                    .register(byte[].class)
                    .register(MessageSubject.class, new MessageSubjectSerializer())
                    .build()
                    .populate(1);
        }
    };

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterCommunicationAdminService clusterCommunicationAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterMonitorService clusterMonitor;

    private final ClusterNodesDelegate nodesDelegate = new InnerNodesDelegate();

    @Activate
    public void activate() throws IOException {
        loadClusterDefinition();
        establishSelfIdentity();

        clusterCommunicator.addSubscriber(
                ClusterManagementMessageSubjects.CLUSTER_MEMBERSHIP_EVENT,
                new ClusterMembershipEventListener());

        // Start-up the monitor service and prime it with the loaded nodes.
        clusterMonitor.initialize(localNode, nodesDelegate);

        for (ControllerNode node : nodes.values()) {
            clusterMonitor.addNode(node);
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
        addNodeInternal(node);

        try {
            clusterCommunicator.broadcast(
                    new ClusterMessage(
                            localNode.id(),
                            ClusterManagementMessageSubjects.CLUSTER_MEMBERSHIP_EVENT,
                            SERIALIZER.encode(
                                    new ClusterMembershipEvent(
                                            ClusterMembershipEventType.NEW_MEMBER,
                                            node))));
        } catch (IOException e) {
            // TODO: In a setup where cluster membership is not static (i.e. not everything has the same picture)
            // we'll need a more consistent/dependable way to replicate membership events.
            log.error("Failed to notify peers of a new cluster member", e);
        }

        return node;
    }

    private void addNodeInternal(ControllerNode node) {
        nodes.put(node.id(), node);
    }

    @Override
    public void removeNode(NodeId nodeId) {
        ControllerNode node = removeNodeInternal(nodeId);

        if (node != null) {
            try {
                clusterCommunicator.broadcast(
                        new ClusterMessage(
                                localNode.id(),
                                ClusterManagementMessageSubjects.CLUSTER_MEMBERSHIP_EVENT,
                                SERIALIZER.encode(
                                        new ClusterMembershipEvent(
                                                ClusterMembershipEventType.LEAVING_MEMBER,
                                                node))));
            } catch (IOException e) {
                // TODO: In a setup where cluster membership is not static (i.e. not everything has the same picture)
                // we'll need a more consistent/dependable way to replicate membership events.
                log.error("Failed to notify peers of a existing cluster member leaving.", e);
            }
        }

    }

    private ControllerNode removeNodeInternal(NodeId nodeId) {
        if (nodeId.equals(localNode.id())) {
            nodes.clear();
            nodes.put(localNode.id(), localNode);
            return localNode;

        }
        // Remove the other node.
        ControllerNode node = nodes.remove(nodeId);
        return node;
    }

    private class ClusterMembershipEventListener implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {

            log.info("Received cluster membership event from peer: {}", message.sender());
            ClusterMembershipEvent event = (ClusterMembershipEvent) SERIALIZER.decode(message.payload());
            if (event.type() == ClusterMembershipEventType.NEW_MEMBER) {
                log.info("Node {} is added", event.node().id());
                addNodeInternal(event.node());
            }
            if (event.type() == ClusterMembershipEventType.LEAVING_MEMBER) {
                log.info("Node {} is removed ", event.node().id());
                removeNodeInternal(event.node().id());
            }
        }
    }

    // Entity to handle back calls from the connection manager.
    private class InnerNodesDelegate implements ClusterNodesDelegate {
        @Override
        public ControllerNode nodeDetected(NodeId nodeId, IpPrefix ip, int tcpPort) {
            ControllerNode node = nodes.get(nodeId);
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
