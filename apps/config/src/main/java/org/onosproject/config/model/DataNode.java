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
package org.onosproject.config.model;

import java.util.LinkedHashMap;

/**
 * Hollow definition of DataNode for ConfigService APIs.
 */
public interface DataNode {
    //will remove this when the corresponding changes in onos-yang-tools become available

    /**
     * Builder for DataNode.
     */
    interface Builder<V> {
        /**
         * clones a base Data node obj to a new one.
         *
         * @param base base DataNode obj to be cloned
         * @return a DataNode builder
         */
        Builder addBaseObj(DataNode base);
        /**
         * Adds the value of the instance node.
         *
         * @param key of the node
         * @return a DataNode builder
         */
        Builder addKey(NodeKey key);
        /**
         * Adds the value of the instance node.
         *
         * @param type of the node
         * @return a DataNode builder
         */
        Builder addType(DataNode.Type type);
        /**
         * Adds the value of the leaf node.
         *
         * @param value at the node
         * @return a DataNode builder
         */
        Builder addValue(String value);

        /**
         * Adds children to the children field.
         *
         * @param children to be added
         * @return a DataNode builder
         */
        //Builder addChildren(LinkedHashMap<NodeKey, DataNode> children);

        /**
         * Builds an immutable DataNode entity.
         *
         * @return DataNode
         */
        DataNode build();
    }

    /**
     * Returns the children if DataNode contains an inner node.
     *
     * @return LinkedHashMap of children for an inner node, null for a leaf node
     */
    LinkedHashMap<NodeKey, DataNode> children();

    /**
     * Returns the value at the leaf node as a string.
     *
     * @return value at the leaf node as a string, null if it is an innernode
     */
    String value();

    /**
     * Returns the node schema identifier.
     *
     * @return node schema identifier
     */
    SchemaIdentifier identifier();

    /**
     * Returns the type of node.
     *
     * @return node type
     */
    Type type();

    /**
     * Returns the key to identify a branching node.
     *
     * @return key to identify a branching node
     */
    NodeKey key();

    /**
     * Represents type of node in data store.
     */
    enum Type {

        /**
         * Single instance node.
         */
        SINGLE_INSTANCE_NODE,

        /**
         * Multi instance node.
         */
        MULTI_INSTANCE_NODE,

        /**
         * Single instance leaf node.
         */
        SINGLE_INSTANCE_LEAF_VALUE_NODE,

        /**
         * Multi instance leaf node.
         */
        MULTI_INSTANCE_LEAF_VALUE_NODE
    }
}
