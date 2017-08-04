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
package org.onosproject.cluster;

import com.google.common.base.MoreObjects;
import org.onosproject.event.AbstractEvent;

import java.util.Objects;

/**
 * Describes a cluster metadata event.
 */
public class ClusterMetadataEvent extends AbstractEvent<ClusterMetadataEvent.Type, ClusterMetadata> {

    /**
     * Type of cluster metadata events.
     */
    public enum Type {
        /**
         * Signifies that the cluster metadata has changed.
         */
        METADATA_CHANGED,
    }

    /**
     * Creates an event of a given type and for the specified metadata and the
     * current time.
     *
     * @param type     cluster metadata event type
     * @param metadata cluster metadata subject
     */
    public ClusterMetadataEvent(Type type, ClusterMetadata metadata) {
        super(type, metadata);
    }

    /**
     * Creates an event of a given type and for the specified metadata and time.
     *
     * @param type     cluster metadata event type
     * @param metadata cluster metadata subject
     * @param time     occurrence time
     */
    public ClusterMetadataEvent(Type type, ClusterMetadata metadata, long time) {
        super(type, metadata, time);
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
        if (obj instanceof ClusterMetadataEvent) {
            final ClusterMetadataEvent other = (ClusterMetadataEvent) obj;
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
