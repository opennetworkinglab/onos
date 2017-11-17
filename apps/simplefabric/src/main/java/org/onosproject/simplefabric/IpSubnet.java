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
package org.onosproject.simplefabric;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.EncapsulationType;

import java.util.Objects;

/**
 * Configuration details for an ip subnet entry.
 */
public class IpSubnet {
    private final IpPrefix ipPrefix;
    private final IpAddress gatewayIp;
    private final MacAddress gatewayMac;
    private EncapsulationType encapsulation;
    private final String l2NetworkName;

    /**
     * Creates a new ip subnet entry.
     *
     * @param ipPrefix  an ip subnet
     * @param gatewayIp IP of the virtual gateway
     * @param gatewayMac MacAddress of the virtual gateway
     * @param encapsulation EnacaptulatioType for routes related to this subnet
     * @param l2NetworkName Name of L2 Network this subnet is bound
     */
    public IpSubnet(IpPrefix ipPrefix, IpAddress gatewayIp, MacAddress gatewayMac,
                    EncapsulationType encapsulation, String l2NetworkName) {
        this.ipPrefix = ipPrefix;
        this.gatewayIp = gatewayIp;
        this.gatewayMac = gatewayMac;
        this.encapsulation = EncapsulationType.NONE;
        this.l2NetworkName = l2NetworkName;
    }

    /**
     * Gets the ip subnet of the ip subnet entry.
     *
     * @return the ip subnet
     */
    public IpPrefix ipPrefix() {
        return ipPrefix;
    }

    /**
     * Gets the virtual gateway IP address of the ip subnet entry.
     *
     * @return the virtual gateway IP address
     */
    public IpAddress gatewayIp() {
        return gatewayIp;
    }

    /**
     * Gets the virtual gateway Mac address of the ip subnet entry.
     *
     * @return the virtuai gateway Mac address
     */
    public MacAddress gatewayMac() {
        return gatewayMac;
    }

    /**
     * Gets the encapsulation type of ip subnet entry.
     *
     * @return the encapsulation type
     */
    public EncapsulationType encapsulation() {
        return encapsulation;
    }

    /**
     * Gets the name of L2 Network this subnet is bound.
     *
     * @return the l2Network name this subnet is allocated
     */
    public String l2NetworkName() {
        return l2NetworkName;
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
        return Objects.hash(ipPrefix, gatewayIp, gatewayMac, encapsulation, l2NetworkName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof IpSubnet)) {
            return false;
        }
        IpSubnet that = (IpSubnet) obj;
        return Objects.equals(this.ipPrefix, that.ipPrefix)
               && Objects.equals(this.gatewayIp, that.gatewayIp)
               && Objects.equals(this.gatewayMac, that.gatewayMac)
               && Objects.equals(this.encapsulation, that.encapsulation)
               && Objects.equals(this.l2NetworkName, that.l2NetworkName);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ipPrefix", ipPrefix)
                .add("gatewayIp", gatewayIp)
                .add("gatewayMac", gatewayMac)
                .add("encapsulation", encapsulation)
                .add("l2NetworkName", l2NetworkName)
                .toString();
    }
}
