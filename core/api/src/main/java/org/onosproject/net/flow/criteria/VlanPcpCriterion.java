/*
 * Copyright 2015 Open Networking Laboratory
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

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of VLAN priority criterion (3 bits).
 */
public final class VlanPcpCriterion implements Criterion {
    private static final byte MASK = 0x7;
    private final byte vlanPcp;             // VLAN pcp value: 3 bits

    /**
     * Constructor.
     *
     * @param vlanPcp the VLAN priority to match (3 bits)
     */
    VlanPcpCriterion(byte vlanPcp) {
        this.vlanPcp = (byte) (vlanPcp & MASK);
    }

    @Override
    public Type type() {
        return Type.VLAN_PCP;
    }

    /**
     * Gets the VLAN priority to match.
     *
     * @return the VLAN priority to match (3 bits)
     */
    public byte priority() {
        return vlanPcp;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
            .add("priority", Long.toHexString(vlanPcp)).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), vlanPcp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof VlanPcpCriterion) {
            VlanPcpCriterion that = (VlanPcpCriterion) obj;
            return Objects.equals(vlanPcp, that.vlanPcp) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
