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

import org.onlab.packet.VlanId;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of VLAN ID criterion.
 */
public final class VlanIdCriterion implements Criterion {
    private final VlanId vlanId;
    private final Type type;

    /**
     * Constructor.
     *
     * @param vlanId the VLAN ID to match
     */
    VlanIdCriterion(VlanId vlanId) {
        this.vlanId = vlanId;
        this.type = Type.VLAN_VID;
    }

    /**
     * Constructs a vlan criterion with a specific type.
     *
     * @param vlanId a vlan id to match
     * @param type a criterion type (only INNER_VLAN_VID and VLAN_ID are supported)
     */
    VlanIdCriterion(VlanId vlanId, Type type) {
        checkArgument(
                type == Type.INNER_VLAN_VID || type == Type.VLAN_VID,
                "Type can only be inner vlan or vlan");
        this.vlanId = vlanId;
        this.type = type;
    }

    @Override
    public Type type() {
        return type;
    }

    /**
     * Gets the VLAN ID to match.
     *
     * @return the VLAN ID to match
     */
    public VlanId vlanId() {
        return vlanId;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + vlanId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), vlanId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof VlanIdCriterion) {
            VlanIdCriterion that = (VlanIdCriterion) obj;
            return Objects.equals(vlanId, that.vlanId) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
