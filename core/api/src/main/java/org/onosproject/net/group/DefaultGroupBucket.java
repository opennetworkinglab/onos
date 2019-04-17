/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.group;

import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Group bucket implementation. A group bucket is collection of
 * instructions that can be performed on a traffic flow. A select
 * Group can have one or more Buckets where traffic will be
 * processed by a single bucket in the group, based on device
 * specific selection algorithm (e.g. hash on some fields of the
 * incoming traffic flows or round robin) and hence can contains
 * optional weight field to define the weights among the buckets
 * in the group. A failover group bucket is associated with a
 * specific port or group that controls its liveness.
 */
public final class DefaultGroupBucket implements GroupBucket, StoredGroupBucketEntry {
    private final GroupDescription.Type type;
    private final TrafficTreatment treatment;
    private final short weight;
    private final PortNumber watchPort;
    private final GroupId watchGroup;
    private long packets;
    private long bytes;

    /**
     * Group bucket constructor with the parameters.
     *
     * @param type group bucket type
     * @param treatment traffic treatment associated with group bucket
     * @param weight optional weight associated with group bucket
     * @param watchPort port that determines the liveness of group bucket
     * @param watchGroup group that determines the liveness of group bucket
     */
    private DefaultGroupBucket(GroupDescription.Type type,
                               TrafficTreatment treatment,
                               short weight,
                               PortNumber watchPort,
                               GroupId watchGroup) {
        this.type = type;
        this.treatment = checkNotNull(treatment);
        this.weight = weight;
        this.watchPort = watchPort;
        this.watchGroup = watchGroup;
    }

    /**
     * Creates indirect group bucket.
     *
     * @param treatment traffic treatment associated with group bucket
     * @return indirect group bucket object
     */
    public static GroupBucket createIndirectGroupBucket(
                                TrafficTreatment treatment) {
        return new DefaultGroupBucket(GroupDescription.Type.INDIRECT,
                                      treatment,
                                      (short) -1,
                                      null,
                                      null);
    }

    /**
     * Creates select group bucket with weight as 1.
     *
     * @param treatment traffic treatment associated with group bucket
     * @return select group bucket object
     */
    public static GroupBucket createSelectGroupBucket(
                                 TrafficTreatment treatment) {
        return new DefaultGroupBucket(GroupDescription.Type.SELECT,
                                      treatment,
                                      (short) 1,
                                      null,
                                      null);
    }

    /**
     * Creates select group bucket with specified weight.
     *
     * @param treatment traffic treatment associated with group bucket
     * @param weight weight associated with group bucket
     * @return select group bucket object
     */
    public static GroupBucket createSelectGroupBucket(
                                 TrafficTreatment treatment,
                                 short weight) {
        if (weight == 0) {
            return null;
        }

        return new DefaultGroupBucket(GroupDescription.Type.SELECT,
                                      treatment,
                                      weight,
                                      null,
                                      null);
    }

    /**
     * Creates failover group bucket with watchport or watchgroup.
     *
     * @param treatment traffic treatment associated with group bucket
     * @param watchPort port that determines the liveness of group bucket
     * @param watchGroup group that determines the liveness of group bucket
     * @return failover group bucket object
     */
    public static GroupBucket createFailoverGroupBucket(
                                 TrafficTreatment treatment,
                                 PortNumber watchPort,
                                 GroupId watchGroup) {
        checkArgument(((watchPort != null) || (watchGroup != null)));
        return new DefaultGroupBucket(GroupDescription.Type.FAILOVER,
                                      treatment,
                                      (short) -1,
                                      watchPort,
                                      watchGroup);
    }

    /**
     * Creates all group bucket.
     *
     * @param treatment traffic treatment associated with group bucket
     * @return all group bucket object
     */
    public static GroupBucket createAllGroupBucket(TrafficTreatment treatment) {
        return new DefaultGroupBucket(GroupDescription.Type.ALL,
                                      treatment,
                                      (short) -1,
                                      null,
                                      null);
    }

    /**
     * Creates clone group bucket.
     *
     * @param treatment traffic treatment associated with group bucket
     * @return clone group bucket object
     */
    public static GroupBucket createCloneGroupBucket(TrafficTreatment treatment) {
        return new DefaultGroupBucket(GroupDescription.Type.CLONE,
                                      treatment,
                                      (short) -1,
                                      null,
                                      null);
    }

    @Override
    public GroupDescription.Type type() {
        return this.type;
    }

    /**
     * Returns list of Traffic instructions that are part of the bucket.
     *
     * @return TrafficTreatment Traffic instruction list
     */
    @Override
    public TrafficTreatment treatment() {
        return treatment;
    }

    /**
     * Returns weight of select group bucket.
     *
     * @return short weight associated with a bucket
     */
    @Override
    public short weight() {
        return weight;
    }

    /**
     * Returns port number used for liveness detection for a
     * failover bucket.
     *
     * @return PortNumber port number used for liveness detection
     */
    @Override
    public PortNumber watchPort() {
        return watchPort;
    }

    /**
     * Returns group identifier used for liveness detection for a
     * failover bucket.
     *
     * @return GroupId group identifier to be used for liveness detection
     */
    @Override
    public GroupId watchGroup() {
        return watchGroup;
    }

    /*
     * The type and treatment can change on a given bucket
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, treatment);
    }

    /*
     * The priority and statistics can change on a given treatment and selector
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultGroupBucket) {
            DefaultGroupBucket that = (DefaultGroupBucket) obj;
            List<Instruction> myInstructions = this.treatment.allInstructions();
            List<Instruction> theirInstructions = that.treatment.allInstructions();

            return Objects.equals(type, that.type) &&
                   myInstructions.containsAll(theirInstructions) &&
                   theirInstructions.containsAll(myInstructions);
        }
        return false;
    }

    @Override
    public boolean hasSameParameters(GroupBucket other) {
        return weight == other.weight() &&
               Objects.equals(watchPort, other.watchPort()) &&
               Objects.equals(watchGroup, other.watchGroup());
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", type)
                .add("treatment", treatment)
                .add("packets", packets)
                .add("bytes", bytes)
                .toString();
    }

    @Override
    public long packets() {
        return packets;
    }

    @Override
    public long bytes() {
        return bytes;
    }

    @Override
    public void setPackets(long packets) {
        this.packets = packets;
    }

    @Override
    public void setBytes(long bytes) {
        this.bytes = bytes;
    }
}
