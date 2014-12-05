/*
 * Copyright 2014 Open Networking Laboratory
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

import com.google.common.base.MoreObjects;

/**
 * Abstract leadership concept.
 */
public class Leadership {

    private final String topic;
    private final NodeId leader;
    private final long epoch;

    public Leadership(String topic, NodeId leader, long epoch) {
        this.topic = topic;
        this.leader = leader;
        this.epoch = epoch;
    }

    /**
     * The topic for which this leadership applies.
     * @return leadership topic.
     */
    public String topic() {
        return topic;
    }

    /**
     * The nodeId of leader for this topic.
     * @return leader node.
     */
    public NodeId leader() {
        return leader;
    }

    /**
     * The epoch when the leadership was assumed.
     * @return leadership epoch
     */
    public long epoch() {
        return epoch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, leader, epoch);
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
                    Objects.equals(this.epoch, other.epoch);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
            .add("topic", topic)
            .add("leader", leader)
            .add("epoch", epoch)
            .toString();
    }
}
