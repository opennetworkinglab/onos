/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onosproject.event.AbstractEvent;

/**
 * Describes cluster-related event.
 */
public class ClusterEvent extends AbstractEvent<ClusterEvent.Type, ControllerNode> {

    /**
     * Type of cluster-related events.
     */
    public enum Type {
        /**
         * Signifies that a new cluster instance has been administratively added.
         */
        INSTANCE_ADDED,

        /**
         * Signifies that a cluster instance has been administratively removed.
         */
        INSTANCE_REMOVED,

        /**
         * Signifies that a cluster instance became active.
         */
        INSTANCE_ACTIVATED,

        /**
         * Signifies that a cluster instance became ready.
         */
        INSTANCE_READY,

        /**
         * Signifies that a cluster instance became inactive.
         */
        INSTANCE_DEACTIVATED
    }

    /**
     * Creates an event of a given type and for the specified instance and the
     * current time.
     *
     * @param type     cluster event type
     * @param instance cluster device subject
     */
    public ClusterEvent(Type type, ControllerNode instance) {
        super(type, instance);
    }

    /**
     * Creates an event of a given type and for the specified device and time.
     *
     * @param type     device event type
     * @param instance event device subject
     * @param time     occurrence time
     */
    public ClusterEvent(Type type, ControllerNode instance, long time) {
        super(type, instance, time);
    }

}
