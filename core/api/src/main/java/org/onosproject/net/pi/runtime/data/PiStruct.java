/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.net.pi.runtime.data;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import org.onosproject.net.pi.model.PiData;

import java.util.List;
import java.util.StringJoiner;

/**
 * Struct entity in a protocol-independent pipeline.
 */
@Beta
public final class PiStruct implements PiData {
    private final ImmutableList<PiData> struct;

    /**
     * Creates a new protocol-independent struct instance for the given collection of PiData.
     *
     * @param struct the collection of PiData
     */
    private PiStruct(List<PiData> struct) {
        this.struct = ImmutableList.copyOf(struct);
    }

    /**
     * Returns a new protocol-independent struct.
     * @param struct the list of PiData
     * @return struct
     */
    public static PiStruct of(List<PiData> struct) {
        return new PiStruct(struct);
    }

    /**
     * Return protocol-independent struct instance.
     *
     * @return the list of PiData
     */
    public List<PiData> struct() {
        return this.struct;
    }

    @Override
    public Type type() {
        return Type.STRUCT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiStruct st = (PiStruct) o;
        return Objects.equal(struct, st.struct);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(struct);
    }

    @Override
    public String toString() {
        StringJoiner stringParams = new StringJoiner(", ", "(", ")");
        this.struct().forEach(p -> stringParams.add(p.toString()));
        return stringParams.toString();
    }
}