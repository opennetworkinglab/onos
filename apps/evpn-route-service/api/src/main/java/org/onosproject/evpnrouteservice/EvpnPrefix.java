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

package org.onosproject.evpnrouteservice;

import java.util.Objects;

import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a evpn prefix.
 */
public final class EvpnPrefix {

    private final RouteDistinguisher rd;
    private final MacAddress macAddress;
    private final IpPrefix ipAddress;

    /**
     * Constructor to initialize the parameters.
     *
     * @param rd         route distinguisher
     * @param macAddress mac address
     * @param ipAddress  IP address
     */
    public EvpnPrefix(RouteDistinguisher rd, MacAddress macAddress,
                      IpPrefix ipAddress) {
        checkNotNull(rd);
        checkNotNull(macAddress);
        checkNotNull(ipAddress);
        this.rd = rd;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
    }

    /**
     * Creates the evpn prefix by given parameters.
     *
     * @param rd         route distinguisher
     * @param macAddress mac address
     * @param ipAddress  ip address
     * @return EvpnPrefix
     */
    public static EvpnPrefix evpnPrefix(RouteDistinguisher rd,
                                        MacAddress macAddress,
                                        IpPrefix ipAddress) {
        return new EvpnPrefix(rd, macAddress, ipAddress);
    }

    /**
     * Returns the route distinguisher.
     *
     * @return RouteDistinguisher
     */
    public RouteDistinguisher routeDistinguisher() {
        return rd;
    }

    /**
     * Returns the mac address.
     *
     * @return MacAddress
     */
    public MacAddress macAddress() {
        return macAddress;
    }

    /**
     * Returns the IP address.
     *
     * @return Ip4Address
     */
    public IpPrefix ipAddress() {
        return ipAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rd, macAddress, ipAddress);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnPrefix)) {
            return false;
        }

        EvpnPrefix that = (EvpnPrefix) other;

        return Objects.equals(this.macAddress(), that.macAddress())
                && Objects.equals(this.ipAddress, that.ipAddress)
                && Objects.equals(this.rd, that.rd);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("macAddress", this.macAddress())
                .add("ipAddress", this.ipAddress()).add("rd", this.rd)
                .toString();
    }
}
