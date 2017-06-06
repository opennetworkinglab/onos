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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.onosproject.net.pi.model.PiHeaderFieldTypeModel;
import org.onosproject.net.pi.model.PiHeaderTypeModel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * BMv2 header type model.
 */
@Beta
public final class Bmv2HeaderTypeModel implements PiHeaderTypeModel {
    private final String name;
    private final int id;
    private final ImmutableMap<String, PiHeaderFieldTypeModel> fields;

    /**
     * Builds a BMv2 header type model by given information.
     *
     * @param name the name
     * @param id the id
     * @param fields the fields
     */
    public Bmv2HeaderTypeModel(String name,
                               int id,
                               List<Bmv2HeaderFieldTypeModel> fields) {
        checkNotNull(name, "Type name can't be null");
        checkNotNull(fields, "Fields can't be null");
        this.name = name;
        this.id = id;
        ImmutableMap.Builder<String, PiHeaderFieldTypeModel> mapBuilder = ImmutableMap.builder();
        fields.forEach(field -> mapBuilder.put(field.name(), field));
        this.fields = mapBuilder.build();
    }

    /**
     * Gets id of this header type model.
     *
     * @return the id
     */
    public int id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<PiHeaderFieldTypeModel> field(String fieldName) {
        return Optional.ofNullable(fields.get(fieldName));
    }

    @Override
    public List<PiHeaderFieldTypeModel> fields() {
        return (ImmutableList<PiHeaderFieldTypeModel>) fields.values();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fields);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2HeaderTypeModel other = (Bmv2HeaderTypeModel) obj;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.fields, other.fields);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("fields", fields)
                .toString();
    }
}
