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
 * Implementation of IP ECN (Explicit Congestion Notification) criterion
 * (2 bits).
 */
public final class IPEcnCriterion implements Criterion {
    private static final byte MASK = 0x3;
    private final byte ipEcn;               // IP ECN value: 2 bits

    /**
     * Constructor.
     *
     * @param ipEcn the IP ECN value to match (2 bits)
     */
    IPEcnCriterion(byte ipEcn) {
        this.ipEcn = (byte) (ipEcn & MASK);
    }

    @Override
    public Type type() {
        return Type.IP_ECN;
    }

    /**
     * Gets the IP ECN value to match.
     *
     * @return the IP ECN value to match (2 bits)
     */
    public byte ipEcn() {
        return ipEcn;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + Long.toHexString(ipEcn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), ipEcn);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPEcnCriterion) {
            IPEcnCriterion that = (IPEcnCriterion) obj;
            return Objects.equals(ipEcn, that.ipEcn) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
