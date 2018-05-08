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
import org.onosproject.net.pi.model.PiData;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * PiHeaderUnion entity in a protocol-independent pipeline.
 */
@Beta
public final class PiHeaderUnion implements PiData {
    private final String validHeaderName;
    private final PiHeader header;
    private final boolean isValid;

    /**
     * Creates a new protocol-independent header union instance for the given header name and header.
     *
     * @param isValid indicates whether this header union is valid
     * @param validHeaderName header name
     * @param header the header
     */
    private PiHeaderUnion(boolean isValid, String validHeaderName, PiHeader header) {
        this.isValid = isValid;
        this.validHeaderName = validHeaderName;
        this.header = header;
    }

    /**
     * Returns a new invalid protocol-independent header union.
     *
     * @return header union
     */
    public static PiHeaderUnion ofInvalid() {
        return new PiHeaderUnion(false, null, null);
    }

    /**
     * Returns a new valid protocol-independent header union.
     * @param validHeaderName header name
     * @param header the header
     * @return header union
     */
    public static PiHeaderUnion of(String validHeaderName, PiHeader header) {
        checkNotNull(validHeaderName);
        checkArgument(!validHeaderName.isEmpty(), "The header name must not be empty");
        checkNotNull(header);
        return new PiHeaderUnion(true, validHeaderName, header);
    }

    /**
     * Returns true if this header is valid, false otherwise.
     *
     * @return a boolean value
     */
    public boolean isValid() {
        return this.isValid;
    }

    /**
     * Return the header name.
     *
     * @return header name, return null if the header union invalid
     */
    public String headerName() {
        return this.validHeaderName;
    }

    /**
     * Return the header.
     *
     * @return header, return null if the header union invalid
     */
    public PiHeader header() {
        return this.header;
    }

    @Override
    public Type type() {
        return Type.HEADERUNION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiHeaderUnion headerUnion = (PiHeaderUnion) o;
        return Objects.equal(validHeaderName, headerUnion.validHeaderName) &&
                Objects.equal(header, headerUnion.header);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(validHeaderName, header);
    }

    @Override
    public String toString() {
        return !isValid ? "INVALID" : validHeaderName + ":" + header.toString();
    }
}