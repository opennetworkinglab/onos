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

import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

/**
 * PiHeaderUnionStack entity in a protocol-independent pipeline.
 */
@Beta
public final class PiHeaderUnionStack implements PiData {
    private final ImmutableList<PiHeaderUnion> headerUnions;

    /**
     * Creates a new protocol-independent header union stack instance for the given collection of HeaderUnion.
     *
     * @param headerUnions the collection of headerUnion
     */
    private PiHeaderUnionStack(List<PiHeaderUnion> headerUnions) {
        this.headerUnions = ImmutableList.copyOf(headerUnions);
    }

    /**
     * Returns a new protocol-independent header union stack.
     * @param headerUnions the list of headerUnion
     * @return header union stack
     */
    public static PiHeaderUnionStack of(List<PiHeaderUnion> headerUnions) {
        return new PiHeaderUnionStack(headerUnions);
    }

    /**
     * Return the collection of header union.
     *
     * @return the collection header union
     */
    public Collection<PiHeaderUnion> headerUnions() {
        return this.headerUnions;
    }

    @Override
    public Type type() {
        return Type.HEADERUNIONSTACK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiHeaderUnionStack headerUnionStack = (PiHeaderUnionStack) o;
        return Objects.equal(headerUnions, headerUnionStack.headerUnions);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(headerUnions);
    }

    @Override
    public String toString() {
        StringJoiner stringParams = new StringJoiner(", ", "(", ")");
        this.headerUnions().forEach(p -> stringParams.add(p.toString()));
        return stringParams.toString();
    }
}