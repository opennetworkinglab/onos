/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.cluster.impl;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.netty.Endpoint;
import org.onlab.netty.Message;
import org.onlab.netty.MessageHandler;
import org.onlab.netty.NettyMessagingService;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.ControllerNode.State;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.hazelcast.util.AddressUtil;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * ClusterService implementation that employs an accrual failure
 * detector to identify cluster member up/down status.
 */
@Component(immediate = true, enabled = false)
@Service
public class ClusterManager implements ClusterService, ClusterAdminService {

    private final Logger log = getLogger(getClass());

    protected final AbstractListenerRegistry<ClusterEvent, ClusterEventListener>
        listenerRegistry = new AbstractListenerRegistry<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    // TODO: make these configurable.
    private static final int HEARTBEAT_FD_PORT = 2419;
    private static final int HEARTBEAT_INTERVAL_MS = 100;
    private static final int PHI_FAILURE_THRESHOLD = 10;

    private static final String CONFIG_DIR = "../config";
    private static final String CLUSTER_DEFINITION_FILE = "cluster.json";

    private ClusterDefinition clusterDefinition;

    private Set<ControllerNode> seedNodes;
    private final Map<NodeId, ControllerNode> allNodes = Maps.newConcurrentMap();
    private final Map<NodeId, State> nodeStates = Maps.newConcurrentMap();
    private NettyMessagingService messagingService = new NettyMessagingService();
    private ScheduledExecutorService heartBeatSender = Executors.newSingleThreadScheduledExecutor(
            groupedThreads("onos/cluster/membership", "heartbeat-sender"));
    private ExecutorService heartBeatMessageHandler = Executors.newSingleThreadExecutor(
            groupedThreads("onos/cluster/membership", "heartbeat-receiver"));

    private static final String HEARTBEAT_MESSAGE = "onos-cluster-heartbeat";


    private PhiAccrualFailureDetector failureDetector;

    private ControllerNode localNode;

    private static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(HeartbeatMessage.class)
                .build()
                .populate(1);
        }
    };

    private static final String INSTANCE_ID_NULL = "Instance ID cannot be null";

    @Activate
    public void activate() {

        File clusterDefinitionFile = new File(CONFIG_DIR, CLUSTER_DEFINITION_FILE);

        try {
            clusterDefinition = new ClusterDefinitionStore(clusterDefinitionFile.getPath()).read();
            seedNodes = ImmutableSet.copyOf(clusterDefinition.getNodes())
                            .stream()
                            .map(nodeInfo -> new DefaultControllerNode(
                                        new NodeId(nodeInfo.getId()),
                                        IpAddress.valueOf(nodeInfo.getIp()),
                                        nodeInfo.getTcpPort()))
                            .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read cluster definition.", e);
        }

        seedNodes.forEach(node -> {
            allNodes.put(node.id(), node);
            nodeStates.put(node.id(), State.INACTIVE);
        });

        establishSelfIdentity();

        messagingService = new NettyMessagingService(HEARTBEAT_FD_PORT);

        try {
            messagingService.activate();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to cleanly initialize membership and"
                    + " failure detector communication channel.", e);
        }
        messagingService.registerHandler(
                HEARTBEAT_MESSAGE,
                new HeartbeatMessageHandler(),
                heartBeatMessageHandler);

        eventDispatcher.addSink(ClusterEvent.class, listenerRegistry);
        failureDetector = new PhiAccrualFailureDetector();

        heartBeatSender.scheduleWithFixedDelay(
                this::heartbeat,
                0,
                HEARTBEAT_INTERVAL_MS,
                TimeUnit.MILLISECONDS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        try {
            messagingService.deactivate();
        } catch (Exception e) {
            log.trace("Failed to cleanly shutdown cluster membership messaging", e);
        }

        heartBeatSender.shutdownNow();
        heartBeatMessageHandler.shutdownNow();
        eventDispatcher.removeSink(ClusterEvent.class);

        log.info("Stopped");
    }

    @Override
    public ControllerNode getLocalNode() {
        return localNode;
    }

    @Override
    public Set<ControllerNode> getNodes() {
        return ImmutableSet.copyOf(allNodes.values());
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return allNodes.get(nodeId);
    }

    @Override
    public State getState(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return nodeStates.get(nodeId);
    }

    @Override
    public void addListener(ClusterEventListener listener) {
        checkNotNull(listener, "Listener must not be null");
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(ClusterEventListener listener) {
        checkNotNull(listener, "Listener must not be null");
        listenerRegistry.removeListener(listener);
    }

    @Override
    public ControllerNode addNode(NodeId nodeId, IpAddress ip, int tcpPort) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        checkNotNull(ip, "IP address must not be null");
        checkArgument(tcpPort > 5000, "Tcp port must be greater than 5000");
        ControllerNode node = new DefaultControllerNode(nodeId, ip, tcpPort);
        allNodes.put(node.id(), node);
        nodeStates.put(nodeId, State.INACTIVE);
        eventDispatcher.post(new ClusterEvent(ClusterEvent.Type.INSTANCE_ADDED, node));
        return node;
    }

    @Override
    public void removeNode(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        ControllerNode node = allNodes.remove(nodeId);
        if (node != null) {
            nodeStates.remove(nodeId);
            eventDispatcher.post(new ClusterEvent(ClusterEvent.Type.INSTANCE_REMOVED, node));
        }
    }

    private void establishSelfIdentity() {
        try {
            IpAddress ip = findLocalIp();
            localNode = new DefaultControllerNode(new NodeId(ip.toString()), ip);
            allNodes.put(localNode.id(), localNode);
            nodeStates.put(localNode.id(), State.ACTIVE);
            log.info("Local Node: {}", localNode);
        } catch (SocketException e) {
            throw new IllegalStateException("Cannot determine local IP", e);
        }
    }

    private void heartbeat() {
        try {
            Set<ControllerNode> peers = allNodes.values()
                    .stream()
                    .filter(node -> !(node.id().equals(localNode.id())))
                    .collect(Collectors.toSet());
            byte[] hbMessagePayload = SERIALIZER.encode(new HeartbeatMessage(localNode, peers));
            peers.forEach((node) -> {
                heartbeatToPeer(hbMessagePayload, node);
                State currentState = nodeStates.get(node.id());
                double phi = failureDetector.phi(node.id());
                if (phi >= PHI_FAILURE_THRESHOLD) {
                    if (currentState == State.ACTIVE) {
                        nodeStates.put(node.id(), State.INACTIVE);
                        notifyStateChange(node.id(), State.ACTIVE, State.INACTIVE);
                    }
                } else {
                    if (currentState == State.INACTIVE) {
                        nodeStates.put(node.id(), State.ACTIVE);
                        notifyStateChange(node.id(), State.INACTIVE, State.ACTIVE);
                    }
                }
            });
        } catch (Exception e) {
            log.debug("Failed to send heartbeat", e);
        }
    }

    private void notifyStateChange(NodeId nodeId, State oldState, State newState) {
        if (newState == State.ACTIVE) {
            eventDispatcher.post(new ClusterEvent(ClusterEvent.Type.INSTANCE_ACTIVATED, allNodes.get(nodeId)));
        } else {
            eventDispatcher.post(new ClusterEvent(ClusterEvent.Type.INSTANCE_DEACTIVATED, allNodes.get(nodeId)));
        }
    }

    private void heartbeatToPeer(byte[] messagePayload, ControllerNode peer) {
        Endpoint remoteEp = new Endpoint(peer.ip(), HEARTBEAT_FD_PORT);
        try {
            messagingService.sendAsync(remoteEp, HEARTBEAT_MESSAGE, messagePayload);
        } catch (IOException e) {
            log.debug("Sending heartbeat to {} failed", remoteEp, e);
        }
    }

    private class HeartbeatMessageHandler implements MessageHandler {
        @Override
        public void handle(Message message) throws IOException {
            HeartbeatMessage hb = SERIALIZER.decode(message.payload());
            failureDetector.report(hb.source().id());
            hb.knownPeers().forEach(node -> {
                allNodes.put(node.id(), node);
            });
        }
    }

    private static class HeartbeatMessage {
        private ControllerNode source;
        private Set<ControllerNode> knownPeers;

        public HeartbeatMessage(ControllerNode source, Set<ControllerNode> members) {
            this.source = source;
            this.knownPeers = ImmutableSet.copyOf(members);
        }

        public ControllerNode source() {
            return source;
        }

        public Set<ControllerNode> knownPeers() {
            return knownPeers;
        }
    }

    private IpAddress findLocalIp() throws SocketException {
        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses =  iface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                IpAddress ip = IpAddress.valueOf(inetAddresses.nextElement());
                if (AddressUtil.matchInterface(ip.toString(), clusterDefinition.getIpPrefix())) {
                    return ip;
                }
            }
        }
        throw new IllegalStateException("Unable to determine local ip");
    }
}
