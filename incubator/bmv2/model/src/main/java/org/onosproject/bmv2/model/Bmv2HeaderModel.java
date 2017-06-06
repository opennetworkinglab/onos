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

package org.onosproject.bmv2.model;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.pi.model.PiHeaderModel;
import org.onosproject.net.pi.model.PiHeaderTypeModel;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.*;

/**
 * BMv2 header model.
 */
@Beta
public final class Bmv2HeaderModel implements PiHeaderModel {
    private final String name;
    private final int id;
    private final int index;
    private final Bmv2HeaderTypeModel type;
    private final boolean isMetadata;

    /**
     * Builds a new BMv2 header model with given information.
     *
     * @param name the name of this header mdoel
     * @param id the id of this header model
     * @param index the header index
     * @param type the type of this header model
     * @param metadata if the header is metadata
     */
    public Bmv2HeaderModel(String name, int id, int index, Bmv2HeaderTypeModel type, boolean metadata) {
        checkNotNull(name, "Model name can't be null.");
        checkArgument(index >= 0, "Index should be a positive integer");
        checkNotNull(type, "Header type can't be null.");
        this.name = name;
        this.id = id;
        this.index = index;
        this.type = type;
        this.isMetadata = metadata;
    }

    /**
     * Gets the name of this header model.
     *
     * @return name of this model
     */
    public String name() {
        return name;
    }

    /**
     * Gets the id of this header model.
     *
     * @return if of this header model
     */
    public int id() {
        return id;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public PiHeaderTypeModel type() {
        return type;
    }

    @Override
    public boolean isMetadata() {
        return isMetadata;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, type, isMetadata);
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
        return Objects.equal(this.id, other.id)
                && Objects.equal(this.type, other.type)
                && Objects.equal(this.isMetadata, other.isMetadata);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("index", id)
                .add("type", type)
                .add("isMetadata", isMetadata)
                .toString();
    }
}
