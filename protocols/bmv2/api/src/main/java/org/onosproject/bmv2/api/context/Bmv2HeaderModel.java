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

package org.onosproject.bmv2.api.context;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * BMv2 header instance model.
 */
@Beta
public final class Bmv2HeaderModel {

    private final String name;
    private final int id;
    private final Bmv2HeaderTypeModel type;
    private final boolean isMetadata;

    /**
     * Creates a new header instance model.
     *
     * @param name     name
     * @param id       id
     * @param type     header type
     * @param metadata if is metadata
     */
    protected Bmv2HeaderModel(String name, int id, Bmv2HeaderTypeModel type, boolean metadata) {
        this.name = name;
        this.id = id;
        this.type = type;
        this.isMetadata = metadata;
    }

    /**
     * Returns the name of this header instance.
     *
     * @return a string value
     */
    public String name() {
        return name;
    }

    /**
     * Return the id of this header instance.
     *
     * @return an integer value
     */
    public int id() {
        return id;
    }

    /**
     * Return the type of this header instance.
     *
     * @return a header type value
     */
    public Bmv2HeaderTypeModel type() {
        return type;
    }

    /**
     * Return true if this header instance is a metadata, false elsewhere.
     *
     * @return a boolean value
     */
    public boolean isMetadata() {
        return isMetadata;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, id, type, isMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2HeaderModel other = (Bmv2HeaderModel) obj;
        return Objects.equal(this.name, other.name)
                && Objects.equal(this.id, other.id)
                && Objects.equal(this.type, other.type)
                && Objects.equal(this.isMetadata, other.isMetadata);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("id", id)
                .add("type", type)
                .add("isMetadata", isMetadata)
                .toString();
    }
}
