/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.joda.time.DateTime;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ClusterStore;
import org.onosproject.cluster.ClusterStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.ControllerNode.State;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
@Service
/**
 * Distributed cluster nodes store that employs an accrual failure
 * detector to identify cluster member up/down status.
 */
public class DistributedClusterStore
        extends AbstractStore<ClusterEvent, ClusterStoreDelegate>
        implements ClusterStore {

    private static final Logger log = getLogger(DistributedClusterStore.class);

    public static final String HEARTBEAT_MESSAGE = "onos-cluster-heartbeat";

    // TODO: make these configurable.
    private static final int HEARTBEAT_INTERVAL_MS = 100;
    private static final int PHI_FAILURE_THRESHOLD = 10;

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

    private final Map<NodeId, ControllerNode> allNodes = Maps.newConcurrentMap();
    private final Map<NodeId, State> nodeStates = Maps.newConcurrentMap();
    private final Map<NodeId, DateTime> nodeStateLastUpdatedTimes = Maps.newConcurrentMap();
    private ScheduledExecutorService heartBeatSender = Executors.newSingleThreadScheduledExecutor(
            groupedThreads("onos/cluster/membership", "heartbeat-sender"));
    private ExecutorService heartBeatMessageHandler = Executors.newSingleThreadExecutor(
            groupedThreads("onos/cluster/membership", "heartbeat-receiver"));

    private PhiAccrualFailureDetector failureDetector;

    private ControllerNode localNode;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataService clusterMetadataService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MessagingService messagingService;

    @Activate
    public void activate() {
        localNode = clusterMetadataService.getLocalNode();

        messagingService.registerHandler(HEARTBEAT_MESSAGE,
                                         new HeartbeatMessageHandler(), heartBeatMessageHandler);

        failureDetector = new PhiAccrualFailureDetector();

        heartBeatSender.scheduleWithFixedDelay(this::heartbeat, 0,
                                               HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        messagingService.unregisterHandler(HEARTBEAT_MESSAGE);
        heartBeatSender.shutdownNow();
        heartBeatMessageHandler.shutdownNow();

        log.info("Stopped");
    }

    @Override
    public void setDelegate(ClusterStoreDelegate delegate) {
        checkNotNull(delegate, "Delegate cannot be null");
        this.delegate = delegate;
    }

    @Override
    public void unsetDelegate(ClusterStoreDelegate delegate) {
        this.delegate = null;
    }

    @Override
    public boolean hasDelegate() {
        return this.delegate != null;
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
    public ControllerNode addNode(NodeId nodeId, IpAddress ip, int tcpPort) {
        ControllerNode node = new DefaultControllerNode(nodeId, ip, tcpPort);
        addNode(node);
        return node;
    }

    @Override
    public void removeNode(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        ControllerNode node = allNodes.remove(nodeId);
        if (node != null) {
            nodeStates.remove(nodeId);
            notifyDelegate(new ClusterEvent(ClusterEvent.Type.INSTANCE_REMOVED, node));
        }
    }

    private void addNode(ControllerNode node) {
        allNodes.put(node.id(), node);
        updateState(node.id(), node.equals(localNode) ? State.ACTIVE : State.INACTIVE);
        notifyDelegate(new ClusterEvent(ClusterEvent.Type.INSTANCE_ADDED, node));
    }

    private void updateState(NodeId nodeId, State newState) {
        nodeStates.put(nodeId, newState);
        nodeStateLastUpdatedTimes.put(nodeId, DateTime.now());
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
                        updateState(node.id(), State.INACTIVE);
                        notifyStateChange(node.id(), State.ACTIVE, State.INACTIVE);
                    }
                } else {
                    if (currentState == State.INACTIVE) {
                        updateState(node.id(), State.ACTIVE);
                        notifyStateChange(node.id(), State.INACTIVE, State.ACTIVE);
                    }
                }
            });
        } catch (Exception e) {
            log.debug("Failed to send heartbeat", e);
        }
    }

    private void notifyStateChange(NodeId nodeId, State oldState, State newState) {
        ControllerNode node = allNodes.get(nodeId);
        if (newState == State.ACTIVE) {
            notifyDelegate(new ClusterEvent(ClusterEvent.Type.INSTANCE_ACTIVATED, node));
        } else {
            notifyDelegate(new ClusterEvent(ClusterEvent.Type.INSTANCE_DEACTIVATED, node));
        }
    }

    private void heartbeatToPeer(byte[] messagePayload, ControllerNode peer) {
        Endpoint remoteEp = new Endpoint(peer.ip(), peer.tcpPort());
        messagingService.sendAsync(remoteEp, HEARTBEAT_MESSAGE, messagePayload).whenComplete((result, error) -> {
            if (error != null) {
                log.trace("Sending heartbeat to {} failed", remoteEp, error);
            }
        });
    }

    private class HeartbeatMessageHandler implements Consumer<byte[]> {
        @Override
        public void accept(byte[] message) {
            HeartbeatMessage hb = SERIALIZER.decode(message);
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

    @Override
    public DateTime getLastUpdated(NodeId nodeId) {
        return nodeStateLastUpdatedTimes.get(nodeId);
    }

}
