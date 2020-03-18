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

import org.onlab.packet.MplsLabel;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_FILTER_MPLS_NULL;

/**
 * An MPLS Rx filter value.
 */
public final class MplsRxFilterValue extends RxFilterValue {

    private MplsLabel mplsLabel;

    /**
     * Constructs an MPLS-based Rx filter.
     *
     * @param cpuId CPU ID of the server this tag will lead to
     */
    public MplsRxFilterValue(int cpuId) {
        super(cpuId);
        this.mplsLabel = null;
    }

    /**
     * Constructs an MPLS-based Rx filter with specific label.
     *
     * @param mplsLabel an MPLS label to use as a filter
     * @param cpuId CPU ID of the server this tag will lead to
     */
    public MplsRxFilterValue(MplsLabel mplsLabel, int cpuId) {
        super(cpuId);
        setValue(mplsLabel);
    }

    /**
     * Constructs an MPLS-based Rx filter out of an existing one.
     *
     * @param other a source MplsRxFilterValue object
     */
    public MplsRxFilterValue(MplsRxFilterValue other) {
        super(other.cpuId);
        setValue(other.value());
    }

    /**
     * Returns the value of this Rx filter.
     *
     * @return MPLS label value
     */
    public MplsLabel value() {
        return this.mplsLabel;
    }

    /**
     * Sets the value of this Rx filter.
     *
     * @param mplsLabel MPLS label value
     */
    public void setValue(MplsLabel mplsLabel) {
        checkNotNull(mplsLabel, MSG_NIC_FLOW_FILTER_MPLS_NULL);
        this.mplsLabel = mplsLabel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.mplsLabel);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof MplsRxFilterValue))) {
            return false;
        }

        MplsRxFilterValue other = (MplsRxFilterValue) obj;

        return this.value().equals(other.value());
    }

    @Override
    public String toString() {
        return this.value().toString();
    }

}
