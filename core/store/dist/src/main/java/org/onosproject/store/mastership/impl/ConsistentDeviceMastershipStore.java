/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.store.mastership.impl;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.mastership.MastershipEvent.Type.BACKUPS_CHANGED;
import static org.onosproject.mastership.MastershipEvent.Type.MASTER_CHANGED;
import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipAdminService;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipStore;
import org.onosproject.mastership.MastershipStoreDelegate;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.StoreSerializer;
import org.slf4j.Logger;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Implementation of the MastershipStore on top of Leadership Service.
 */
@Component(immediate = true, enabled = true)
@Service
public class ConsistentDeviceMastershipStore
    extends AbstractStore<MastershipEvent, MastershipStoreDelegate>
    implements MastershipStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipAdminService leadershipAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    private NodeId localNodeId;

    private static final MessageSubject ROLE_RELINQUISH_SUBJECT =
            new MessageSubject("mastership-store-device-role-relinquish");

    private static final Pattern DEVICE_MASTERSHIP_TOPIC_PATTERN =
            Pattern.compile("device:(.*)");

    private ExecutorService eventHandler;
    private ExecutorService messageHandlingExecutor;
    private ScheduledExecutorService transferExecutor;
    private final LeadershipEventListener leadershipEventListener =
            new InternalDeviceMastershipEventListener();

    private static final String NODE_ID_NULL = "Node ID cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final int WAIT_BEFORE_MASTERSHIP_HANDOFF_MILLIS = 3000;

    public static final StoreSerializer SERIALIZER = StoreSerializer.using(
            KryoNamespace.newBuilder()
                    .register(KryoNamespaces.API)
                    .register(MastershipRole.class)
                    .register(MastershipEvent.class)
                    .register(MastershipEvent.Type.class)
                    .build("MastershipStore"));

    @Activate
    public void activate() {

        eventHandler = Executors.newSingleThreadExecutor(
                        groupedThreads("onos/store/device/mastership", "event-handler", log));

        messageHandlingExecutor =
                Executors.newSingleThreadExecutor(
                        groupedThreads("onos/store/device/mastership", "message-handler", log));
        transferExecutor =
                Executors.newSingleThreadScheduledExecutor(
                        groupedThreads("onos/store/device/mastership", "mastership-transfer-executor", log));
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
    public CompletableFuture<MastershipRole> requestRole(DeviceId deviceId) {
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        String leadershipTopic = createDeviceMastershipTopic(deviceId);
        Leadership leadership = leadershipService.runForLeadership(leadershipTopic);
        return CompletableFuture.completedFuture(localNodeId.equals(leadership.leaderNodeId())
                ? MastershipRole.MASTER : MastershipRole.STANDBY);
    }

    @Override
    public MastershipRole getRole(NodeId nodeId, DeviceId deviceId) {
        checkArgument(nodeId != null, NODE_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        String leadershipTopic = createDeviceMastershipTopic(deviceId);
        Leadership leadership = leadershipService.getLeadership(leadershipTopic);
        NodeId leader = leadership == null ? null : leadership.leaderNodeId();
        List<NodeId> candidates = leadership == null ?
                ImmutableList.of() : ImmutableList.copyOf(leadership.candidates());
        return Objects.equal(nodeId, leader) ?
                MastershipRole.MASTER : candidates.contains(nodeId) ? MastershipRole.STANDBY : MastershipRole.NONE;
    }

    @Override
    public NodeId getMaster(DeviceId deviceId) {
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        return leadershipService.getLeader(createDeviceMastershipTopic(deviceId));
    }

    @Override
    public RoleInfo getNodes(DeviceId deviceId) {
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        Map<NodeId, MastershipRole> roles = Maps.newHashMap();
        clusterService.getNodes()
                      .forEach((node) -> roles.put(node.id(), getRole(node.id(), deviceId)));

        NodeId master = null;
        final List<NodeId> standbys = Lists.newLinkedList();

        List<NodeId> candidates = leadershipService.getCandidates(createDeviceMastershipTopic(deviceId));

        for (Map.Entry<NodeId, MastershipRole> entry : roles.entrySet()) {
            if (entry.getValue() == MastershipRole.MASTER) {
                master = entry.getKey();
            } else if (entry.getValue() == MastershipRole.STANDBY) {
                standbys.add(entry.getKey());
            }
        }

        List<NodeId> sortedStandbyList = candidates.stream().filter(standbys::contains).collect(Collectors.toList());

        return new RoleInfo(master, sortedStandbyList);
    }

    @Override
    public Set<DeviceId> getDevices(NodeId nodeId) {
        checkArgument(nodeId != null, NODE_ID_NULL);

        return leadershipService
                .ownedTopics(nodeId)
                .stream()
                .filter(this::isDeviceMastershipTopic)
                .map(this::extractDeviceIdFromTopic)
                .collect(Collectors.toSet());
    }

    @Override
    public CompletableFuture<MastershipEvent> setMaster(NodeId nodeId, DeviceId deviceId) {
        checkArgument(nodeId != null, NODE_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        String leadershipTopic = createDeviceMastershipTopic(deviceId);
        if (leadershipAdminService.promoteToTopOfCandidateList(leadershipTopic, nodeId)) {
            transferExecutor.schedule(() -> leadershipAdminService.transferLeadership(leadershipTopic, nodeId),
                    WAIT_BEFORE_MASTERSHIP_HANDOFF_MILLIS, TimeUnit.MILLISECONDS);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public MastershipTerm getTermFor(DeviceId deviceId) {
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        String leadershipTopic = createDeviceMastershipTopic(deviceId);
        Leadership leadership = leadershipService.getLeadership(leadershipTopic);
        return leadership != null && leadership.leaderNodeId() != null ?
            MastershipTerm.of(leadership.leaderNodeId(), leadership.leader().term()) : null;
    }

    @Override
    public CompletableFuture<MastershipEvent> setStandby(NodeId nodeId, DeviceId deviceId) {
        checkArgument(nodeId != null, NODE_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        NodeId currentMaster = getMaster(deviceId);
        if (!nodeId.equals(currentMaster)) {
            return CompletableFuture.completedFuture(null);
        }

        String leadershipTopic = createDeviceMastershipTopic(deviceId);
        List<NodeId> candidates = leadershipService.getCandidates(leadershipTopic);

        NodeId newMaster = candidates.stream()
                                     .filter(candidate -> !Objects.equal(nodeId, candidate))
                                     .findFirst()
                                     .orElse(null);
        log.info("Transitioning to role {} for {}. Next master: {}",
                newMaster != null ? MastershipRole.STANDBY : MastershipRole.NONE, deviceId, newMaster);

        if (newMaster != null) {
            return setMaster(newMaster, deviceId);
        }
        return relinquishRole(nodeId, deviceId);
    }

    @Override
    public CompletableFuture<MastershipEvent> relinquishRole(NodeId nodeId, DeviceId deviceId) {
        checkArgument(nodeId != null, NODE_ID_NULL);
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        if (nodeId.equals(localNodeId)) {
            return relinquishLocalRole(deviceId);
        }

        log.debug("Forwarding request to relinquish "
                + "role for device {} to {}", deviceId, nodeId);
        return clusterCommunicator.sendAndReceive(
                deviceId,
                ROLE_RELINQUISH_SUBJECT,
                SERIALIZER::encode,
                SERIALIZER::decode,
                nodeId);
    }

    private CompletableFuture<MastershipEvent> relinquishLocalRole(DeviceId deviceId) {
        checkArgument(deviceId != null, DEVICE_ID_NULL);

        String leadershipTopic = createDeviceMastershipTopic(deviceId);
        if (!leadershipService.getCandidates(leadershipTopic).contains(localNodeId)) {
            return CompletableFuture.completedFuture(null);
        }
        MastershipEvent.Type eventType = localNodeId.equals(leadershipService.getLeader(leadershipTopic)) ?
                MastershipEvent.Type.MASTER_CHANGED : MastershipEvent.Type.BACKUPS_CHANGED;
        leadershipService.withdraw(leadershipTopic);
        return CompletableFuture.completedFuture(new MastershipEvent(eventType, deviceId, getNodes(deviceId)));
    }

    @Override
    public void relinquishAllRole(NodeId nodeId) {
        // Noop. LeadershipService already takes care of detecting and purging stale locks.
    }

    private class InternalDeviceMastershipEventListener implements LeadershipEventListener {

        @Override
        public boolean isRelevant(LeadershipEvent event) {
            Leadership leadership = event.subject();
            return isDeviceMastershipTopic(leadership.topic());
        }

        @Override
        public void event(LeadershipEvent event) {
            eventHandler.execute(() -> handleEvent(event));
        }

        private void handleEvent(LeadershipEvent event) {
            Leadership leadership = event.subject();
            DeviceId deviceId = extractDeviceIdFromTopic(leadership.topic());
            RoleInfo roleInfo = getNodes(deviceId);
            switch (event.type()) {
            case LEADER_AND_CANDIDATES_CHANGED:
                notifyDelegate(new MastershipEvent(BACKUPS_CHANGED, deviceId, roleInfo));
                notifyDelegate(new MastershipEvent(MASTER_CHANGED, deviceId, roleInfo));
                break;
            case LEADER_CHANGED:
                notifyDelegate(new MastershipEvent(MASTER_CHANGED, deviceId, roleInfo));
                break;
            case CANDIDATES_CHANGED:
                notifyDelegate(new MastershipEvent(BACKUPS_CHANGED, deviceId, roleInfo));
                break;
            default:
                return;
            }
        }
    }

    private String createDeviceMastershipTopic(DeviceId deviceId) {
        return String.format("device:%s", deviceId.toString());
    }

    private DeviceId extractDeviceIdFromTopic(String topic) {
        Matcher m = DEVICE_MASTERSHIP_TOPIC_PATTERN.matcher(topic);
        if (m.matches()) {
            return DeviceId.deviceId(m.group(1));
        } else {
            throw new IllegalArgumentException("Invalid device mastership topic: " + topic);
        }
    }

    private boolean isDeviceMastershipTopic(String topic) {
        Matcher m = DEVICE_MASTERSHIP_TOPIC_PATTERN.matcher(topic);
        return m.matches();
    }
}
