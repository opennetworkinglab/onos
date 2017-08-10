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
 * Represents a evpn instance prefix.
 */
public final class EvpnInstancePrefix {

    private final MacAddress macAddress;
    private final IpPrefix ipPrefix;

    /**
     * Constructor to initialize the parameters.
     *
     * @param macAddress Mac address
     * @param ipPrefix   IP address
     */
    private EvpnInstancePrefix(MacAddress macAddress,
                               IpPrefix ipPrefix) {
        checkNotNull(macAddress);
        this.macAddress = macAddress;
        this.ipPrefix = ipPrefix;
    }

    /**
     * Creates the instance of EvpnInstancePrefix.
     *
     * @param macAddress Mac address
     * @param ipPrefix   IP address
     * @return Evpn instance prefix
     */
    public static EvpnInstancePrefix evpnPrefix(MacAddress macAddress,
                                                IpPrefix ipPrefix) {
        return new EvpnInstancePrefix(macAddress, ipPrefix);
    }

    /**
     * Returns the MAC of the route.
     *
     * @return MAC address
     */
    public MacAddress macAddress() {
        return macAddress;
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return IP prefix
     */
    public IpPrefix ipPrefix() {
        return ipPrefix;
    }

    @Override
    public int hashCode() {
        return Objects.hash(macAddress);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnInstancePrefix)) {
            return false;
        }

        EvpnInstancePrefix that = (EvpnInstancePrefix) other;

        return Objects.equals(this.macAddress, that.macAddress)
                && Objects.equals(this.ipPrefix, that.ipPrefix);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("macAddress", this.macAddress)
                .add("ipAddress", this.ipPrefix).toString();
    }
}
