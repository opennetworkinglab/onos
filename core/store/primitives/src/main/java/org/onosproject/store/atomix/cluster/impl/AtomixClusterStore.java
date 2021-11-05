/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.atomix.cluster.impl;

import com.google.common.collect.Maps;
import io.atomix.cluster.ClusterMembershipEvent;
import io.atomix.cluster.ClusterMembershipEventListener;
import io.atomix.cluster.ClusterMembershipService;
import io.atomix.cluster.Member;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterStore;
import org.onosproject.cluster.ClusterStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.Node;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.Version;
import org.onosproject.core.VersionService;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.atomix.impl.AtomixManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Atomix cluster store.
 */
@Component(immediate = true, service = ClusterStore.class)
public class AtomixClusterStore extends AbstractStore<ClusterEvent, ClusterStoreDelegate> implements ClusterStore {
    private static final String INSTANCE_ID_NULL = "Instance ID cannot be null";

    private static final String STATE_KEY = "state";
    private static final String VERSION_KEY = "version";
    private static final String TYPE_KEY = "type";
    private static final String TYPE_ONOS = "onos";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected AtomixManager atomixManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VersionService versionService;

    private ClusterMembershipService membershipService;
    private ControllerNode localNode;
    private final Map<NodeId, ControllerNode> nodes = Maps.newConcurrentMap();
    private final Map<NodeId, ControllerNode.State> states = Maps.newConcurrentMap();
    private final Map<NodeId, Version> versions = Maps.newConcurrentMap();
    private final Map<NodeId, Instant> updates = Maps.newConcurrentMap();
    private final ClusterMembershipEventListener membershipEventListener = this::changeMembership;

    @Activate
    public void activate() {
        membershipService = atomixManager.getAtomix().getMembershipService();
        membershipService.addListener(membershipEventListener);
        membershipService.getMembers().forEach(member -> {
            ControllerNode node = toControllerNode(member);
            nodes.put(node.id(), node);
            updateState(node, member);
            updateVersion(node, member);
        });
        membershipService.getLocalMember().properties().put(STATE_KEY, ControllerNode.State.ACTIVE.name());
        membershipService.getLocalMember().properties().put(VERSION_KEY, versionService.version().toString());
        localNode = toControllerNode(membershipService.getLocalMember());
        states.put(localNode.id(), ControllerNode.State.ACTIVE);
        versions.put(localNode.id(), versionService.version());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        membershipService.removeListener(membershipEventListener);
        log.info("Stopped");
    }

    private void changeMembership(ClusterMembershipEvent event) {
        ControllerNode node = nodes.get(NodeId.nodeId(event.subject().id().id()));
        log.debug("Received a membership event {}", event);
        switch (event.type()) {
            case MEMBER_ADDED:
            case METADATA_CHANGED:
                if (node == null) {
                    node = toControllerNode(event.subject());
                    nodes.put(node.id(), node);
                    notifyDelegate(clusterEvent(ClusterEvent.Type.INSTANCE_ADDED, event.subject(), node));
                }
                updateVersion(node, event.subject());
                updateState(node, event.subject());
                break;
            case MEMBER_REMOVED:
                if (node != null
                    && states.put(node.id(), ControllerNode.State.INACTIVE) != ControllerNode.State.INACTIVE) {
                    notifyDelegate(clusterEvent(ClusterEvent.Type.INSTANCE_DEACTIVATED, event.subject(), node));
                    notifyDelegate(clusterEvent(ClusterEvent.Type.INSTANCE_REMOVED, event.subject(), node));
                }
                break;
            default:
                break;
        }
    }

    private void updateState(ControllerNode node, Member member) {
        String state = member.properties().getProperty(STATE_KEY);
        if (state == null || !state.equals(ControllerNode.State.READY.name())) {
            if (states.put(node.id(), ControllerNode.State.ACTIVE) != ControllerNode.State.ACTIVE) {
                log.info("Updated node {} state to {}", node.id(), ControllerNode.State.ACTIVE);
                markUpdated(node.id());
                notifyDelegate(clusterEvent(ClusterEvent.Type.INSTANCE_ACTIVATED, member, node));
            }
        } else {
            if (states.put(node.id(), ControllerNode.State.READY) != ControllerNode.State.READY) {
                log.info("Updated node {} state to {}", node.id(), ControllerNode.State.READY);
                markUpdated(node.id());
                notifyDelegate(clusterEvent(ClusterEvent.Type.INSTANCE_READY, member, node));
            }
        }
    }

    private void updateVersion(ControllerNode node, Member member) {
        String versionString = member.properties().getProperty(VERSION_KEY);
        if (versionString != null) {
            Version version = Version.version(versionString);
            if (!Objects.equals(versions.put(node.id(), version), version)) {
                log.info("Updated node {} version to {}", node.id(), version);
            }
        }
    }

    private void markUpdated(NodeId nodeId) {
        updates.put(nodeId, Instant.now());
    }

    private ControllerNode toControllerNode(Member member) {
        return new DefaultControllerNode(
            NodeId.nodeId(member.id().id()),
            member.address().host(),
            member.address().port());
    }

    @Override
    public ControllerNode getLocalNode() {
        return localNode;
    }

    @Override
    public Set<Node> getStorageNodes() {
        return membershipService.getMembers()
            .stream()
            .filter(member -> !Objects.equals(member.properties().getProperty(TYPE_KEY), TYPE_ONOS))
            .map(this::toControllerNode)
            .collect(Collectors.toSet());
    }

    @Override
    public Set<ControllerNode> getNodes() {
        return membershipService.getMembers()
            .stream()
            .filter(member -> Objects.equals(member.properties().getProperty(TYPE_KEY), TYPE_ONOS))
            .map(this::toControllerNode)
            .collect(Collectors.toSet());
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        Member member = membershipService.getMember(nodeId.id());
        return member != null ? toControllerNode(member) : null;
    }

    @Override
    public ControllerNode.State getState(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return states.get(nodeId);
    }

    @Override
    public Version getVersion(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return versions.get(nodeId);
    }

    @Override
    public Instant getLastUpdatedInstant(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return updates.get(nodeId);
    }

    @Override
    public void markFullyStarted(boolean started) {
        ControllerNode.State state = started ? ControllerNode.State.READY : ControllerNode.State.ACTIVE;
        membershipService.getLocalMember().properties().setProperty(STATE_KEY, state.name());
    }

    @Override
    public ControllerNode addNode(NodeId nodeId, IpAddress ip, int tcpPort) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        ControllerNode node = new DefaultControllerNode(nodeId, ip, tcpPort);
        nodes.put(node.id(), node);
        ControllerNode.State state = node.equals(localNode)
            ? ControllerNode.State.ACTIVE : ControllerNode.State.INACTIVE;
        Member member = membershipService.getMember(node.id().id());
        member.properties().setProperty(STATE_KEY, state.name());
        notifyDelegate(clusterEvent(ClusterEvent.Type.INSTANCE_ADDED, member, node));
        return node;
    }

    @Override
    public void removeNode(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        ControllerNode node = nodes.remove(nodeId);
        if (node != null) {
            states.remove(nodeId);
            notifyDelegate(clusterEvent(ClusterEvent.Type.INSTANCE_REMOVED,
                    membershipService.getMember(node.id().id()), node));
        }
    }

    private ClusterEvent clusterEvent(ClusterEvent.Type type, Member member, ControllerNode node) {
        // Atomix nodes do not set the property TYPE. Nowadays, the internal else is not used.
        if (member != null && !isNullOrEmpty(member.properties().getProperty(TYPE_KEY))) {
            if (Objects.equals(member.properties().getProperty(TYPE_KEY), TYPE_ONOS)) {
                return new ClusterEvent(type, node, ClusterEvent.InstanceType.ONOS);
            } else {
                return new ClusterEvent(type, node, ClusterEvent.InstanceType.STORAGE);
            }
        }
        return new ClusterEvent(type, node, ClusterEvent.InstanceType.STORAGE);
    }
}
