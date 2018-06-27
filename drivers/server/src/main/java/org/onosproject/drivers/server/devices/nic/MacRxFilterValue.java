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

import org.onlab.packet.MacAddress;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A MAC Rx filter value.
 */
public class MacRxFilterValue extends RxFilterValue implements Comparable {

    private MacAddress mac;

    public MacRxFilterValue() {
        super();
        this.mac = null;
    }

    public MacRxFilterValue(MacAddress mac) {
        super();
        setValue(mac);
    }

    public MacRxFilterValue(MacRxFilterValue other) {
        super();
        setValue(other.value());
    }

    /**
     * Returns the value of this Rx filter.
     *
     * @return MAC value
     */
    public MacAddress value() {
        return this.mac;
    }

    /**
     * Sets the value of this Rx filter.
     *
     * @param mac MAC value
     */
    public void setValue(MacAddress mac) {
        checkNotNull(mac, "MAC address of Rx filter is NULL");
        this.mac = mac;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.mac);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof MacRxFilterValue))) {
            return false;
        }

        MacRxFilterValue other = (MacRxFilterValue) obj;

        return this.value().equals(other.value());
    }

    @Override
    public int compareTo(Object other) {
        if (this == other) {
            return 0;
        }

        if (other == null) {
            return -1;
        }

        if (other instanceof MacRxFilterValue) {
            MacRxFilterValue otherRxVal = (MacRxFilterValue) other;

            // Extract the digits out of the ID
            String thisMac  = this.toString();
            String otherMac = otherRxVal.toString();

            return thisMac.compareToIgnoreCase(otherMac);
        }

        return -1;
    }

    @Override
    public String toString() {
        return  this.value().toString();
    }

}
