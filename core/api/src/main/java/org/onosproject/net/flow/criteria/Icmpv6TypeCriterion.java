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
 * Implementation of ICMPv6 type criterion (8 bits unsigned integer).
 */
public final class Icmpv6TypeCriterion implements Criterion {
    private static final short MASK = 0xff;
    private final short icmpv6Type;         // ICMPv6 type: 8 bits

    /**
     * Constructor.
     *
     * @param icmpv6Type the ICMPv6 type to match (8 bits unsigned integer)
     */
    Icmpv6TypeCriterion(short icmpv6Type) {
        this.icmpv6Type = (short) (icmpv6Type & MASK);
    }

    @Override
    public Type type() {
        return Type.ICMPV6_TYPE;
    }

    /**
     * Gets the ICMPv6 type to match.
     *
     * @return the ICMPv6 type to match (8 bits unsigned integer)
     */
    public short icmpv6Type() {
        return icmpv6Type;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + icmpv6Type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), icmpv6Type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Icmpv6TypeCriterion) {
            Icmpv6TypeCriterion that = (Icmpv6TypeCriterion) obj;
            return Objects.equals(icmpv6Type, that.icmpv6Type) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
