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

import java.util.List;

/**
 * Abstraction of an instance in the elastic  config store.
 */
public interface ConfigNode<V> {
    /**
     * Builder for ConfigNode.
     */
    interface Builder<V> {
        /**
         * Adds the type of the instance node.
         *
         * @param type node type
         * @return a ConfigNode builder
         */
        Builder addType(NodeType type);

        /**
         * Adds the value of the instance node.
         *
         * @param value at the node
         * @return a ConfigNode builder
         */
        Builder addValue(Class<V> value);

        /**
         * Adds children to the children field.
         *
         * @param children to be added
         * @return a ConfigNode builder
         */
        Builder addChildren(Class<ConfigNode> children);

        /**
         * Builds an immutable ConfigNode entity.
         *
         * @return ConfigNode
         */
        ConfigNode build();
    }

    /**
     * Returns the type of the instance node.
     *
     * @return node type
     */
    NodeType type();

    /**
     * Returns the value of the instance node.
     *
     * @return value at  the node
     */
    Class<V> value();

    /**
     * Returns the children of the instance node.
     *
     * @return children of the node
     */
    List<ConfigNode> children();
}