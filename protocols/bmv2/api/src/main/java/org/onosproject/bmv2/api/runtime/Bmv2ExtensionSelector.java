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

package org.onosproject.bmv2.api.runtime;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Extension selector for BMv2 used as a wrapper for multiple BMv2 match parameters.
 */
public final class Bmv2ExtensionSelector extends AbstractExtension implements ExtensionSelector {

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(HashMap.class)
            .register(Bmv2MatchParam.class)
            .register(Bmv2ExactMatchParam.class)
            .register(Bmv2TernaryMatchParam.class)
            .register(Bmv2LpmMatchParam.class)
            .register(Bmv2ValidMatchParam.class)
            .build();

    private Map<String, Bmv2MatchParam> parameterMap;

    /**
     * Creates a new BMv2 extension selector for the given match parameters map, where the keys are expected to be field
     * names formatted as headerName.fieldMemberName (e.g. ethernet.dstAddr).
     *
     * @param paramMap a map
     */
    public Bmv2ExtensionSelector(Map<String, Bmv2MatchParam> paramMap) {
        this.parameterMap = checkNotNull(paramMap, "param map cannot be null");
    }

    /**
     * Returns the match parameters map of this selector.
     *
     * @return a match parameter map
     */
    public Map<String, Bmv2MatchParam> parameterMap() {
        return parameterMap;
    }


    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.BMV2_MATCH_PARAMS.type();
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(parameterMap);
    }

    @Override
    public void deserialize(byte[] data) {
        this.parameterMap = appKryo.deserialize(data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parameterMap);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2ExtensionSelector other = (Bmv2ExtensionSelector) obj;
        return Objects.equal(this.parameterMap, other.parameterMap);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("parameterMap", parameterMap)
                .toString();
    }
}
