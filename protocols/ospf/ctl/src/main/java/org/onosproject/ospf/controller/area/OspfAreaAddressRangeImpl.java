/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.ospf.controller.area;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfAreaAddressRange;

/**
 * Representation of an area address ranges.
 * Address ranges are used in order to aggregate routing information at area boundaries.
 * Each address range is specified by an [address,mask] pair and a status indication of
 * either advertise or do not advertise
 */
public class OspfAreaAddressRangeImpl implements OspfAreaAddressRange {

    private Ip4Address ipAddress;
    private String mask;
    private boolean advertise;

    /**
     * Gets the IP address.
     *
     * @return IP address
     */
    public Ip4Address ipAddress() {
        return ipAddress;
    }

    /**
     * Sets the IP address.
     *
     * @param ipAddress IP address
     */
    public void setIpAddress(Ip4Address ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Gets the network mask.
     *
     * @return network  mask
     */
    public String mask() {
        return mask;
    }

    /**
     * Sets the network mask.
     *
     * @param mask network mask value
     */
    public void setMask(String mask) {
        this.mask = mask;
    }

    /**
     * Gets the advertise value.
     *
     * @return advertise value
     */
    public boolean isAdvertise() {
        return advertise;
    }

    /**
     * Sets the advertise value.
     *
     * @param advertise advertise value
     */
    public void setAdvertise(boolean advertise) {
        this.advertise = advertise;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof OspfAreaAddressRangeImpl)) {
            return false;
        }
        OspfAreaAddressRangeImpl otherAreaAddressRange = (OspfAreaAddressRangeImpl) other;
        return Objects.equal(ipAddress, otherAreaAddressRange.ipAddress) &&
                Objects.equal(mask, otherAreaAddressRange.mask) &&
                Objects.equal(advertise, otherAreaAddressRange.advertise);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ipAddress, mask, advertise);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("ipAddress", ipAddress)
                .add("mask", mask)
                .add("advertise", advertise)
                .toString();
    }
}
