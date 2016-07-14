/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.flow.criteria;

import java.util.Objects;

/**
 * Implementation of IPv6 Extension Header pseudo-field criterion
 * (16 bits). Those are defined in Criterion.IPv6ExthdrFlags.
 */
public final class IPv6ExthdrFlagsCriterion implements Criterion {
    private static final int MASK = 0xffff;
    private final int exthdrFlags;          // IPv6 Exthdr flags: 16 bits

    /**
     * Constructor.
     *
     * @param exthdrFlags the IPv6 Extension Header pseudo-field flags
     * to match (16 bits). Those are defined in Criterion.IPv6ExthdrFlags
     */
    IPv6ExthdrFlagsCriterion(int exthdrFlags) {
        this.exthdrFlags = exthdrFlags & MASK;
    }

    @Override
    public Type type() {
        return Type.IPV6_EXTHDR;
    }

    /**
     * Gets the IPv6 Extension Header pseudo-field flags to match.
     *
     * @return the IPv6 Extension Header pseudo-field flags to match
     * (16 bits). Those are defined in Criterion.IPv6ExthdrFlags
     */
    public int exthdrFlags() {
        return exthdrFlags;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + Long.toHexString(exthdrFlags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), exthdrFlags);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPv6ExthdrFlagsCriterion) {
            IPv6ExthdrFlagsCriterion that = (IPv6ExthdrFlagsCriterion) obj;
            return Objects.equals(exthdrFlags, that.exthdrFlags) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
