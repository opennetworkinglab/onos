/*
 * Copyright 2014-present Open Networking Laboratory
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.joda.time.DateTime;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cfg.ComponentConfigService;
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
import org.onosproject.store.serializers.StoreSerializer;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.cluster.ClusterEvent.Type.INSTANCE_ACTIVATED;
import static org.onosproject.cluster.ClusterEvent.Type.INSTANCE_DEACTIVATED;
import static org.onosproject.cluster.ClusterEvent.Type.INSTANCE_READY;
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

    private static final int DEFAULT_HEARTBEAT_INTERVAL = 100;
    @Property(name = "heartbeatInterval", intValue = DEFAULT_HEARTBEAT_INTERVAL,
            label = "Interval time to send heartbeat to other controller nodes (millisecond)")
    private int heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;

    private static final int DEFAULT_PHI_FAILURE_THRESHOLD = 10;
    @Property(name = "phiFailureThreshold", intValue = DEFAULT_PHI_FAILURE_THRESHOLD,
            label = "the value of Phi threshold to detect accrual failure")
    private int phiFailureThreshold = DEFAULT_PHI_FAILURE_THRESHOLD;

    private static final StoreSerializer SERIALIZER = StoreSerializer.using(
                  KryoNamespace.newBuilder()
                    .register(KryoNamespaces.API)
                    .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                    .register(HeartbeatMessage.class)
                    .build("ClusterStore"));

    private static final String INSTANCE_ID_NULL = "Instance ID cannot be null";

    private final Map<NodeId, ControllerNode> allNodes = Maps.newConcurrentMap();
    private final Map<NodeId, State> nodeStates = Maps.newConcurrentMap();
    private final Map<NodeId, DateTime> nodeStateLastUpdatedTimes = Maps.newConcurrentMap();

    private ScheduledExecutorService heartBeatSender = Executors.newSingleThreadScheduledExecutor(
            groupedThreads("onos/cluster/membership", "heartbeat-sender", log));
    private ExecutorService heartBeatMessageHandler = Executors.newSingleThreadExecutor(
            groupedThreads("onos/cluster/membership", "heartbeat-receiver", log));

    private PhiAccrualFailureDetector failureDetector;

    private ControllerNode localNode;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataService clusterMetadataService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MessagingService messagingService;

    // This must be optional to avoid a cyclic dependency
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY,
               bind = "bindComponentConfigService",
               unbind = "unbindComponentConfigService",
               policy = ReferencePolicy.DYNAMIC)
    protected ComponentConfigService cfgService;

    /**
     * Hook for wiring up optional reference to a service.
     *
     * @param service service being announced
     */
    protected void bindComponentConfigService(ComponentConfigService service) {
        if (cfgService == null) {
            cfgService = service;
            cfgService.registerProperties(getClass());
            readComponentConfiguration();
        }
    }

    /**
     * Hook for unwiring optional reference to a service.
     *
     * @param service service being withdrawn
     */
    protected void unbindComponentConfigService(ComponentConfigService service) {
        if (cfgService == service) {
            cfgService.unregisterProperties(getClass(), false);
            cfgService = null;
        }
    }

    @Activate
    public void activate() {
        localNode = clusterMetadataService.getLocalNode();

        messagingService.registerHandler(HEARTBEAT_MESSAGE,
                                         new HeartbeatMessageHandler(), heartBeatMessageHandler);

        failureDetector = new PhiAccrualFailureDetector();

        heartBeatSender.scheduleWithFixedDelay(this::heartbeat, 0,
                                               heartbeatInterval, TimeUnit.MILLISECONDS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        messagingService.unregisterHandler(HEARTBEAT_MESSAGE);
        heartBeatSender.shutdownNow();
        heartBeatMessageHandler.shutdownNow();

        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration();
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
        return MoreObjects.firstNonNull(nodeStates.get(nodeId), State.INACTIVE);
    }

    @Override
    public void markFullyStarted(boolean started) {
        updateState(localNode.id(), started ? State.READY : State.ACTIVE);
    }

    @Override
    public ControllerNode addNode(NodeId nodeId, IpAddress ip, int tcpPort) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
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
        State currentState = nodeStates.get(nodeId);
        if (!Objects.equals(currentState, newState)) {
            nodeStates.put(nodeId, newState);
            nodeStateLastUpdatedTimes.put(nodeId, DateTime.now());
            notifyStateChange(nodeId, currentState, newState);
        }
    }

    private void heartbeat() {
        try {
            Set<ControllerNode> peers = allNodes.values()
                    .stream()
                    .filter(node -> !(node.id().equals(localNode.id())))
                    .collect(Collectors.toSet());
            State state = nodeStates.get(localNode.id());
            byte[] hbMessagePayload = SERIALIZER.encode(new HeartbeatMessage(localNode, state));
            peers.forEach((node) -> {
                heartbeatToPeer(hbMessagePayload, node);
                State currentState = nodeStates.get(node.id());
                double phi = failureDetector.phi(node.id());
                if (phi >= phiFailureThreshold) {
                    if (currentState.isActive()) {
                        updateState(node.id(), State.INACTIVE);
                    }
                } else {
                    if (currentState == State.INACTIVE) {
                        updateState(node.id(), State.ACTIVE);
                    }
                }
            });
        } catch (Exception e) {
            log.debug("Failed to send heartbeat", e);
        }
    }

    private void notifyStateChange(NodeId nodeId, State oldState, State newState) {
        if (oldState != newState) {
            ControllerNode node = allNodes.get(nodeId);
            // Either this node or that node is no longer part of the same cluster
            if (node == null) {
                log.debug("Could not find node {} in the cluster, ignoring state change", nodeId);
                return;
            }
            ClusterEvent.Type type = newState == State.READY ? INSTANCE_READY :
                    newState == State.ACTIVE ? INSTANCE_ACTIVATED :
                            INSTANCE_DEACTIVATED;
            notifyDelegate(new ClusterEvent(type, node));
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

    private class HeartbeatMessageHandler implements BiConsumer<Endpoint, byte[]> {
        @Override
        public void accept(Endpoint sender, byte[] message) {
            HeartbeatMessage hb = SERIALIZER.decode(message);
            if (clusterMetadataService.getClusterMetadata().getNodes().contains(hb.source())) {
                failureDetector.report(hb.source().id());
                updateState(hb.source().id(), hb.state);
            }
        }
    }

    private static class HeartbeatMessage {
        private ControllerNode source;
        private State state;

        public HeartbeatMessage(ControllerNode source, State state) {
            this.source = source;
            this.state = state != null ? state : State.ACTIVE;
        }

        public ControllerNode source() {
            return source;
        }
    }

    @Override
    public DateTime getLastUpdated(NodeId nodeId) {
        return nodeStateLastUpdatedTimes.get(nodeId);
    }

    /**
     * Extracts properties from the component configuration.
     *
     */
    private void readComponentConfiguration() {
        Set<ConfigProperty> configProperties = cfgService.getProperties(getClass().getName());
        for (ConfigProperty property : configProperties) {
            if ("heartbeatInterval".equals(property.name())) {
                String s = property.value();
                if (s == null) {
                    setHeartbeatInterval(DEFAULT_HEARTBEAT_INTERVAL);
                    log.info("Heartbeat interval time is not configured, default value is {}",
                            DEFAULT_HEARTBEAT_INTERVAL);
                } else {
                    int newHeartbeatInterval = isNullOrEmpty(s) ? DEFAULT_HEARTBEAT_INTERVAL
                                                                : Integer.parseInt(s.trim());
                    if (newHeartbeatInterval > 0 && heartbeatInterval != newHeartbeatInterval) {
                        heartbeatInterval = newHeartbeatInterval;
                        restartHeartbeatSender();
                    }
                    log.info("Configured. Heartbeat interval time is configured to {}",
                            heartbeatInterval);
                }
            }
            if ("phiFailureThreshold".equals(property.name())) {
                String s = property.value();
                if (s == null) {
                    setPhiFailureThreshold(DEFAULT_PHI_FAILURE_THRESHOLD);
                    log.info("Phi failure threshold is not configured, default value is {}",
                            DEFAULT_PHI_FAILURE_THRESHOLD);
                } else {
                    int newPhiFailureThreshold = isNullOrEmpty(s) ? DEFAULT_HEARTBEAT_INTERVAL
                                                                  : Integer.parseInt(s.trim());
                    setPhiFailureThreshold(newPhiFailureThreshold);
                    log.info("Configured. Phi failure threshold is configured to {}",
                            phiFailureThreshold);
                }
            }
        }
    }

    /**
     * Sets heartbeat interval between the termination of one execution of heartbeat
     * and the commencement of the next.
     *
     * @param interval term between each heartbeat
     */
    private void setHeartbeatInterval(int interval) {
        try {
            checkArgument(interval > 0, "Interval must be greater than zero");
            heartbeatInterval = interval;
        } catch (IllegalArgumentException e) {
            log.warn(e.getMessage());
            heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
        }
    }

    /**
     * Sets Phi failure threshold.
     * Phi is based on a paper titled: "The φ Accrual Failure Detector" by Hayashibara, et al.
     *
     * @param threshold
     */
    private void setPhiFailureThreshold(int threshold) {
        phiFailureThreshold = threshold;
    }

    /**
     * Restarts heartbeatSender executor.
     */
    private void restartHeartbeatSender() {
        try {
            ScheduledExecutorService prevSender = heartBeatSender;
            heartBeatSender = Executors.newSingleThreadScheduledExecutor(
                    groupedThreads("onos/cluster/membership", "heartbeat-sender-%d", log));
            heartBeatSender.scheduleWithFixedDelay(this::heartbeat, 0,
                                                   heartbeatInterval, TimeUnit.MILLISECONDS);
            prevSender.shutdown();
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
