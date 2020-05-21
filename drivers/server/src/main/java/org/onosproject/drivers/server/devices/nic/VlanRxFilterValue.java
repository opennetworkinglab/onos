/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.server.devices.nic;

import org.onlab.packet.VlanId;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_FILTER_VLAN_NULL;

/**
 * A VLAN Rx filter value.
 */
public class VlanRxFilterValue extends RxFilterValue {

    private VlanId vlanId;

    /**
     * Constructs a VLAN-based Rx filter.
     * @param cpuId CPU id of the server this tag will lead to
     */
    public VlanRxFilterValue(int cpuId) {
        super(cpuId);
        this.vlanId = VlanId.NONE;
    }

    /**
     * Constructs a VLAN-based Rx filter with specific ID.
     *
     * @param vlanId a VLAN ID to use as a filter
     * @param cpuId CPU id of the server this tag will lead to
     */
    public VlanRxFilterValue(VlanId vlanId, int cpuId) {
        super(cpuId);
        setValue(vlanId);
    }

    /**
     * Constructs a VLAN-based Rx filter out of an existing one.
     *
     * @param other a source VlanRxFilterValue object
     */
    public VlanRxFilterValue(VlanRxFilterValue other) {
        super(other.cpuId);
        setValue(other.value());
    }

    /**
     * Returns the value of this Rx filter.
     *
     * @return VLAN ID value
     */
    public VlanId value() {
        return this.vlanId;
    }

    /**
     * Sets the value of this Rx filter.
     *
     * @param vlanId VLAN ID value
     */
    public void setValue(VlanId vlanId) {
        checkNotNull(vlanId, MSG_NIC_FLOW_FILTER_VLAN_NULL);
        this.vlanId = vlanId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.vlanId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof VlanRxFilterValue))) {
            return false;
        }

        VlanRxFilterValue other = (VlanRxFilterValue) obj;

        return this.value().equals(other.value());
    }

    @Override
    public String toString() {
        return this.value().toString();
    }

}
