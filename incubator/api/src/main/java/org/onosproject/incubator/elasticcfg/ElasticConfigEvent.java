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
package org.onosproject.incubator.elasticcfg;

import org.onosproject.event.AbstractEvent;

/**
 * Describes a ProprietaryConfig event.
 */
public class ElasticConfigEvent extends AbstractEvent<ElasticConfigEvent.Type, ConfigNodePath> {

    private final ConfigNode value;

    /**
     * Type of configuration events.
     */
    public enum Type {
        /**
         * Signifies that a prop configuration instance was added.
         */
        NODE_ADDED,

        /**
         * Signifies that prop configuration instance was updated.
         */
        NODE_UPDATED,

        /**
         * Signifies that prop configuration instance was removed.
         */
        NODE_REMOVED,
        /**
         * Signifies that a prop configuration subtree was added.
         */
        SUBTREE_ADDED,

        /**
         * Signifies that prop configuration subtree was updated.
         */
        SUBTREE_UPDATED,

        /**
         * Signifies that prop configuration subtree was removed.
         */
        SUBTREE_REMOVED
    }

    /**
     * Creates an event of a given type, config node value and config node path.
     *
     * @param type  config node type
     * @param path  config node path
     * @param value config node value
     */
    public ElasticConfigEvent(Type type, ConfigNodePath path, ConfigNode value) {
        super(type, path);
        this.value = value;
    }

    /**
     * Returns the config node value.
     *
     * @return ConfigNode value
     */
    public ConfigNode value() {
        return value;
    }
}