/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.store.impl;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipAdminService;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkMastershipStore;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipInfo;
import org.onosproject.mastership.MastershipStoreDelegate;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.mastership.MastershipEvent.Type.BACKUPS_CHANGED;
import static org.onosproject.mastership.MastershipEvent.Type.MASTER_CHANGED;
import static org.onosproject.mastership.MastershipEvent.Type.SUSPENDED;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true, enabled = false, service = VirtualNetworkMastershipStore.class)
public class ConsistentVirtualDeviceMastershipStore
        extends AbstractVirtualStore<MastershipEvent, MastershipStoreDelegate>
        implements VirtualNetworkMastershipStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipAdminService leadershipAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService clusterCommunicator;

    private NodeId localNodeId;

    private static final MessageSubject ROLE_RELINQUISH_SUBJECT =
            new MessageSubject("virtual-mastership-store-device-role-relinquish");

    private static final Pattern DEVICE_MASTERSHIP_TOPIC_PATTERN =
            Pattern.compile("vnet:(.*),device:(.*)");

    private ExecutorService eventHandler;
    private ExecutorService messageHandlingExecutor;
    private ScheduledExecutorService transferExecutor;
    private final LeadershipEventListener leadershipEventListener =
            new InternalDeviceMastershipEventListener();

    private static final String NODE_ID_NULL = "Node ID cannot be null";
    private static final String NETWORK_ID_NULL = "Network ID cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final int WAIT_BEFORE_MASTERSHIP_HANDOFF_MILLIS = 3000;

    public static final Serializer SERIALIZER = Serializer.using(
            KryoNamespace.newBuilder()
                    .register(KryoNamespaces.API)
                    .register(MastershipRole.class)
                    .register(MastershipEvent.class)
                    .register(MastershipEvent.Type.class)
                    .register(VirtualDeviceId.class)
                    .build("VirtualMastershipStore"));

    @Activate
    public void activate() {
        eventHandler = Executors.newSingleThreadExecutor(
                groupedThreads("onos/store/virtual/mastership", "event-handler", log));

        messageHandlingExecutor =
                Executors.newSingleThreadExecutor(
                        groupedThreads("onos/store/virtual/mastership", "message-handler", log));
        transferExecutor =
                Executors.newSingleThreadScheduledExecutor(
                        groupedThreads("onos/store/virtual/mastership", "mastership-transfer-executor", log));
        clusterCommunicator.addSubscriber(ROLE_RELINQUISH_SUBJECT,
                                          SERIALIZER::decode,
                                          this::relinquishLocalRole,
                                          SERIALIZER::encode,
                                          messageHandlingExecutor);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.addListener(leadershipEventListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clusterCommunicator.removeSubscriber(ROLE_RELINQUISH_SUBJECT);
        leadershipService.removeListener(leadershipEventListener);
        messageHandlingExecutor.shutdown();
        transferExecutor.shutdown();
        eventHandler.shutdown();
        log.info("Stopped");
    }

    @Override
    public CompletableFuture<MastershipRole> requestRole(NetworkId networkId,
                                                         DeviceId deviceId) {
        checkArgument(networkId != null, NETWORK_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        String leadershipTopic = createDeviceMastershipTopic(networkId, deviceId);
        Leadership leadership = leadershipService.runForLeadership(leadershipTopic);
        return CompletableFuture
                .completedFuture(localNodeId.equals(leadership.leaderNodeId()) ?
                                         MastershipRole.MASTER : MastershipRole.STANDBY);
    }

    @Override
    public MastershipRole getRole(NetworkId networkId, NodeId nodeId, DeviceId deviceId) {
        checkArgument(networkId != null, NETWORK_ID_NULL);
        checkArgument(nodeId != null, NODE_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        String leadershipTopic = createDeviceMastershipTopic(networkId, deviceId);
        Leadership leadership = leadershipService.getLeadership(leadershipTopic);
        NodeId leader = leadership == null ? null : leadership.leaderNodeId();
        List<NodeId> candidates = leadership == null ?
                ImmutableList.of() : ImmutableList.copyOf(leadership.candidates());
        return Objects.equal(nodeId, leader) ?
                MastershipRole.MASTER : candidates.contains(nodeId) ?
                MastershipRole.STANDBY : MastershipRole.NONE;
    }

    @Override
    public NodeId getMaster(NetworkId networkId, DeviceId deviceId) {
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        return leadershipService.getLeader(createDeviceMastershipTopic(networkId, deviceId));
    }

    @Override
    public RoleInfo getNodes(NetworkId networkId, DeviceId deviceId) {
        checkArgument(networkId != null, NETWORK_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);
        Leadership leadership = leadershipService.getLeadership(createDeviceMastershipTopic(networkId, deviceId));
        return new RoleInfo(leadership.leaderNodeId(), leadership.candidates());
    }

    @Override
    public MastershipInfo getMastership(NetworkId networkId, DeviceId deviceId) {
        checkArgument(networkId != null, NETWORK_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);
        Leadership leadership = leadershipService.getLeadership(createDeviceMastershipTopic(networkId, deviceId));
        return buildMastershipFromLeadership(leadership);
    }

    @Override
    public Set<DeviceId> getDevices(NetworkId networkId, NodeId nodeId) {
        checkArgument(networkId != null, NETWORK_ID_NULL);
        checkArgument(nodeId != null, NODE_ID_NULL);

        // FIXME This result contains REMOVED device.
        // MastershipService cannot listen to DeviceEvent to GC removed topic,
        // since DeviceManager depend on it.
        // Reference count, etc. at LeadershipService layer?
        return leadershipService
                .ownedTopics(nodeId)
                .stream()
                .filter(this::isVirtualMastershipTopic)
                .map(this::extractDeviceIdFromTopic)
                .collect(Collectors.toSet());
    }

    @Override
    public CompletableFuture<MastershipEvent> setMaster(NetworkId networkId,
                                                        NodeId nodeId, DeviceId deviceId) {
        checkArgument(networkId != null, NETWORK_ID_NULL);
        checkArgument(nodeId != null, NODE_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        String leadershipTopic = createDeviceMastershipTopic(networkId, deviceId);
        if (leadershipAdminService.promoteToTopOfCandidateList(leadershipTopic, nodeId)) {
            transferExecutor.schedule(() -> leadershipAdminService.transferLeadership(leadershipTopic, nodeId),
                                      WAIT_BEFORE_MASTERSHIP_HANDOFF_MILLIS, TimeUnit.MILLISECONDS);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public MastershipTerm getTermFor(NetworkId networkId, DeviceId deviceId) {
        checkArgument(networkId != null, NETWORK_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        String leadershipTopic = createDeviceMastershipTopic(networkId, deviceId);
        Leadership leadership = leadershipService.getLeadership(leadershipTopic);
        return leadership != null && leadership.leaderNodeId() != null ?
                MastershipTerm.of(leadership.leaderNodeId(),
                                  leadership.leader().term()) : null;
    }

    @Override
    public CompletableFuture<MastershipEvent> setStandby(NetworkId networkId,
                                                         NodeId nodeId,
                                                         DeviceId deviceId) {
        checkArgument(networkId != null, NETWORK_ID_NULL);
        checkArgument(nodeId != null, NODE_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        NodeId currentMaster = getMaster(networkId, deviceId);
        if (!nodeId.equals(currentMaster)) {
            return CompletableFuture.completedFuture(null);
        }

        String leadershipTopic = createDeviceMastershipTopic(networkId, deviceId);
        List<NodeId> candidates = leadershipService.getCandidates(leadershipTopic);

        NodeId newMaster = candidates.stream()
                .filter(candidate -> !Objects.equal(nodeId, candidate))
                .findFirst()
                .orElse(null);
        log.info("Transitioning to role {} for {}. Next master: {}",
                 newMaster != null ? MastershipRole.STANDBY : MastershipRole.NONE,
                 deviceId, newMaster);

        if (newMaster != null) {
            return setMaster(networkId, newMaster, deviceId);
        }
        return relinquishRole(networkId, nodeId, deviceId);
    }

    @Override
    public CompletableFuture<MastershipEvent> relinquishRole(NetworkId networkId,
                                                             NodeId nodeId,
                                                             DeviceId deviceId) {
        checkArgument(networkId != null, NETWORK_ID_NULL);
        checkArgument(nodeId != null, NODE_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        if (nodeId.equals(localNodeId)) {
            return relinquishLocalRoleByNetwork(networkId, deviceId);
        }

        log.debug("Forwarding request to relinquish "
                          + "role for vnet {} device {} to {}", deviceId, nodeId);
        return clusterCommunicator.sendAndReceive(
                new VirtualDeviceId(networkId, deviceId),
                ROLE_RELINQUISH_SUBJECT,
                SERIALIZER::encode,
                SERIALIZER::decode,
                nodeId);
    }

    private CompletableFuture<MastershipEvent> relinquishLocalRoleByNetwork(NetworkId networkId,
                                                                   DeviceId deviceId) {
        checkArgument(networkId != null, NETWORK_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        String leadershipTopic = createDeviceMastershipTopic(networkId, deviceId);
        if (!leadershipService.getCandidates(leadershipTopic).contains(localNodeId)) {
            return CompletableFuture.completedFuture(null);
        }
        MastershipEvent.Type eventType = localNodeId.equals(leadershipService.getLeader(leadershipTopic)) ?
                MastershipEvent.Type.MASTER_CHANGED : MastershipEvent.Type.BACKUPS_CHANGED;
        leadershipService.withdraw(leadershipTopic);
        return CompletableFuture.completedFuture(
            new MastershipEvent(eventType, deviceId, getMastership(networkId, deviceId)));
    }

    private CompletableFuture<MastershipEvent>
    relinquishLocalRole(VirtualDeviceId virtualDeviceId) {
        return relinquishLocalRoleByNetwork(virtualDeviceId.networkId,
                                            virtualDeviceId.deviceId);
    }

    @Override
    public void relinquishAllRole(NetworkId networkId, NodeId nodeId) {
        // Noop. LeadershipService already takes care of detecting and purging stale locks.
    }

    private MastershipInfo buildMastershipFromLeadership(Leadership leadership) {
        ImmutableMap.Builder<NodeId, MastershipRole> builder = ImmutableMap.builder();
        if (leadership.leaderNodeId() != null) {
            builder.put(leadership.leaderNodeId(), MastershipRole.MASTER);
        }
        leadership.candidates().forEach(nodeId -> builder.put(nodeId, MastershipRole.STANDBY));
        clusterService.getNodes().stream()
            .filter(node -> !leadership.candidates().contains(node.id()))
            .forEach(node -> builder.put(node.id(), MastershipRole.NONE));

        return new MastershipInfo(
            leadership.leader() != null ? leadership.leader().term() : 0,
            leadership.leader() != null
                ? Optional.of(leadership.leader().nodeId())
                : Optional.empty(),
            builder.build());
    }

    private class InternalDeviceMastershipEventListener
            implements LeadershipEventListener {

        @Override
        public boolean isRelevant(LeadershipEvent event) {
            Leadership leadership = event.subject();
            return isVirtualMastershipTopic(leadership.topic());
        }

        @Override
        public void event(LeadershipEvent event) {
            eventHandler.execute(() -> handleEvent(event));
        }

        private void handleEvent(LeadershipEvent event) {
            Leadership leadership = event.subject();

            NetworkId networkId = extractNetworkIdFromTopic(leadership.topic());
            DeviceId deviceId = extractDeviceIdFromTopic(leadership.topic());
            MastershipInfo mastershipInfo = event.type() != LeadershipEvent.Type.SERVICE_DISRUPTED
                ? buildMastershipFromLeadership(event.subject())
                : new MastershipInfo();

            switch (event.type()) {
                case LEADER_AND_CANDIDATES_CHANGED:
                    notifyDelegate(networkId, new MastershipEvent(BACKUPS_CHANGED, deviceId, mastershipInfo));
                    notifyDelegate(networkId, new MastershipEvent(MASTER_CHANGED, deviceId, mastershipInfo));
                    break;
                case LEADER_CHANGED:
                    notifyDelegate(networkId, new MastershipEvent(MASTER_CHANGED, deviceId, mastershipInfo));
                    break;
                case CANDIDATES_CHANGED:
                    notifyDelegate(networkId, new MastershipEvent(BACKUPS_CHANGED, deviceId, mastershipInfo));
                    break;
                case SERVICE_DISRUPTED:
                    notifyDelegate(networkId, new MastershipEvent(SUSPENDED, deviceId, mastershipInfo));
                    break;
                case SERVICE_RESTORED:
                    // Do nothing, wait for updates from peers
                    break;
                default:
            }
        }
    }

    private String createDeviceMastershipTopic(NetworkId networkId, DeviceId deviceId) {
        return String.format("vnet:%s,device:%s", networkId.toString(), deviceId.toString());
    }

    /**
     * Returns the virtual network identifier extracted from the topic.
     *
     * @param topic topic to extract virtual network identifier
     * @return an extracted virtual network identifier
     * @throws IllegalArgumentException the topic not match with the pattern
     * used for virtual network mastership store
     */
    private NetworkId extractNetworkIdFromTopic(String topic) {
        Matcher m = DEVICE_MASTERSHIP_TOPIC_PATTERN.matcher(topic);
        if (m.matches()) {
            return NetworkId.networkId(Long.getLong(m.group(1)));
        } else {
            throw new IllegalArgumentException("Invalid virtual mastership topic: "
                                                       + topic);
        }
    }

    /**
     * Returns the device identifier extracted from the topic.
     *
     * @param topic topic to extract device identifier
     * @return an extracted virtual device identifier
     * @throws IllegalArgumentException the topic not match with the pattern
     * used for virtual network mastership store
     */
    private DeviceId extractDeviceIdFromTopic(String topic) {
        Matcher m = DEVICE_MASTERSHIP_TOPIC_PATTERN.matcher(topic);
        if (m.matches()) {
            return DeviceId.deviceId(m.group(2));
        } else {
            throw new IllegalArgumentException("Invalid virtual mastership topic: "
                                                       + topic);
        }
    }

    /**
     * Returns whether the topic is matched with virtual mastership store topic.
     *
     * @param topic topic to match
     * @return True when the topic matched with virtual network mastership store
     */
    private boolean isVirtualMastershipTopic(String topic) {
        Matcher m = DEVICE_MASTERSHIP_TOPIC_PATTERN.matcher(topic);
        return m.matches();
    }

    /**
     * A wrapper class used for the communication service.
     */
    private class VirtualDeviceId {
        NetworkId networkId;
        DeviceId deviceId;

        public VirtualDeviceId(NetworkId networkId, DeviceId deviceId) {
            this.networkId = networkId;
            this.deviceId = deviceId;
        }

        public int hashCode() {
            return Objects.hashCode(networkId, deviceId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof VirtualDeviceId) {
                final VirtualDeviceId that = (VirtualDeviceId) obj;
                return this.getClass() == that.getClass() &&
                        Objects.equal(this.networkId, that.networkId) &&
                        Objects.equal(this.deviceId, that.deviceId);
            }
            return false;
        }
    }
}
