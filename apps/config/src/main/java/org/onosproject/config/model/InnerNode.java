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
import java.util.Map;

import static org.onosproject.config.model.ModelConstants.LEAF_IS_TERMINAL;

/**
 * Abstraction of an entity which represents an inner node in data store.
 */
public final class InnerNode extends DataNode {

    /**
     * Map containing info of all child data nodes with respect to their node
     * keys.
     */
    private Map<NodeKey, DataNode> childNodes = new LinkedHashMap<>();

    /**
     * Returns the children nodes to the current node.
     * Children nodes are identified based on the node key.
     *
     * @return read only linked map of children nodes
     */
    public Map<NodeKey, DataNode> childNodes() {
        return childNodes;
    }

    /**
     * Creates an instance of inner node.
     *
     * @param builder inner node builder
     */
    public InnerNode(Builder builder) {
        super(builder);
        childNodes = builder.childNodes;
    }

    /**
     * Returns inner node builder instance.
     *
     * @param name      name of node
     * @param nameSpace namespace of node
     * @return inner node builder instance
     */
    public static Builder builder(String name, String nameSpace) {
        return new Builder(name, nameSpace);
    }

    /**
     * Returns inner node copy builder.
     *
     * @return inner node copy builder
     */
    @Override
    public Builder copyBuilder() {
        return new Builder(this);
    }

    /**
     * Builder with get and set functions to build inner node,
     * builder will be used both to create inner node from scratch or from a
     * given inner node.
     */
    public static class Builder extends DataNode.Builder<Builder> {

        /**
         * Map containing info of all child data nodes with respect to their
         * node keys.
         */
        private Map<NodeKey, DataNode> childNodes = new LinkedHashMap<>();

        public Builder() {
        }
        /**
         * Creates an instance of data node builder.
         *
         * @param name      name of node
         * @param namespace namespace of node
         */
        public Builder(String name, String namespace) {
            keyBuilder = NodeKey.builder().schemaId(name, namespace);
        }

        /**
         * Creates an instance of inner node builder.
         *
         * @param node old inner node
         */
        public Builder(InnerNode node) {
            super(node);
            childNodes = node.childNodes;
        }

        /**
         * Adds node to the builder.
         *
         * @param node node to be added
         * @return inner node builder
         */
        public Builder addNode(DataNode node) {
            childNodes.put(node.key(), node);
            return this;
        }

        /**
         * Builds a inner node object.
         *
         * @return inner node
         */
        @Override
        public InnerNode build() {
            if (type == null) {
                throw new IllegalStateException("node should have a type.");
            }
            if (key == null) {
                key = keyBuilder.build();
            }
            return new InnerNode(this);
        }

        @Override
        public InnerNode.Builder createChildBuilder(String name, String nameSpace) {
            return InnerNode.builder(name, nameSpace)
                    .parent(this);
        }

        @Override
        public LeafNode.Builder createChildBuilder(String name, String nameSpace,
                                                   Object value) {
            return LeafNode.builder(name, nameSpace)
                    .parent(this)
                    .value(value);
        }

        @Override
        public InnerNode.Builder deleteChild(NodeKey key) {
            childNodes.remove(key);
            return this;
        }

        @Override
        public Builder getChildBuilder(NodeKey nodeKey) {
            DataNode node = childNodes.get(nodeKey);
            if (node == null) {
                throw new IllegalArgumentException(
                        "Invalid key: no child nodes found for given key: " +
                                nodeKey);
            }
            return (Builder) node.copyBuilder().parent(this);
        }

        @Override
        public Builder addKeyLeaf(String name, String nameSpace, Object val) {
            ListKey.ListKeyBuilder listKeyBuilder;
            if (!(keyBuilder instanceof ListKey.ListKeyBuilder)) {
                if (keyBuilder instanceof LeafListKey.LeafListKeyBuilder) {
                    throw new ModelException(LEAF_IS_TERMINAL);
                }

                listKeyBuilder = new ListKey.ListKeyBuilder(keyBuilder);
            } else {
                listKeyBuilder = (ListKey.ListKeyBuilder) keyBuilder;
            }

            listKeyBuilder.addKeyLeaf(name, nameSpace, val);
            keyBuilder = listKeyBuilder;
            return this;
        }

        @Override
        public LeafNode.Builder addLeafListValue(Object val) {
            throw new IllegalStateException("node is not of leaf list type.");
        }
    }
}
