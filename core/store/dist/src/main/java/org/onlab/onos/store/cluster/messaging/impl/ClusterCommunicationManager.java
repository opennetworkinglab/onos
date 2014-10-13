package org.onlab.onos.store.cluster.messaging.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.cluster.impl.ClusterMembershipEvent;
import org.onlab.onos.store.cluster.impl.ClusterMembershipEventType;
import org.onlab.onos.store.cluster.impl.ClusterNodesDelegate;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationAdminService;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.ClusterMessageHandler;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.serializers.ClusterMessageSerializer;
import org.onlab.onos.store.serializers.KryoPoolUtil;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.onos.store.serializers.MessageSubjectSerializer;
import org.onlab.util.KryoPool;
import org.onlab.netty.Endpoint;
import org.onlab.netty.Message;
import org.onlab.netty.MessageHandler;
import org.onlab.netty.MessagingService;
import org.onlab.netty.NettyMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service
public class ClusterCommunicationManager
        implements ClusterCommunicationService, ClusterCommunicationAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ControllerNode localNode;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    private ClusterNodesDelegate nodesDelegate;
    private final Timer timer = new Timer("onos-controller-heatbeats");
    public static final long HEART_BEAT_INTERVAL_MILLIS = 1000L;

    // TODO: This probably should not be a OSGi service.
    private MessagingService messagingService;

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

    @Activate
    public void activate() {
        localNode = clusterService.getLocalNode();
        NettyMessagingService netty = new NettyMessagingService(localNode.tcpPort());
        // FIXME: workaround until it becomes a service.
        try {
            netty.activate();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            log.error("NettyMessagingService#activate", e);
        }
        messagingService = netty;
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        // TODO: cleanup messageingService if needed.
        log.info("Stopped");
    }

    @Override
    public boolean broadcast(ClusterMessage message) throws IOException {
        boolean ok = true;
        for (ControllerNode node : clusterService.getNodes()) {
            if (!node.equals(localNode)) {
                ok = unicast(message, node.id()) && ok;
            }
        }
        return ok;
    }

    @Override
    public boolean multicast(ClusterMessage message, Set<NodeId> nodes) throws IOException {
        boolean ok = true;
        for (NodeId nodeId : nodes) {
            if (!nodeId.equals(localNode.id())) {
                ok = unicast(message, nodeId) && ok;
            }
        }
        return ok;
    }

    @Override
    public boolean unicast(ClusterMessage message, NodeId toNodeId) throws IOException {
        ControllerNode node = clusterService.getNode(toNodeId);
        checkArgument(node != null, "Unknown nodeId: %s", toNodeId);
        Endpoint nodeEp = new Endpoint(node.ip().toString(), node.tcpPort());
        try {
            messagingService.sendAsync(nodeEp,
                    message.subject().value(), SERIALIZER.encode(message));
            return true;
        } catch (IOException e) {
            log.error("Failed to send cluster message to nodeId: " + toNodeId, e);
            throw e;
        }
    }

    @Override
    public void addSubscriber(MessageSubject subject,
            ClusterMessageHandler subscriber) {
        messagingService.registerHandler(subject.value(), new InternalClusterMessageHandler(subscriber));
    }

    @Override
    public void initialize(ControllerNode localNode,
            ClusterNodesDelegate delegate) {
        this.localNode = localNode;
        this.nodesDelegate = delegate;
        this.addSubscriber(new MessageSubject("CLUSTER_MEMBERSHIP_EVENT"), new ClusterMemebershipEventHandler());
        timer.schedule(new KeepAlive(), 0, HEART_BEAT_INTERVAL_MILLIS);
    }

    @Override
    public void addNode(ControllerNode node) {
        //members.put(node.id(), node);
    }

    @Override
    public void removeNode(ControllerNode node) {
//        broadcast(new ClusterMessage(
//                localNode.id(),
//                new MessageSubject("CLUSTER_MEMBERSHIP_EVENT"),
//                SERIALIZER.encode(new ClusterMembershipEvent(ClusterMembershipEventType.LEAVING_MEMBER, node))));
        //members.remove(node.id());
    }

    // Sends a heart beat to all peers.
    private class KeepAlive extends TimerTask {

        @Override
        public void run() {
            try {
                broadcast(new ClusterMessage(
                    localNode.id(),
                    new MessageSubject("CLUSTER_MEMBERSHIP_EVENT"),
                    SERIALIZER.encode(new ClusterMembershipEvent(ClusterMembershipEventType.HEART_BEAT, localNode))));
            } catch (IOException e) {
                log.warn("I/O error while broadcasting heart beats.", e);
            }
        }
    }

    private class ClusterMemebershipEventHandler implements ClusterMessageHandler {

        @Override
        public void handle(ClusterMessage message) {

            ClusterMembershipEvent event = SERIALIZER.decode(message.payload());
            ControllerNode node = event.node();
            if (event.type() == ClusterMembershipEventType.HEART_BEAT) {
                log.info("Node {} sent a hearbeat", node.id());
                nodesDelegate.nodeDetected(node.id(), node.ip(), node.tcpPort());
            } else if (event.type() == ClusterMembershipEventType.LEAVING_MEMBER) {
                log.info("Node {} is leaving", node.id());
                nodesDelegate.nodeRemoved(node.id());
            } else if (event.type() == ClusterMembershipEventType.UNREACHABLE_MEMBER) {
                log.info("Node {} is unreachable", node.id());
                nodesDelegate.nodeVanished(node.id());
            }
        }
    }

    private final class InternalClusterMessageHandler implements MessageHandler {

        private final ClusterMessageHandler handler;

        public InternalClusterMessageHandler(ClusterMessageHandler handler) {
            this.handler = handler;
        }

        @Override
        public void handle(Message message) {
            try {
                ClusterMessage clusterMessage = SERIALIZER.decode(message.payload());
                handler.handle(clusterMessage);
            } catch (Exception e) {
                log.error("Exception caught during ClusterMessageHandler", e);
                throw e;
            }
        }
    }
}
