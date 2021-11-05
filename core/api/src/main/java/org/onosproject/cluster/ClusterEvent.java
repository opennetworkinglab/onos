/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.base.MoreObjects;
import org.onosproject.event.AbstractEvent;

import java.util.Objects;

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

    public enum InstanceType {
        /**
         * Signifies that the event refers to an ONOS instance.
         */
        ONOS,

        /**
         * Signifies that the event refers to an Atomix instance.
         */
        STORAGE,

        /**
         * Signifies that the event refers to an Unknown instance.
         */
        UNKNOWN
    }

    private final InstanceType instanceType;

    /**
     * Creates an event of a given type and for the specified instance and the
     * current time.
     *
     * @param type     cluster event type
     * @param instance cluster device subject
     */
    public ClusterEvent(Type type, ControllerNode instance) {
        this(type, instance, InstanceType.UNKNOWN);
    }

    /**
     * Creates an event of a given type and for the specified device and time.
     *
     * @param type     device event type
     * @param instance event device subject
     * @param time     occurrence time
     */
    public ClusterEvent(Type type, ControllerNode instance, long time) {
        this(type, instance, time, InstanceType.UNKNOWN);
    }

    /**
     * Creates an event of a given type and for the specified instance and the
     * current time.
     *
     * @param type     cluster event type
     * @param instance cluster device subject
     * @param instanceType instance type
     */
    public ClusterEvent(Type type, ControllerNode instance, InstanceType instanceType) {
        super(type, instance);
        this.instanceType = instanceType;
    }

    /**
     * Creates an event of a given type and for the specified device and time.
     *
     * @param type     device event type
     * @param instance event device subject
     * @param time     occurrence time
     * @param instanceType instance type
     */
    public ClusterEvent(Type type, ControllerNode instance, long time, InstanceType instanceType) {
        super(type, instance, time);
        this.instanceType = instanceType;
    }

    /**
     * Returns the instance type subject.
     *
     * @return instance type subject or UNKNOWN if the event is not instance type specific.
     */
    public InstanceType instanceType() {
        return instanceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), subject(), time(), instanceType());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ClusterEvent) {
            final ClusterEvent other = (ClusterEvent) obj;
            return Objects.equals(this.type(), other.type()) &&
                    Objects.equals(this.subject(), other.subject()) &&
                    Objects.equals(this.time(), other.time()) &&
                    Objects.equals(this.instanceType(), other.instanceType());
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
                .add("type", type())
                .add("subject", subject())
                .add("time", time())
                .add("instanceType", instanceType())
                .toString();
    }

}
