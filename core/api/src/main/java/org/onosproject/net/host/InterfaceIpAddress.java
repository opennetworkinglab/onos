/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.host;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a single IP address information on an interface.
 *
 * TODO:
 *  - Add computation for the default broadcast address if it is not
 *    specified
 *  - Add explicit checks that each IP address or prefix belong to the
 *    same IP version: IPv4/IPv6.
 *  - Inside the copy constructor we should use copy constructors for each
 *    field
 */
public class InterfaceIpAddress {
    private final IpAddress ipAddress;
    private final IpPrefix subnetAddress;
    private final IpAddress broadcastAddress;
    private final IpAddress peerAddress;

    /**
     * Copy constructor.
     *
     * @param other the object to copy from
     */
    public InterfaceIpAddress(InterfaceIpAddress other) {
        // TODO: we should use copy constructors for each field
        this.ipAddress = other.ipAddress;
        this.subnetAddress = other.subnetAddress;
        this.broadcastAddress = other.broadcastAddress;
        this.peerAddress = other.peerAddress;
    }

    /**
     * Constructor for a given IP address and a subnet address.
     *
     * @param ipAddress the IP address
     * @param subnetAddress the IP subnet address
     */
    public InterfaceIpAddress(IpAddress ipAddress, IpPrefix subnetAddress) {
        checkArgument(checkNotNull(ipAddress).version() == checkNotNull(subnetAddress).version(),
            "IP and subnet version mismatch");
        this.ipAddress = ipAddress;
        this.subnetAddress = subnetAddress;
        this.broadcastAddress = computeBroadcastAddress(ipAddress, subnetAddress);
        this.peerAddress = null;
    }

    /**
     * Constructor for a given IP address and a subnet address.
     *
     * @param ipAddress the IP address
     * @param subnetAddress the IP subnet address
     * @param broadcastAddress the IP broadcast address. It can be used
     * to specify non-default broadcast address
     */
    public InterfaceIpAddress(IpAddress ipAddress, IpPrefix subnetAddress,
                              IpAddress broadcastAddress) {
        checkArgument(checkNotNull(ipAddress).version() == checkNotNull(subnetAddress).version(),
            "IP and subnet version mismatch");
        this.ipAddress = ipAddress;
        this.subnetAddress = subnetAddress;
        this.broadcastAddress = broadcastAddress;
        this.peerAddress = null;
    }

    /**
     * Constructor for a given IP address and a subnet address.
     *
     * @param ipAddress the IP address
     * @param subnetAddress the IP subnet address
     * @param broadcastAddress the IP broadcast address. It can be used
     * to specify non-default broadcast address. It should be null for
     * point-to-point interfaces with a peer address
     * @param peerAddress the peer IP address for point-to-point interfaces
     */
    public InterfaceIpAddress(IpAddress ipAddress, IpPrefix subnetAddress,
                              IpAddress broadcastAddress,
                              IpAddress peerAddress) {
        checkArgument(checkNotNull(ipAddress).version() == checkNotNull(subnetAddress).version(),
            "IP and subnet version mismatch");
        this.ipAddress = ipAddress;
        this.subnetAddress = subnetAddress;
        this.broadcastAddress = broadcastAddress;
        this.peerAddress = peerAddress;
    }

    /**
     * Gets the IP address.
     *
     * @return the IP address
     */
    public IpAddress ipAddress() {
        return ipAddress;
    }

    /**
     * Gets the IP subnet address.
     *
     * @return the IP subnet address
     */
    public IpPrefix subnetAddress() {
        return subnetAddress;
    }

    /**
     * Gets the subnet IP broadcast address.
     *
     * @return the subnet IP broadcast address
     */
    public IpAddress broadcastAddress() {
        return broadcastAddress;
    }

    /**
     * Gets the IP point-to-point interface peer address.
     *
     * @return the IP point-to-point interface peer address
     */
    public IpAddress peerAddress() {
        return peerAddress;
    }

    /**
     * Converts a CIDR string literal to an interface IP address.
     * E.g. 10.0.0.1/24
     *
     * @param value an IP address value in string form
     * @return an interface IP address
     * @throws IllegalArgumentException if the argument is invalid
     */
    public static InterfaceIpAddress valueOf(String value) {
        String[] splits = value.split("/");
        checkArgument(splits.length == 2, "Invalid IP address and prefix length format");

        // NOTE: IpPrefix will mask-out the bits after the prefix length.
        IpPrefix subnet = IpPrefix.valueOf(value);
        IpAddress addr = IpAddress.valueOf(splits[0]);
        return new InterfaceIpAddress(addr, subnet);
    }

    /**
     * Compute the IP broadcast address.
     *
     * @param ipAddress base IP address
     * @param subnetAddress subnet specification
     * @return the IP broadcast address
     */
    public static IpAddress computeBroadcastAddress(IpAddress ipAddress, IpPrefix subnetAddress) {
        if (ipAddress.isIp6()) {
            return null;
        } else {
            IpAddress maskedIP = IpAddress.makeMaskedAddress(ipAddress, subnetAddress.prefixLength());
            int ipB = maskedIP.getIp4Address().toInt() | ((1 << (32 - subnetAddress.prefixLength())) - 1);
            return IpAddress.valueOf(ipB);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof InterfaceIpAddress)) {
            return false;
        }
        InterfaceIpAddress otherAddr = (InterfaceIpAddress) other;

        return Objects.equals(this.ipAddress, otherAddr.ipAddress)
            && Objects.equals(this.subnetAddress, otherAddr.subnetAddress)
            && Objects.equals(this.broadcastAddress,
                              otherAddr.broadcastAddress)
            && Objects.equals(this.peerAddress, otherAddr.peerAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, subnetAddress, broadcastAddress,
                            peerAddress);
    }

    @Override
    public String toString() {
        return ipAddress.toString() + "/" + subnetAddress.prefixLength();
    }
}
