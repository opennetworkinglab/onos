/*
 * Copyright 2015-present Open Networking Foundation
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
 * Implementation of ICMP code criterion (8 bits unsigned integer).
 */
public final class IcmpCodeCriterion implements Criterion {
    private static final short MASK = 0xff;
    private final short icmpCode;           // The ICMP code: 8 bits

    /**
     * Constructor.
     *
     * @param icmpCode the ICMP code to match (8 bits unsigned integer)
     */
    IcmpCodeCriterion(short icmpCode) {
        this.icmpCode = (short) (icmpCode & MASK);
    }

    @Override
    public Type type() {
        return Type.ICMPV4_CODE;
    }

    /**
     * Gets the ICMP code to match.
     *
     * @return the ICMP code to match (8 bits unsigned integer)
     */
    public short icmpCode() {
        return icmpCode;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + icmpCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), icmpCode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IcmpCodeCriterion) {
            IcmpCodeCriterion that = (IcmpCodeCriterion) obj;
            return Objects.equals(icmpCode, that.icmpCode) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
