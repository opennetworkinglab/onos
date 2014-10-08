package org.onlab.onos.store.cluster.messaging.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import org.onlab.netty.Response;
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
    public boolean broadcast(ClusterMessage message) {
        boolean ok = true;
        for (ControllerNode node : clusterService.getNodes()) {
            if (!node.equals(localNode)) {
                ok = unicast(message, node.id()) && ok;
            }
        }
        return ok;
    }

    @Override
    public boolean multicast(ClusterMessage message, Set<NodeId> nodes) {
        boolean ok = true;
        for (NodeId nodeId : nodes) {
            if (!nodeId.equals(localNode.id())) {
                ok = unicast(message, nodeId) && ok;
            }
        }
        return ok;
    }

    @Override
    public boolean unicast(ClusterMessage message, NodeId toNodeId) {
        ControllerNode node = clusterService.getNode(toNodeId);
        checkArgument(node != null, "Unknown nodeId: %s", toNodeId);
        Endpoint nodeEp = new Endpoint(node.ip().toString(), node.tcpPort());
        try {
            log.info("sending...");
            Response resp = messagingService.sendAndReceive(nodeEp,
                    message.subject().value(), SERIALIZER.encode(message));
            resp.get(1, TimeUnit.SECONDS);
            log.info("sent...");
            return true;
        } catch (IOException | TimeoutException e) {
            log.error("Failed to send cluster message to nodeId: " + toNodeId, e);
        }

        return false;
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
        broadcast(new ClusterMessage(
                localNode.id(),
                new MessageSubject("CLUSTER_MEMBERSHIP_EVENT"),
                SERIALIZER.encode(new ClusterMembershipEvent(ClusterMembershipEventType.LEAVING_MEMBER, node))));
        //members.remove(node.id());
    }

    // Sends a heart beat to all peers.
    private class KeepAlive extends TimerTask {

        @Override
        public void run() {
            broadcast(new ClusterMessage(
                localNode.id(),
                new MessageSubject("CLUSTER_MEMBERSHIP_EVENT"),
                SERIALIZER.encode(new ClusterMembershipEvent(ClusterMembershipEventType.HEART_BEAT, localNode))));
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

    // FIXME: revert static
    private class InternalClusterMessageHandler implements MessageHandler {

        private final ClusterMessageHandler handler;

        public InternalClusterMessageHandler(ClusterMessageHandler handler) {
            this.handler = handler;
        }

        @Override
        public void handle(Message message) {
            // FIXME: remove me
            log.info("InternalClusterMessageHandler.handle({})", message);
            try {
                log.info("before decode");
                ClusterMessage clusterMessage = SERIALIZER.decode(message.payload());
                log.info("Subject:({}), Sender:({})", clusterMessage.subject(), clusterMessage.sender());
                handler.handle(clusterMessage);
                message.respond("ACK".getBytes());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                log.error("failed", e);
            }
        }
    }
}
