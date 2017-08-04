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
package org.onosproject.reactive.routing;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import java.util.Objects;

/**
 * Configuration details for an IP prefix entry.
 */
public class LocalIpPrefixEntry {
    private final IpPrefix ipPrefix;
    private final IpPrefixType type;
    private final IpAddress gatewayIpAddress;

    /**
     * Specifies the type of local IP prefix.
     */
    public enum IpPrefixType {
        /**
         * Public IP prefixes should be exchanged by eBGP.
         */
        PUBLIC,
        /**
         * Private IP prefixes should be used only locally and not exchanged
         * by eBGP.
         */
        PRIVATE,
        /**
         * For IP prefixes in blacklist.
         */
        BLACK_LIST
    }

    /**
     * Creates a new IP prefix entry.
     *
     * @param ipPrefix         an IP prefix
     * @param type             an IP prefix type as an IpPrefixType
     * @param gatewayIpAddress IP of the gateway
     */
    public LocalIpPrefixEntry(IpPrefix ipPrefix,
                              IpPrefixType type,
                              IpAddress gatewayIpAddress) {
        this.ipPrefix = ipPrefix;
        this.type = type;
        this.gatewayIpAddress = gatewayIpAddress;
    }

    /**
     * Gets the IP prefix of the IP prefix entry.
     *
     * @return the IP prefix
     */
    public IpPrefix ipPrefix() {
        return ipPrefix;
    }

    /**
     * Gets the IP prefix type of the IP prefix entry.
     *
     * @return the IP prefix type
     */
    public IpPrefixType ipPrefixType() {
        return type;
    }

    /**
     * Gets the gateway IP address of the IP prefix entry.
     *
     * @return the gateway IP address
     */
    public IpAddress getGatewayIpAddress() {
        return gatewayIpAddress;
    }

    /**
     * Tests whether the IP version of this entry is IPv4.
     *
     * @return true if the IP version of this entry is IPv4, otherwise false.
     */
    public boolean isIp4() {
        return ipPrefix.isIp4();
    }

    /**
     * Tests whether the IP version of this entry is IPv6.
     *
     * @return true if the IP version of this entry is IPv6, otherwise false.
     */
    public boolean isIp6() {
        return ipPrefix.isIp6();
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipPrefix, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof LocalIpPrefixEntry)) {
            return false;
        }

        LocalIpPrefixEntry that = (LocalIpPrefixEntry) obj;
        return Objects.equals(this.ipPrefix, that.ipPrefix)
                && Objects.equals(this.type, that.type);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ipPrefix", ipPrefix)
                .add("ipPrefixType", type)
                .toString();
    }
}
