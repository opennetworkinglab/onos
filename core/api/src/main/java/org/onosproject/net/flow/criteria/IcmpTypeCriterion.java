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
 * Implementation of ICMP type criterion (8 bits unsigned integer).
 */
public final class IcmpTypeCriterion implements Criterion {
    private static final short MASK = 0xff;
    private final short icmpType;           // The ICMP type: 8 bits

    /**
     * Constructor.
     *
     * @param icmpType the ICMP type to match (8 bits unsigned integer)
     */
    IcmpTypeCriterion(short icmpType) {
        this.icmpType = (short) (icmpType & MASK);
    }

    @Override
    public Type type() {
        return Type.ICMPV4_TYPE;
    }

    /**
     * Gets the ICMP type to match.
     *
     * @return the ICMP type to match (8 bits unsigned integer)
     */
    public short icmpType() {
        return icmpType;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + icmpType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), icmpType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IcmpTypeCriterion) {
            IcmpTypeCriterion that = (IcmpTypeCriterion) obj;
            return Objects.equals(icmpType, that.icmpType) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
