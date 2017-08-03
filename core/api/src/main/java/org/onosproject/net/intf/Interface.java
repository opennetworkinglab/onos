/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.intf;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.host.InterfaceIpAddress;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An Interface maps network configuration information (such as addresses and
 * vlans) to a port in the network.
 */

public class Interface {
    public static final String NO_INTERFACE_NAME = "";

    private final String name;
    private final ConnectPoint connectPoint;
    private final List<InterfaceIpAddress> ipAddresses;
    private final MacAddress macAddress;
    // TODO: Deprecate this due to ambiguity
    private final VlanId vlan;
    private final VlanId vlanUntagged;
    private final Set<VlanId> vlanTagged;
    private final VlanId vlanNative;

    /**
     * Creates new Interface with the provided configuration.
     *
     * @param name name of the interface
     * @param connectPoint the connect point this interface maps to
     * @param ipAddresses list of IP addresses
     * @param macAddress MAC address
     * @param vlan VLAN ID
     */
    public Interface(String name, ConnectPoint connectPoint,
                     List<InterfaceIpAddress> ipAddresses,
                     MacAddress macAddress, VlanId vlan) {
        this(name, connectPoint, ipAddresses, macAddress, vlan, null, null, null);
    }

    /**
     * Creates new Interface with the provided configuration.
     *
     * @param name name of the interface
     * @param connectPoint the connect point this interface maps to
     * @param ipAddresses list of IP addresses
     * @param macAddress MAC address
     * @param vlan VLAN ID
     * @param vlanUntagged untagged VLAN.
     *                     Cannot be used with vlanTagged or vlanNative.
     * @param vlanTagged   tagged VLANs.
     *                     Cannot be used with vlanUntagged.
     * @param vlanNative   native vLAN. Optional.
     *                     Can only be used when vlanTagged is specified. Cannot be used with vlanUntagged.
     */
    public Interface(String name, ConnectPoint connectPoint,
                     List<InterfaceIpAddress> ipAddresses,
                     MacAddress macAddress, VlanId vlan,
                     VlanId vlanUntagged, Set<VlanId> vlanTagged, VlanId vlanNative) {
        this.name = name == null ? NO_INTERFACE_NAME : name;
        this.connectPoint = checkNotNull(connectPoint);
        this.ipAddresses = ipAddresses == null ? Lists.newArrayList() : ipAddresses;
        this.macAddress = macAddress == null ? MacAddress.NONE : macAddress;
        this.vlan = vlan == null ? VlanId.NONE : vlan;
        this.vlanUntagged = vlanUntagged == null ? VlanId.NONE : vlanUntagged;
        this.vlanTagged = vlanTagged == null ? ImmutableSet.of() : ImmutableSet.copyOf(vlanTagged);
        this.vlanNative = vlanNative == null ? VlanId.NONE : vlanNative;
    }

    /**
     * Retrieves the name of the interface.
     *
     * @return name
     */
    public String name() {
        return name;
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
     * Retrieves a list of IP addresses that are assigned to the interface in
     * the order that they were configured.
     *
     * @return list of IP addresses
     */
    public List<InterfaceIpAddress> ipAddressesList() {
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

    /**
     * Retrieves the VLAN ID that is assigned to untagged packets.
     *
     * @return the VLAN ID
     */
    public VlanId vlanUntagged() {
        return vlanUntagged;
    }

    /**
     * Retrieves the set of VLAN IDs that are allowed on this interface.
     *
     * @return the VLAN ID
     */
    public Set<VlanId> vlanTagged() {
        return vlanTagged;
    }

    /**
     * Retrieves the VLAN ID that is assigned to untagged packets on this
     * tagged interface.
     *
     * @return the VLAN ID
     */
    public VlanId vlanNative() {
        return vlanNative;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Interface)) {
            return false;
        }

        Interface otherInterface = (Interface) other;

        return Objects.equals(name, otherInterface.name) &&
                Objects.equals(connectPoint, otherInterface.connectPoint) &&
                Objects.equals(ipAddresses, otherInterface.ipAddresses) &&
                Objects.equals(macAddress, otherInterface.macAddress) &&
                Objects.equals(vlan, otherInterface.vlan) &&
                Objects.equals(vlanUntagged, otherInterface.vlanUntagged) &&
                Objects.equals(vlanTagged, otherInterface.vlanTagged) &&
                Objects.equals(vlanNative, otherInterface.vlanNative);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectPoint, name, ipAddresses, macAddress, vlan,
                vlanUntagged, vlanTagged, vlanNative);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .add("connectPoint", connectPoint)
                .add("ipAddresses", ipAddresses)
                .add("macAddress", macAddress)
                .add("vlan", vlan)
                .add("vlanUntagged", vlanUntagged)
                .add("vlanTagged", vlanTagged)
                .add("vlanNative", vlanNative)
                .toString();
    }
}
