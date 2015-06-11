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
package org.onosproject.cluster;

import java.util.Objects;
import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

/**
 * Abstract leadership concept. The information carried by this construct
 * include the topic of contention, the {@link NodeId}s of Nodes that could
 * become leader for the topic, the epoch when the term for a given leader
 * began, and the system time when the term began. Note:
 * <ul>
 * <li>The list of NodeIds may include the current leader at index 0, and the
 * rest in decreasing preference order.</li>
 * <li>The epoch is the logical age of a Leadership construct, and should be
 * used for comparing two Leaderships, but only of the same topic.</li>
 * <li>The leader may be null if its accuracy can't be guaranteed. This applies
 * to CANDIDATES_CHANGED events and candidate board contents.</li>
 * </ul>
 */
public class Leadership {

    private final String topic;
    private final Optional<NodeId> leader;
    private final List<NodeId> candidates;
    private final long epoch;
    private final long electedTime;

    public Leadership(String topic, NodeId leader, long epoch, long electedTime) {
        this.topic = topic;
        this.leader = Optional.of(leader);
        this.candidates = ImmutableList.of(leader);
        this.epoch = epoch;
        this.electedTime = electedTime;
    }

    public Leadership(String topic, NodeId leader, List<NodeId> candidates,
            long epoch, long electedTime) {
        this.topic = topic;
        this.leader = Optional.of(leader);
        this.candidates = ImmutableList.copyOf(candidates);
        this.epoch = epoch;
        this.electedTime = electedTime;
    }

    public Leadership(String topic, List<NodeId> candidates,
            long epoch, long electedTime) {
        this.topic = topic;
        this.leader = Optional.empty();
        this.candidates = ImmutableList.copyOf(candidates);
        this.epoch = epoch;
        this.electedTime = electedTime;
    }

    /**
     * The topic for which this leadership applies.
     *
     * @return leadership topic.
     */
    public String topic() {
        return topic;
    }

    /**
     * The nodeId of leader for this topic.
     *
     * @return leader node.
     */
    // This will return Optional<NodeId> in the future.
    public NodeId leader() {
        return leader.orElse(null);
    }

    /**
     * Returns an preference-ordered list of nodes that are in the leadership
     * race for this topic.
     *
     * @return a list of NodeIds in priority-order, or an empty list.
     */
    public List<NodeId> candidates() {
        return candidates;
    }

    /**
     * The epoch when the leadership was assumed.
     * <p>
     * Comparing epochs is only appropriate for leadership events for the same
     * topic. The system guarantees that for any given topic the epoch for a new
     * term is higher (not necessarily by 1) than the epoch for any previous
     * term.
     *
     * @return leadership epoch
     */
    public long epoch() {
        return epoch;
    }

    /**
     * The system time when the term started.
     * <p>
     * The elected time is initially set on the node coordinating
     * the leader election using its local system time. Due to possible
     * clock skew, relying on this value for determining event ordering
     * is discouraged. Epoch is more appropriate for determining
     * event ordering.
     *
     * @return elected time.
     */
    public long electedTime() {
        return electedTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, leader, candidates, epoch, electedTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Leadership) {
            final Leadership other = (Leadership) obj;
            return Objects.equals(this.topic, other.topic) &&
                    Objects.equals(this.leader, other.leader) &&
                    Objects.equals(this.candidates, other.candidates) &&
                    Objects.equals(this.epoch, other.epoch) &&
                    Objects.equals(this.electedTime, other.electedTime);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
            .add("topic", topic)
            .add("leader", leader)
            .add("candidates", candidates)
            .add("epoch", epoch)
            .add("electedTime", new DateTime(electedTime))
            .toString();
    }
}
