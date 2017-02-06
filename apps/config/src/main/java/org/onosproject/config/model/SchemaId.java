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
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.config.model.ModelConstants.INCOMPLETE_SCHEMA_INFO;

/**
 * Representation of an entity which identifies a schema node in the schema /
 * data tree.
 */
public class SchemaId implements Comparable<SchemaId>, Cloneable {

    private String name;
    private String nameSpace;

    private SchemaId() {
    }

    public SchemaId(String name, String nameSpace) {
        checkNotNull(name, INCOMPLETE_SCHEMA_INFO);
        checkNotNull(nameSpace, INCOMPLETE_SCHEMA_INFO);
        this.name = name;
        this.nameSpace = nameSpace;
    }

    /**
     * Returns node schema name. This is mandatory to identify node according
     * to schema.
     *
     * @return node name
     */
    public String name() {
        return name;
    }

    /**
     * Returns node's namespace. This is mandatory serializers must translate
     * any implicit namespace to explicit namespace.
     *
     * @return node's namespace
     */
    public String namespace() {
        return nameSpace;
    }

    /**
     * Creates and returns a deep copy of this object.
     *
     * @return cloned copy
     * @throws CloneNotSupportedException if the object's class does not
     *                                    support the {@code Cloneable} interface
     */
    public SchemaId clone() throws CloneNotSupportedException {
        return (SchemaId) super.clone();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, nameSpace);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        SchemaId that = (SchemaId) obj;
        return Objects.equals(name, that.name) &&
                Objects.equals(nameSpace, that.nameSpace);
    }

    @Override
    public int compareTo(SchemaId o) {
        checkNotNull(o);
        if (name.equals(o.name)) {
            if (nameSpace.equals(o.nameSpace)) {
                return 0;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("name", name)
                .add("nameSpace", nameSpace)
                .toString();
    }
}