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

/**
 * A flow rule-based Rx filter value.
 */
public final class FlowRxFilterValue extends RxFilterValue
        implements Comparable {

    private long cpuCoreId;
    private String flowRule;

    /**
     * Constructs a flow-based Rx filter.
     */
    public FlowRxFilterValue() {
        super();
        setValue(0);
        setRule("");
    }

    /**
     * Constructs a flow-based Rx filter with CPU core ID.
     *
     * @param cpuCoreId a CPU core ID when the flow ends up
     */
    public FlowRxFilterValue(long cpuCoreId) {
        super();
        setValue(cpuCoreId);
        setRule("");
    }

    /**
     * Constructs a flow-based Rx filter with CPU core ID
     * and an associated rule.
     *
     * @param cpuCoreId a CPU core ID
     * @param flowRule a flow rule as a string
     */
    public FlowRxFilterValue(long cpuCoreId, String flowRule) {
        super();
        setValue(cpuCoreId);
        setRule(flowRule);
    }

    /**
     * Constructs a flow-based Rx filter out of an existing one.
     *
     * @param other a source FlowRxFilterValue object
     */
    public FlowRxFilterValue(FlowRxFilterValue other) {
        super();
        setValue(other.value());
        setRule(other.rule());
    }

    /**
     * Returns the value of this Rx filter.
     *
     * @return Flow Rx filter value
     */
    public long value() {
        return this.cpuCoreId;
    }

    /**
     * Sets the value of this Rx filter.
     *
     * @param cpuCoreId a CPU core ID for this Rx filter
     */
    public void setValue(long cpuCoreId) {
        checkArgument(cpuCoreId >= 0,
            "NIC flow Rx filter has invalid CPU core ID");
        this.cpuCoreId = cpuCoreId;
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
        checkNotNull(flowRule,
            "NIC flow Rx filter rule is NULL");
        this.flowRule = flowRule;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.cpuCoreId, this.flowRule);
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

        return (this.value() == other.value()) &&
                this.rule().equals(other.rule());
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

            long thisCoreId  = this.value();
            long otherCoreId = otherRxVal.value();

            if (thisCoreId > otherCoreId) {
                return 1;
            } else if (thisCoreId < otherCoreId) {
                return -1;
            } else {
                return 0;
            }
        }

        return -1;
    }

    @Override
    public String toString() {
        return Long.toString(this.value());
    }

}
