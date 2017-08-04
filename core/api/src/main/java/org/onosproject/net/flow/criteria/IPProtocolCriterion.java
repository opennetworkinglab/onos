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
 * Implementation of Internet Protocol Number criterion (8 bits unsigned)
 * integer.
 */
public final class IPProtocolCriterion implements Criterion {
    private static final short MASK = 0xff;
    private final short proto;      // IP protocol number: 8 bits

    /**
     * Constructor.
     *
     * @param protocol the IP protocol (e.g., TCP=6, UDP=17) to match
     * (8 bits unsigned integer)
     */
    IPProtocolCriterion(short protocol) {
        this.proto = (short) (protocol & MASK);
    }

    @Override
    public Type type() {
        return Type.IP_PROTO;
    }

    /**
     * Gets the IP protocol to match.
     *
     * @return the IP protocol to match (8 bits unsigned integer)
     */
    public short protocol() {
        return proto;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + proto;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), proto);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPProtocolCriterion) {
            IPProtocolCriterion that = (IPProtocolCriterion) obj;
            return Objects.equals(proto, that.proto);
        }
        return false;
    }
}
