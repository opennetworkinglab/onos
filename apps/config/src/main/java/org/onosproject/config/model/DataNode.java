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

/**
 * Abstraction of an entity which represents data tree node. Information
 * exchange between YANG runtime, protocol and store will be based on this
 * node, agnostic of schema.
 */
public abstract class DataNode {

    /*
     * Represents type of node in data store.
     */
    public enum Type {

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

    /**
     * Type of node in data store.
     */
    protected Type type;

    /**
     * Identifies a node uniquely among its siblings.
     */
    protected NodeKey key;

    /**
     * Returns the type of node.
     *
     * @return node type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the key to identify a branching node.
     *
     * @return key to identify a branching node
     */
    public NodeKey key() {
        return key;
    }

    /**
     * Creates an instance of data node.
     *
     * @param builder data node builder
     */
    protected DataNode(Builder builder) {
        type = builder.type;
        key = builder.key;
    }

    /**
     * Returns data node builder for a given data node.
     * It contains all the attributes from the data node. It is to provide
     * mutability of data node using builder pattern.
     *
     * @return data node builder
     */
    public abstract Builder copyBuilder();

    /**
     * Represents the implementation of data node builder class.
     *
     * @param <B> type of data node builder
     */
    public abstract static  class Builder<B extends Builder<B>> {

        /**
         * Type of node in data store.
         */
        protected Type type;

        /**
         * Identifies a node uniquely among its siblings.
         */
        protected NodeKey key;

        /**
         * Node key builder.
         */
        protected NodeKey.NodeKeyBuilder keyBuilder;

        /**
         * Parent data node.
         */
        protected InnerNode.Builder parent;

        /**
         * Creates an instance of data node builder.
         */
        protected Builder() {
        }

        /**
         * Creates an instance of data node builder using old data node.
         *
         * @param node data node which
         */
        protected Builder(DataNode node) {
            type = node.type;
            key = node.key;
        }

        /**
         * Sets node key in builder object.
         * when serializers have an instance of key present with them they can
         * directly set the key value using this method.
         *
         * @param key node key identifier
         * @return data node builder object
         */
        public B key(NodeKey key) {
            this.key = key;
            return (B) this;
        }

        /**
         * Sets parent node's builder.
         *
         * @param node parent node builder
         * @return builder object
         */
        protected B parent(InnerNode.Builder node) {
            parent = node;
            return (B) this;
        }

        /**
         * Sets node type in builder object.
         *
         * @param type node type
         * @return data node builder
         */
        public B type(Type type) {
            this.type = type;
            return (B) this;
        }

        /**
         * Creates a child builder of type inner node and set a back reference
         * of parent node. it is used while creating a data tree.
         *
         * @param name      name of inner node
         * @param nameSpace namespace of inner node
         * @return child node builder
         */
        public abstract InnerNode.Builder createChildBuilder(
                String name, String nameSpace);

        /**
         * Creates a child build of type leaf node and set a back reference
         * of parent node. it is used while creating a data tree. the value
         * of leaf is set while creation.
         *
         * @param name      name of leaf node
         * @param nameSpace namespace of leaf node
         * @param value     value for leaf node
         * @return child node builder
         */
        public abstract LeafNode.Builder createChildBuilder(
                String name, String nameSpace, Object value);

        /**
         * Deletes child node for a given node key from parent node.
         * <p>
         * for deleting a node from data tree , caller should parse resource
         * identifier to reach to the child node. while parsing the resource
         * identifier caller need to create a new data node using copy
         * builder. this copy builder can be used further to create child
         * nodes copy builders.
         *
         * @param key node key for child node
         * @return data node builder
         */
        public abstract InnerNode.Builder deleteChild(NodeKey key);

        /**
         * Returns a child node builder for a given node key. it contains all
         * the attribute of child node. it is used to modify the data tree
         * while delete or update operations.
         * <p>
         * this method provides copy builder of child node when a
         * update/delete request comes. it sets a back reference of parent
         * node as well in child node's copy builder.
         *
         * @param key data node key
         * @return child node
         */
        public abstract InnerNode.Builder getChildBuilder(NodeKey key);


        /**
         * Add key leaf for list node key. It can be used while handling a
         * list node when in your yang file you have multiple key leaves.
         * <p>
         * this method is used for adding multiple key leaves in you list
         * node. these keys will be added to key builder which is built while
         * while node building. To use this method caller should know about
         * schema of list and key leaves.
         *
         * @param name      name of leaf node
         * @param nameSpace namespace of leaf node
         * @param val       value of leaf
         * @return data node builder
         */
        public abstract InnerNode.Builder addKeyLeaf(String name, String nameSpace,
                                                     Object val);

        /**
         * Add key value to leaf list key. this can be used while handling a
         * leaf list where you need to add multiple values.
         *
         * @param val value
         * @return data node builder
         */
        public abstract LeafNode.Builder addLeafListValue(Object val);

        /**
         * Builds data node.
         *
         * @return data node
         */
        public abstract DataNode build();

        /**
         * Returns parent node builder after building and adding the child
         * node to parent's child node map.
         * <p>
         * This method is used when caller has reached to the depth of the
         * subtree and then he wants to go back to its parent node so he
         * should build the node and then it should add it to parent node's
         * map. this method provides both the functionalities of build and
         * add to parent . Also it returns back the parent pointer so caller
         * can do further operations on data tree.
         *
         * @return parent's builder object
         */
        public InnerNode.Builder exitNode() {
            if (parent != null) {
                parent.addNode(build());
            }
            return parent;
        }
    }
}

