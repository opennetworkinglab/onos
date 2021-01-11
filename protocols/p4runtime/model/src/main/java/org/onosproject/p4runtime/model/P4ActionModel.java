/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.p4runtime.model;

import com.google.common.collect.ImmutableMap;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionModel;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiActionParamModel;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of PiActionModel for P4Runtime.
 */
final class P4ActionModel implements PiActionModel {

    private final PiActionId id;
    private final ImmutableMap<PiActionParamId, PiActionParamModel> params;

    P4ActionModel(PiActionId id,
                  ImmutableMap<PiActionParamId, PiActionParamModel> params) {
        this.id = id;
        this.params = params;
    }

    @Override
    public PiActionId id() {
        return id;
    }

    @Override
    public Optional<PiActionParamModel> param(PiActionParamId paramId) {
        return Optional.ofNullable(params.get(paramId));
    }

    @Override
    public Collection<PiActionParamModel> params() {
        return params.values();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, params);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final P4ActionModel other = (P4ActionModel) obj;
        return Objects.equals(this.id, other.id)
                && Objects.equals(this.params, other.params);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("params", params.values())
                .toString();
    }
}
