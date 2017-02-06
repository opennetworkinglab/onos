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

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of an entity which identifies a uniquely branching
 * leaf-list entry corresponding to a multi instance leaf schema.
 */
public final class LeafListKey extends NodeKey<LeafListKey>
        implements Comparable<LeafListKey> {
    private Object val;

    /**
     * Create object from builder.
     *
     * @param builder initialized builder
     */
    private LeafListKey(LeafListKeyBuilder builder) {
        super(builder);
        val = builder.val;
    }

    /**
     * Returns value of node, this is only valid for multi-instance leaf, node.
     *
     * @return value maintained in the node
     */
    Object value() {
        return val;
    }

    /**
     * Returns value as string, for usage in serializers.
     *
     * @return string representation of value
     */
    String asString() {
        return val.toString();
    }

    /**
     * Creates and returns a deep copy of this object.
     *
     * @return cloned copy
     * @throws CloneNotSupportedException if the object's class does not
     *                                    support the {@code Cloneable} interface
     */
    public LeafListKey clone() throws CloneNotSupportedException {
        return (LeafListKey) super.clone();
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaId, val);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!getClass().equals(obj.getClass())) {
            return false;
        }

        LeafListKey that = (LeafListKey) obj;
        return Objects.equals(val, that.val) &&
                Objects.equals(schemaId, that.schemaId);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("value", val)
                .toString();
    }

    /**
     * Represents Leaf list key builder.
     */
    public static class LeafListKeyBuilder
            extends NodeKeyBuilder<LeafListKeyBuilder> {

        private Object val;

        /**
         * constructor used while constructing the key from scratch.
         */
        public LeafListKeyBuilder() {

        }

        /**
         * Adds the value for for the leaf list node identifier.
         *
         * @param val leaf list value
         * @return LeafListKeyBuilder
         */
        LeafListKeyBuilder value(Object val) {
            this.val = val;
            return this;
        }

        /**
         * Creates a leaf list entry identifier.
         *
         * @return leaf list entry identifier
         */
        public LeafListKey build() {
            return new LeafListKey(this);
        }
    }
}
