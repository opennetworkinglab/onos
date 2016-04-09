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
 * Implementation of ICMPv6 code criterion (8 bits unsigned integer).
 */
public final class Icmpv6CodeCriterion implements Criterion {
    private static final short MASK = 0xff;
    private final short icmpv6Code;         // ICMPv6 code: 8 bits

    /**
     * Constructor.
     *
     * @param icmpv6Code the ICMPv6 code to match (8 bits unsigned integer)
     */
    Icmpv6CodeCriterion(short icmpv6Code) {
        this.icmpv6Code = (short) (icmpv6Code & MASK);
    }

    @Override
    public Type type() {
        return Type.ICMPV6_CODE;
    }

    /**
     * Gets the ICMPv6 code to match.
     *
     * @return the ICMPv6 code to match (8 bits unsigned integer)
     */
    public short icmpv6Code() {
        return icmpv6Code;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + icmpv6Code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), icmpv6Code);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Icmpv6CodeCriterion) {
            Icmpv6CodeCriterion that = (Icmpv6CodeCriterion) obj;
            return Objects.equals(icmpv6Code, that.icmpv6Code) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
