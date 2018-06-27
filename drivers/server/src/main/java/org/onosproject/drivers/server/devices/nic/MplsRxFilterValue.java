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

/**
 * An MPLS Rx filter value.
 */
public class MplsRxFilterValue extends RxFilterValue {

    private MplsLabel mplsLabel;

    public MplsRxFilterValue() {
        super();
        this.mplsLabel = null;
    }

    public MplsRxFilterValue(MplsLabel mplsLabel) {
        super();
        setValue(mplsLabel);
    }

    public MplsRxFilterValue(MplsRxFilterValue other) {
        super();
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
        checkNotNull(mplsLabel, "MPLS label of Rx filter is NULL");
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
        return  this.value().toString();
    }

}
