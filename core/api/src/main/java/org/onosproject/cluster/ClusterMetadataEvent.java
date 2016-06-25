/*
 * Copyright 2015-present Open Networking Laboratory
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
}
