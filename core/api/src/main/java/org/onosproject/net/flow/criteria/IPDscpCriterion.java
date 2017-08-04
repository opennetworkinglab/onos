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
 * Implementation of IP DSCP (Differentiated Services Code Point)
 * criterion (6 bits).
 */
public final class IPDscpCriterion implements Criterion {
    private static final byte MASK = 0x3f;
    private final byte ipDscp;              // IP DSCP value: 6 bits

    /**
     * Constructor.
     *
     * @param ipDscp the IP DSCP value to match
     */
    IPDscpCriterion(byte ipDscp) {
        this.ipDscp = (byte) (ipDscp & MASK);
    }

    @Override
    public Type type() {
        return Type.IP_DSCP;
    }

    /**
     * Gets the IP DSCP value to match.
     *
     * @return the IP DSCP value to match
     */
    public byte ipDscp() {
        return ipDscp;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + Long.toHexString(ipDscp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), ipDscp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPDscpCriterion) {
            IPDscpCriterion that = (IPDscpCriterion) obj;
            return Objects.equals(ipDscp, that.ipDscp) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
