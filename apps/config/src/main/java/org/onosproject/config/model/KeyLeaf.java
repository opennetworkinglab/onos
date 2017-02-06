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
import static java.util.Objects.hash;

/**
 * Represents the List's key leaf value.
 */
public class KeyLeaf implements Cloneable {

    private SchemaId leafSchema;
    private Object leafVal;

    private KeyLeaf() {
    }

    /**
     * Constructs a key leaf with all the identifier and value initialized.
     *
     * @param name      name of the leaf
     * @param nameSpace namespace of leaf
     * @param leafVal   value of leaf
     */
    public KeyLeaf(String name, String nameSpace, Object leafVal) {
        leafSchema = new SchemaId(name, nameSpace);
        this.leafVal = leafVal;
    }

    /**
     * Creates and returns a deep copy of this object.
     *
     * @return cloned copy
     * @throws CloneNotSupportedException if the object's class does not
     *                                    support the {@code Cloneable} interface
     */
    public KeyLeaf clone() throws CloneNotSupportedException {
        KeyLeaf clonedLeaf = (KeyLeaf) super.clone();
        clonedLeaf.leafSchema = leafSchema.clone();
        return clonedLeaf;
    }

    /**
     * Returns the node schema schemaId.
     *
     * @return node schema schemaId
     */
    public SchemaId leafSchema() {
        return leafSchema;
    }

    /**
     * Returns value contained in leaf node.
     *
     * @return value contained in leaf node
     */
    public Object leafValue() {
        return leafVal;
    }

    /**
     * Returns value as string, for usage in serializers.
     *
     * @return string representation of value
     */
    public String leafValAsString() {
        return leafVal.toString();
    }

    @Override
    public int hashCode() {
        return hash(leafSchema, leafVal);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        KeyLeaf that = (KeyLeaf) obj;
        return Objects.equals(leafSchema, that.leafSchema) &&
                Objects.equals(leafVal, that.leafVal);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("schemaId", leafSchema)
                .add("leafValue", leafVal)
                .toString();
    }
}
