/*
 * Copyright 2017-present Open Networking Laboratory
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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents an entity which identifies a unique branching node
 * corresponding to a multi instance schema definition.
 */
public final class ListKey extends NodeKey<ListKey> implements Comparable<ListKey> {

    private List<KeyLeaf> keyLeafs;

    /**
     * Create object from builder.
     *
     * @param builder initialized builder
     */
    private ListKey(ListKeyBuilder builder) {
        super(builder);
        keyLeafs = builder.keyLeafs;
    }

    /**
     * Returns the list of key leaf nodes of a multi instance node, which
     * uniquely identifies the branching node entry corresponding to a multi
     * instance schema definition.
     *
     * @return List of key leaf nodes
     */
    List<KeyLeaf> keyLeafs() {
        return keyLeafs;
    }

    /**
     * Creates and returns a deep copy of this object.
     *
     * @return cloned copy
     * @throws CloneNotSupportedException if the object's class does not
     *                                    support the {@code Cloneable} interface
     */
    public ListKey clone() throws CloneNotSupportedException {
        ListKey clonedListKey = (ListKey) super.clone();
        List<KeyLeaf> clonedKeyLeafs = new LinkedList<>();
        for (KeyLeaf leaf : keyLeafs) {
            clonedKeyLeafs.add(leaf.clone());
        }
        clonedListKey.keyLeafs = clonedKeyLeafs;
        return clonedListKey;
    }

    public int compareTo(ListKey o) {
        //TODO: implement me
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaId, keyLeafs);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!getClass().equals(obj.getClass())) {
            return false;
        }

        ListKey that = (ListKey) obj;
        List<KeyLeaf> thatList = that.keyLeafs;
        return keyLeafs.size() == thatList.size() &&
                keyLeafs.containsAll(thatList) &&
                Objects.equals(schemaId, that.schemaId);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("value", keyLeafs)
                .toString();
    }

    /**
     * Represents list key builder.
     */
    public static class ListKeyBuilder extends NodeKeyBuilder<ListKeyBuilder> {
        private List<KeyLeaf> keyLeafs = new LinkedList<>();

        /**
         * used to construct the key from scratch.
         */
        public ListKeyBuilder() {
        }

        /**
         * used to construct a key from an existing node key.
         *
         * @param base existing node key
         */
        public ListKeyBuilder(NodeKeyBuilder base) {
            super(base);
        }

        /**
         * Adds the key leaf for the list resource.
         *
         * @param name      key leaf name
         * @param nameSpace key laef namespace
         * @param val       value of key
         */
        void addKeyLeaf(String name, String nameSpace, Object val) {
            KeyLeaf keyLeaf = new KeyLeaf(name, nameSpace, val);
            keyLeafs.add(keyLeaf);
        }

        /**
         * Creates the list key object.
         *
         * @return list key
         */
        public ListKey build() {
            return new ListKey(this);
        }
    }
}