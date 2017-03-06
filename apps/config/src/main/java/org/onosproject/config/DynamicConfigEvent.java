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
package org.onosproject.config;


import com.google.common.annotations.Beta;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.event.AbstractEvent;

/**
 * Describes a DynamicConfig change event.
 */
@Beta
public class DynamicConfigEvent extends AbstractEvent<DynamicConfigEvent.Type, ResourceId> {

    /**
     * Type of configuration events.
     * A configuration instance could be a leaf node or a subtree,
     * identified by the subject, ResourceId.
     */
    public enum Type {
        /**
         * Signifies that a dynamic configuration instance was added.
         */
        NODE_ADDED,

        /**
         * Signifies that dynamic configuration instance was updated.
         */
        NODE_UPDATED,

        /**
         * Signifies that dynamic configuration instance was replaced.
         */
        NODE_REPLACED,

        /**
         * Signifies that dynamic configuration instance was removed.
         */
        NODE_DELETED,

        /**
         * Signifies an unknown and hence invalid store opeartion.
         */
        UNKNOWN_OPRN
    }

    /**
     * Creates an event of a given type, config node value and config node path.
     *
     * @param type  config node type
     * @param path  config node path
     */
    public DynamicConfigEvent(Type type, ResourceId path) {
        super(type, path);
    }
}