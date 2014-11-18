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
package org.onlab.onos.cluster;

import org.onlab.onos.event.AbstractEvent;

/**
 * Describes leadership-related event.
 */
public class LeadershipEvent extends AbstractEvent<LeadershipEvent.Type, ControllerNode> {

    /**
     * Type of leadership-related events.
     */
    public enum Type {
        /**
         * Signifies that the leader has changed. The event subject is the
         * new leader.
         */
        LEADER_CHANGED
    }

    /**
     * Creates an event of a given type and for the specified instance and the
     * current time.
     *
     * @param type     leadership event type
     * @param instance cluster device subject
     */
    public LeadershipEvent(Type type, ControllerNode instance) {
        super(type, instance);
    }

    /**
     * Creates an event of a given type and for the specified device and time.
     *
     * @param type     device event type
     * @param instance event device subject
     * @param time     occurrence time
     */
    public LeadershipEvent(Type type, ControllerNode instance, long time) {
        super(type, instance, time);
    }

}
