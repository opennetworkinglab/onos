/*
 * Copyright 2018-present Open Networking Foundation
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

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_FILTER_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_RULE_NULL;

/**
 * A flow rule-based Rx filter value.
 */
public final class FlowRxFilterValue extends RxFilterValue
        implements Comparable {

    private long value;
    private String flowRule;

    /**
     * Constructs a flow-based Rx filter.
     *
     * @param cpuId CPU ID of the server this tag will lead to
     */
    public FlowRxFilterValue(int cpuId) {
        super(cpuId);
        setValue(0);
        setRule("");
    }

    /**
     * Constructs a flow-based Rx filter with physical CPU core ID.
     *
     * @param value Flow tag
     * @param cpuId CPU ID of the server this tag will lead to
     */
    public FlowRxFilterValue(long value, int cpuId) {
        super(cpuId);
        setValue(value);
        setRule("");
    }

    /**
     * Constructs a flow-based Rx filter with CPU core ID
     * and an associated rule.
     *
     * @param value Flow tag
     * @param cpuId CPU ID of the server this tag will lead to
     * @param flowRule a flow rule as a string
     */
    public FlowRxFilterValue(long value, int cpuId, String flowRule) {
        super(cpuId);
        setValue(value);
        setRule(flowRule);
    }

    /**
     * Constructs a flow-based Rx filter out of an existing one.
     *
     * @param other a source FlowRxFilterValue object
     */
    public FlowRxFilterValue(FlowRxFilterValue other) {
        super(other.cpuId);
        setValue(other.value());
        setRule(other.rule());
    }

    /**
     * Returns the value of this Rx filter.
     *
     * @return Flow Rx filter value
     */
    public long value() {
        return this.value;
    }

    /**
     * Sets the value of this Rx filter.
     *
     * @param value a CPU core ID for this Rx filter
     */
    private void setValue(long value) {
        checkArgument(value >= 0, MSG_NIC_FLOW_FILTER_NEGATIVE);
        this.value = value;
    }

    /**
     * Returns the rule of this Rx filter.
     *
     * @return Flow Rx filter rule
     */
    public String rule() {
        return this.flowRule;
    }

    /**
     * Sets the rule of this Rx filter.
     *
     * @param flowRule Flow Rx filter rule as a string
     */
    public void setRule(String flowRule) {
        checkNotNull(flowRule, MSG_NIC_FLOW_RULE_NULL);
        this.flowRule = flowRule;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.flowRule, this.cpuId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof FlowRxFilterValue))) {
            return false;
        }

        FlowRxFilterValue other = (FlowRxFilterValue) obj;

        return this.value() == other.value() &&
                this.rule().equals(other.rule()) && ((RxFilterValue) this).equals(other);
    }

    @Override
    public int compareTo(Object other) {
        if (this == other) {
            return 0;
        }

        if (other == null) {
            return -1;
        }

        if (other instanceof FlowRxFilterValue) {
            FlowRxFilterValue otherRxVal = (FlowRxFilterValue) other;

            long thisCpuId  = this.value();
            long otherCpuId = otherRxVal.value();

            if (thisCpuId > otherCpuId) {
                return 1;
            } else if (thisCpuId < otherCpuId) {
                return -1;
            } else {
                return this.cpuId - otherRxVal.cpuId;
            }
        }

        return -1;
    }

    @Override
    public String toString() {
        return Long.toString(this.value());
    }

}
