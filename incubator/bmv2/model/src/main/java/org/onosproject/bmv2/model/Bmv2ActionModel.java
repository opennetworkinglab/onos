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
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.onosproject.net.pi.model.PiActionModel;
import org.onosproject.net.pi.model.PiActionParamModel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * BMv2 action model.
 */
@Beta
public final class Bmv2ActionModel implements PiActionModel {
    private final String name;
    private final int id;
    private final ImmutableMap<String, PiActionParamModel> params;

    /**
     * Builds BMv2 action model with given information.
     *
     * @param name the model name
     * @param id the model id
     * @param params the action parameters
     */
    public Bmv2ActionModel(String name, int id, List<Bmv2ActionParamModel> params) {
        checkNotNull(name, "Model name can't be null");
        checkNotNull(params, "Action parameters can't be null");

        this.name = name;
        this.id = id;
        ImmutableMap.Builder<String, PiActionParamModel> mapBuilder = ImmutableMap.builder();
        params.forEach(param -> mapBuilder.put(param.name(), param));
        this.params = mapBuilder.build();
    }

    /**
     * Gets id from this action model.
     *
     * @return the model id
     */
    public int id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<PiActionParamModel> param(String name) {
        checkNotNull(name, "Parameter name can't be null");
        return Optional.ofNullable(params.get(name));
    }

    @Override
    public List<PiActionParamModel> params() {
        return (ImmutableList<PiActionParamModel>) params.values();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, params);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Bmv2ActionModel)) {
            return false;
        }
        Bmv2ActionModel that = (Bmv2ActionModel) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.params, that.params);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .add("params", params)
                .toString();
    }
}
