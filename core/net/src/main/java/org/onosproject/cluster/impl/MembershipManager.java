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
package org.onosproject.cluster.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.Member;
import org.onosproject.cluster.MembershipGroup;
import org.onosproject.cluster.MembershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cluster membership manager.
 */
@Component(immediate = true, service = MembershipService.class)
public class MembershipManager implements MembershipService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    private Member localMember;

    @Activate
    public void activate() {
        NodeId localId = clusterService.getLocalNode().id();
        localMember = new Member(
                localId,
                clusterService.getVersion(localId));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    private Member toMemberId(ControllerNode node) {
        return new Member(node.id(), clusterService.getVersion(node.id()));
    }

    @Override
    public Member getLocalMember() {
        return localMember;
    }

    @Override
    public MembershipGroup getLocalGroup() {
        return getGroup(getLocalMember().version());
    }

    @Override
    public Set<Member> getMembers() {
        return clusterService.getNodes().stream()
                .filter(node -> Optional.ofNullable(clusterService.getVersion(node.id()))
                        .filter(version -> version.equals(localMember.version())).isPresent())
                .map(this::toMemberId)
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<MembershipGroup> getGroups() {
        Map<Version, Set<Member>> groups = Maps.newHashMap();
        clusterService.getNodes().stream()
                .map(this::toMemberId)
                .forEach(member ->
                        groups.computeIfAbsent(member.version(), k -> Sets.newHashSet()).add(member));
        return Maps.transformEntries(groups, MembershipGroup::new).values();
    }

    @Override
    public MembershipGroup getGroup(Version version) {
        return new MembershipGroup(version, getMembers(version));
    }

    @Override
    public Set<Member> getMembers(Version version) {
        return getMembers()
                .stream()
                .filter(m -> Objects.equals(m.version(), version))
                .collect(Collectors.toSet());
    }

    @Override
    public Member getMember(NodeId nodeId) {
        ControllerNode node = clusterService.getNode(nodeId);
        return node != null ? toMemberId(node) : null;
    }
}
