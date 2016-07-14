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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * A BMv2 action model.
 */
@Beta
public final class Bmv2ActionModel {

    private final String name;
    private final int id;
    private final LinkedHashMap<String, Bmv2RuntimeDataModel> runtimeDatas = Maps.newLinkedHashMap();

    /**
     * Creates a new action model.
     *
     * @param name         name
     * @param id           id
     * @param runtimeDatas list of runtime data
     */
    protected Bmv2ActionModel(String name, int id, List<Bmv2RuntimeDataModel> runtimeDatas) {
        this.name = name;
        this.id = id;
        runtimeDatas.forEach(r -> this.runtimeDatas.put(r.name(), r));
    }

    /**
     * Returns the name of this action.
     *
     * @return a string value
     */
    public String name() {
        return name;
    }

    /**
     * Returns the id of this action.
     *
     * @return an integer value
     */
    public int id() {
        return id;
    }

    /**
     * Returns this action's runtime data defined by the given name, null
     * if not present.
     *
     * @return runtime data or null
     */
    public Bmv2RuntimeDataModel runtimeData(String name) {
        return runtimeDatas.get(name);
    }

    /**
     * Returns an immutable list of runtime data for this action.
     * The list is ordered according to the values defined in the configuration.
     *
     * @return list of runtime data.
     */
    public List<Bmv2RuntimeDataModel> runtimeDatas() {
        return ImmutableList.copyOf(runtimeDatas.values());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, runtimeDatas);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2ActionModel other = (Bmv2ActionModel) obj;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.id, other.id)
                && Objects.equals(this.runtimeDatas, other.runtimeDatas);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("id", id)
                .add("runtimeDatas", runtimeDatas)
                .toString();
    }

}
