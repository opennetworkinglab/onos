/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.incubator.net.intf;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.host.InterfaceIpAddress;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An Interface maps network configuration information (such as addresses and
 * vlans) to a port in the network.
 */
@Beta
public class Interface {
    private final ConnectPoint connectPoint;
    private final Set<InterfaceIpAddress> ipAddresses;
    private final MacAddress macAddress;
    private final VlanId vlan;

    /**
     * Creates new Interface with the provided configuration.
     *
     * @param connectPoint the connect point this interface maps to
     * @param ipAddresses Set of IP addresses
     * @param macAddress MAC address
     * @param vlan VLAN ID
     */
    public Interface(ConnectPoint connectPoint,
                     Set<InterfaceIpAddress> ipAddresses,
                     MacAddress macAddress, VlanId vlan) {
        this.connectPoint = checkNotNull(connectPoint);
        this.ipAddresses = Sets.newHashSet(checkNotNull(ipAddresses));
        this.macAddress = checkNotNull(macAddress);
        this.vlan = checkNotNull(vlan);
    }

    /**
     * Retrieves the connection point that this interface maps to.
     *
     * @return the connection point
     */
    public ConnectPoint connectPoint() {
        return connectPoint;
    }

    /**
     * Retrieves the set of IP addresses that are assigned to the interface.
     *
     * @return the set of interface IP addresses
     */
    public Set<InterfaceIpAddress> ipAddresses() {
        return ipAddresses;
    }

    /**
     * Retrieves the MAC address that is assigned to the interface.
     *
     * @return the MAC address
     */
    public MacAddress mac() {
        return macAddress;
    }

    /**
     * Retrieves the VLAN ID that is assigned to the interface.
     *
     * @return the VLAN ID
     */
    public VlanId vlan() {
        return vlan;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Interface)) {
            return false;
        }

        Interface otherInterface = (Interface) other;

        return Objects.equals(connectPoint, otherInterface.connectPoint) &&
                Objects.equals(ipAddresses, otherInterface.ipAddresses) &&
                Objects.equals(macAddress, otherInterface.macAddress) &&
                Objects.equals(vlan, otherInterface.vlan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectPoint, ipAddresses, macAddress, vlan);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("connectPoint", connectPoint)
                .add("ipAddresses", ipAddresses)
                .add("macAddress", macAddress)
                .add("vlan", vlan)
                .toString();
    }
}
