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
 * Implementation of TCP flags criterion (12 bits unsigned integer).
 */
public final class TcpFlagsCriterion implements Criterion {
    private static final int MASK = 0xfffff;
    private final int flags;            // TCP flags: 12 bits

    /**
     * Constructor.
     *
     * @param flags the TCP flags to match (12 bits)
     */
    TcpFlagsCriterion(int flags) {
        this.flags = flags & MASK;
    }

    @Override
    public Type type() {
        return Type.TCP_FLAGS;
    }

    /**
     * Gets the TCP flags to match.
     *
     * @return the TCP flags to match (12 bits)
     */
    public int flags() {
        return flags;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + Long.toHexString(flags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), flags);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TcpFlagsCriterion) {
            TcpFlagsCriterion that = (TcpFlagsCriterion) obj;
            return Objects.equals(flags, that.flags) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
