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

import org.onosproject.event.AbstractEvent;

import com.google.common.base.MoreObjects;

/**
 * Describes leadership-related event.
 */
public class LeadershipEvent extends AbstractEvent<LeadershipEvent.Type, Leadership> {

    /**
     * Type of leadership-related events.
     */
    public enum Type {
        /**
         * Signifies that the leader has been elected. The event subject is the
         * new leader.
         */
        LEADER_ELECTED,

        /**
         * Signifies that the leader has been re-elected. The event subject is the
         * leader.
         */
        LEADER_REELECTED,

        /**
         * Signifies that the leader has been booted and lost leadership. The
         * event subject is the former leader.
         */
        LEADER_BOOTED,

        /**
         * Signifies that the list of candidates for leadership for a topic has
         * changed. This event does not guarantee accurate leader information.
         */
        CANDIDATES_CHANGED
    }

    /**
     * Creates an event of a given type and for the specified instance and the
     * current time.
     *
     * @param type       leadership event type
     * @param leadership event subject
     */
    public LeadershipEvent(Type type, Leadership leadership) {
        super(type, leadership);
    }

    /**
     * Creates an event of a given type and for the specified subject and time.
     *
     * @param type     leadership event type
     * @param leadership event subject
     * @param time     occurrence time
     */
    public LeadershipEvent(Type type, Leadership leadership, long time) {
        super(type, leadership, time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), subject(), time());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LeadershipEvent) {
            final LeadershipEvent other = (LeadershipEvent) obj;
            return Objects.equals(this.type(), other.type()) &&
                    Objects.equals(this.subject(), other.subject()) &&
                    Objects.equals(this.time(), other.time());
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
            .add("type", type())
            .add("subject", subject())
            .add("time", time())
            .toString();
    }
}
