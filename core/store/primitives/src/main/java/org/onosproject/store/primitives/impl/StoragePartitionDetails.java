/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.store.primitives.impl;

import io.atomix.copycat.server.cluster.Member;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.onosproject.cluster.PartitionId;
import org.onosproject.store.service.PartitionInfo;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

/**
 * Operational details for a {@code StoragePartition}.
 */
public class StoragePartitionDetails {

    private final PartitionId partitionId;
    private final Set<Member> activeMembers;
    private final Set<Member> configuredMembers;
    private final Member leader;
    private final long leaderTerm;

    public StoragePartitionDetails(PartitionId partitionId,
            Collection<Member> activeMembers,
            Collection<Member> configuredMembers,
            Member leader,
            long leaderTerm) {
        this.partitionId = partitionId;
        this.activeMembers = ImmutableSet.copyOf(activeMembers);
        this.configuredMembers = ImmutableSet.copyOf(configuredMembers);
        this.leader = leader;
        this.leaderTerm = leaderTerm;
    }

    /**
     * Returns the set of active members.
     * @return active members
     */
    public Set<Member> activeMembers() {
        return activeMembers;
    }

    /**
     * Returns the set of configured members.
     * @return configured members
     */
    public Set<Member> configuredMembers() {
        return configuredMembers;
    }

    /**
     * Returns the partition leader.
     * @return leader
     */
    public Member leader() {
        return leader;
    }

    /**
     * Returns the partition leader term.
     * @return leader term
     */
    public long leaderTerm() {
        return leaderTerm;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("activeMembers", activeMembers)
                .add("configuredMembers", configuredMembers)
                .add("leader", leader)
                .add("leaderTerm", leaderTerm)
                .toString();
    }

    /**
     * Returns the details as an instance of {@code PartitionInfo}.
     * @return partition info
     */
    public PartitionInfo toPartitionInfo() {
        Function<Member, String> memberToString =
                m -> m == null ? "none" : String.format("%s:%d", m.address().host(), m.address().port());
        return new PartitionInfo(partitionId.toString(),
                leaderTerm,
                activeMembers.stream().map(memberToString).collect(Collectors.toList()),
                memberToString.apply(leader));
    }
}
