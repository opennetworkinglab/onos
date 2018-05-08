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
 * Tuple entity in a protocol-independent pipeline.
 */
@Beta
public final class PiTuple implements PiData {
    private final ImmutableList<PiData> tuple;

    /**
     * Creates a new protocol-independent tuple instance for the given collection of PiData.
     *
     * @param tuple the collection of PiData
     */
    private PiTuple(List<PiData> tuple) {
        this.tuple = ImmutableList.copyOf(tuple);
    }

    /**
     * Returns a new protocol-independent tuple.
     * @param tuple the list of PiData
     * @return tuple
     */
    public static PiTuple of(List<PiData> tuple) {
        return new PiTuple(tuple);
    }

    /**
     * Return protocol-independent tuple instance.
     *
     * @return the list of PiData
     */
    public List<PiData> tuple() {
        return this.tuple;
    }

    @Override
    public Type type() {
        return Type.TUPLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiTuple tp = (PiTuple) o;
        return Objects.equal(tuple, tp.tuple);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tuple);
    }

    @Override
    public String toString() {
        StringJoiner stringParams = new StringJoiner(", ", "(", ")");
        this.tuple().forEach(p -> stringParams.add(p.toString()));
        return stringParams.toString();
    }
}