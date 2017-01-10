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
 * Representation of an instance node in the Dynamic config store.
 */
public final class DefaultDataNode implements DataNode {
    DataNode.Type type;
    NodeKey key;
    //Object value;
    String value;
    LinkedHashMap<NodeKey, DataNode> children;

    /**
     * Creates a new DefaultDataNode.
     *
     * @param key node key
     * @param type of the node
     * @param value of leaf node
     * @param children of the inner node
     */
    private DefaultDataNode(NodeKey key, DataNode.Type type,
                            String value, LinkedHashMap<NodeKey, DataNode> children) {
        this.type = type;
        this.key = key;
        this.value = value;
        this.children = children;
    }
    /**
     *
     */
    /**
     * Creates a new DefaultDataNode.
     *
     * @param node to be cloned
     * @param value of leaf node
     */
    private DefaultDataNode(DataNode node, String value) {
        this.type = node.type();
        this.key = node.key();
        this.value = value;
        this.children = null;
    }
    /**
     * Creates a new DefaultDataNode.
     *
     * @param node to be cloned
     * @param children to be added
     */
    private DefaultDataNode(DataNode node, LinkedHashMap<NodeKey, DataNode> children) {
        this.type = node.type();
        this.key = node.key();
        this.value = null;
        this.children = children;
    }

    @Override
    public LinkedHashMap<NodeKey, DataNode> children() {
        return this.children;
    }

    @Override
    public  String value() {
        return value;
        //return value.toString();
    }


    /**
     * Creates and returns a new builder instance.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder<V> implements DataNode.Builder {

        private DataNode.Type type;
        private NodeKey key;
        //Object value;
        private String value;
        private LinkedHashMap<NodeKey, DataNode> children;

        private Builder() {
            this.type = null;
            this.key = null;
            this.value = null;
            this.children = null;
        }

        @Override
        public Builder addBaseObj(DataNode base) {
            this.key = base.key();
            this.type = base.type();
            this.value = base.value();
            this.children = base.children();
            return this;
        }

        @Override
        public Builder addKey(NodeKey key) {
            this.key = key;
            return this;
        }

        @Override
        public Builder addType(DataNode.Type type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder addValue(String value) {
            this.value = value;
            return this;
        }

        //@Override
        public Builder addChildren(LinkedHashMap<NodeKey, DataNode> children) {
            this.children = children;
            return this;
        }

        @Override
        public DataNode build() {
            return new DefaultDataNode(this.key, this.type, this.value, this.children);
        }
    }


    @Override
    public SchemaIdentifier identifier() {
        return this.key.schemaId;
    }

    @Override
    public Type type() {
        return this.type;
    }

    @Override
    public NodeKey key() {
        return this.key;
    }
}