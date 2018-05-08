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
 * Instance of a PiHeaderStack in a protocol-independent pipeline.
 */
@Beta
public final class PiHeaderStack implements PiData {
    private final ImmutableList<PiHeader> headers;

    /**
     * Creates a new protocol-independent header stack instance for the given collection of Header.
     *
     * @param headers the collection of header
     */
    private PiHeaderStack(List<PiHeader> headers) {
        this.headers = ImmutableList.copyOf(headers);
    }

    /**
     * Returns a new protocol-independent header stack.
     * @param headers the list of header
     * @return header stack
     */
    public static PiHeaderStack of(List<PiHeader> headers) {
        return new PiHeaderStack(headers);
    }

    /**
     * Return the list of header.
     *
     * @return the list of header
     */
    public List<PiHeader> headers() {
        return this.headers;
    }

    @Override
    public Type type() {
        return Type.HEADERSTACK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiHeaderStack stack = (PiHeaderStack) o;
        return Objects.equal(headers, stack.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(headers);
    }

    @Override
    public String toString() {
        StringJoiner stringParams = new StringJoiner(", ", "(", ")");
        this.headers().forEach(p -> stringParams.add(p.toString()));
        return stringParams.toString();
    }
}