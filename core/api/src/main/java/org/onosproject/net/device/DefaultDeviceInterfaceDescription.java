/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.device;

import com.google.common.collect.Lists;
import org.onlab.packet.VlanId;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Basic implementation of description of a legacy device interface.
 */
public class DefaultDeviceInterfaceDescription implements
        DeviceInterfaceDescription {
    private String name;
    private Mode mode;
    private List<VlanId> vlans;
    private boolean isRateLimited;
    private short rateLimit;

    /**
     * Device interface description object constructor.
     *
     * @param name the name of the interface
     * @param mode the operation mode of the interface
     * @param vlans the vlan-id of the interface (none, one or multiple can be
     *              specified based on if mode is normal, access or trunk).
     * @param isRateLimited bandwidth limit application indication
     * @param rateLimit percentage of bandwidth limit
     */
    public DefaultDeviceInterfaceDescription(String name,
                                             Mode mode,
                                             List<VlanId> vlans,
                                             boolean isRateLimited,
                                             short rateLimit) {
        this.name = name;
        this.mode = (mode != null ? mode : Mode.NORMAL);
        this.vlans = (vlans != null ? vlans : Lists.newArrayList());
        this.isRateLimited = isRateLimited;
        this.rateLimit = rateLimit;
    }

    /**
     * Returns the name of the interface.
     *
     * @return name of the interface
     */
    @Override
    public String name() {
        return this.name;
    }

    /**
     * Returns the operation mode of the interface.
     *
     * @return operation mode of the interface
     */
    @Override
    public Mode mode() {
        return this.mode;
    }

    /**
     * Returns the VLAN-IDs configured for the interface. No VLAN-ID should be
     * returned for NORMAL mode, 1 VLAN-ID for access mode and 1 or more
     * VLAN-IDs for trunking mode.
     *
     * @return VLAN-ID(s) configured for the interface.
     */
    @Override
    public List<VlanId> vlans() {
        return vlans;
    }

    /**
     * Indicates whether a rate limit has been set on the interface.
     *
     * @return indication whether interface is rate limited or not
     */
    @Override
    public boolean isRateLimited() {
        return isRateLimited;
    }

    /**
     * Returns the rate limit set on the interface bandwidth.
     *
     * @return the rate limit set on the interface bandwidth
     */
    @Override
    public short rateLimit() {
        return rateLimit;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DefaultDeviceInterfaceDescription)) {
            return false;
        }

        DefaultDeviceInterfaceDescription otherInterface =
                (DefaultDeviceInterfaceDescription) other;

        return Objects.equals(name, otherInterface.name) &&
                Objects.equals(mode, otherInterface.mode) &&
                Objects.equals(vlans, otherInterface.vlans) &&
                Objects.equals(isRateLimited, otherInterface.isRateLimited) &&
                Objects.equals(rateLimit, otherInterface.rateLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, mode, vlans, isRateLimited, rateLimit);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .add("Device name", name())
                .add("Device mode", mode())
                .add("VLAN IDs", vlans())
                .add("Rate limited", isRateLimited())
                .add("Rate limit", rateLimit())
                .toString();
    }

}
