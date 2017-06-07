/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.dhcprelay.store;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import org.onlab.packet.DHCP;
import org.onlab.packet.DHCP6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class to record DHCP from DHCP relay application.
 */
public class DhcpRecord {
    private final Set<HostLocation> locations;
    private final MacAddress macAddress;
    private final VlanId vlanId;
    private MacAddress nextHop;

    private Ip4Address ip4Address;
    private DHCP.MsgType ip4Status;

    private Ip6Address ip6Address;
    private DHCP6.MsgType ip6Status;

    private long lastSeen;
    private boolean directlyConnected;

    /**
     * Creates a DHCP record for a host (mac + vlan).
     *
     * @param hostId the host id for the host
     */
    public DhcpRecord(HostId hostId) {
        checkNotNull(hostId, "Host id can't be null");

        this.locations = Sets.newHashSet();
        this.macAddress = hostId.mac();
        this.vlanId = hostId.vlanId();
        this.lastSeen = System.currentTimeMillis();
        this.directlyConnected = false;
    }

    /**
     * Gets host locations.
     *
     * @return the locations of host
     */
    public Set<HostLocation> locations() {
        return locations;
    }

    /**
     * Adds a location to record.
     *
     * @param location the location
     * @return the DHCP record
     */
    public DhcpRecord addLocation(HostLocation location) {
        locations.add(location);
        return this;
    }

    /**
     * Removes a location from record.
     *
     * @param location the location
     * @return the DHCP record
     */
    public DhcpRecord removeLocation(HostLocation location) {
        locations.remove(location);
        return this;
    }

    /**
     * Gets host mac address of this record.
     *
     * @return the host mac address
     */
    public MacAddress macAddress() {
        return macAddress;
    }

    /**
     * Gets host vlan id of this record.
     *
     * @return the host id.
     */
    public VlanId vlanId() {
        return vlanId;
    }

    /**
     * Gets IPv4 address which assigned to the host.
     *
     * @return the IP address assigned to the host
     */
    public Optional<Ip4Address> ip4Address() {
        return Optional.ofNullable(ip4Address);
    }

    /**
     * Sets IPv4 address.
     *
     * @param ip4Address the IPv4 address
     * @return the DHCP record
     */
    public DhcpRecord ip4Address(Ip4Address ip4Address) {
        this.ip4Address = ip4Address;
        return this;
    }

    /**
     * Gets IPv6 address which assigned to the host.
     *
     * @return the IP address assigned to the host
     */
    public Optional<Ip6Address> ip6Address() {
        return Optional.ofNullable(ip6Address);
    }

    /**
     * Sets IPv6 address.
     *
     * @param ip6Address the IPv6 address
     * @return the DHCP record
     */
    public DhcpRecord ip6Address(Ip6Address ip6Address) {
        this.ip6Address = ip6Address;
        return this;
    }

    /**
     * Gets the latest time this record updated.
     *
     * @return the last time host send or receive DHCP packet
     */
    public long lastSeen() {
        return lastSeen;
    }

    /**
     * Updates the update time of this record.
     *
     * @return the DHCP record
     */
    public DhcpRecord updateLastSeen() {
        lastSeen = System.currentTimeMillis();
        return this;
    }

    /**
     * Indicated that the host is direct connected to the network or not.
     *
     * @return true if the host is directly connected to the network; false otherwise
     */
    public boolean directlyConnected() {
        return directlyConnected;
    }

    /**
     * Sets the flag which indicated that the host is directly connected to the
     * network.
     *
     * @param directlyConnected the flag to set
     * @return the DHCP record
     */
    public DhcpRecord setDirectlyConnected(boolean directlyConnected) {
        this.directlyConnected = directlyConnected;
        return this;
    }

    /**
     * Gets the DHCPv4 status of this record.
     *
     * @return the DHCPv4 status; empty if not exists
     */
    public Optional<DHCP.MsgType> ip4Status() {
        return Optional.ofNullable(ip4Status);
    }

    /**
     * Sets status of DHCPv4.
     *
     * @param ip4Status the status
     * @return the DHCP record
     */
    public DhcpRecord ip4Status(DHCP.MsgType ip4Status) {
        this.ip4Status = ip4Status;
        return this;
    }

    /**
     * Gets the DHCPv6 status of this record.
     *
     * @return the DHCPv6 status; empty if not exists
     */
    public Optional<DHCP6.MsgType> ip6Status() {
        return Optional.ofNullable(ip6Status);
    }

    /**
     * Sets status of DHCPv6.
     *
     * @param ip6Status the DHCPv6 status
     * @return the DHCP record
     */
    public DhcpRecord ip6Status(DHCP6.MsgType ip6Status) {
        this.ip6Status = ip6Status;
        return this;
    }

    /**
     * Gets nextHop mac address.
     *
     * @return the IPv4 nextHop mac address; empty if not exists
     */
    public Optional<MacAddress> nextHop() {
        return Optional.ofNullable(nextHop);
    }

    /**
     * Sets nextHop mac address.
     *
     * @param nextHop the IPv4 nextHop mac address
     * @return the DHCP record
     */
    public DhcpRecord nextHop(MacAddress nextHop) {
        this.nextHop = nextHop;
        return this;
    }

    /**
     * Clone this DHCP record.
     *
     * @return the DHCP record which cloned
     */
    public DhcpRecord clone() {
        DhcpRecord newRecord = new DhcpRecord(HostId.hostId(macAddress, vlanId));
        locations.forEach(newRecord::addLocation);
        newRecord.directlyConnected = directlyConnected;
        newRecord.nextHop = nextHop;
        newRecord.ip4Address = ip4Address;
        newRecord.ip4Status = ip4Status;
        newRecord.ip6Address = ip6Address;
        newRecord.ip6Status = ip6Status;
        newRecord.lastSeen = lastSeen;
        return newRecord;
    }

    @Override
    public int hashCode() {
        return Objects.hash(locations, macAddress, vlanId, ip4Address, ip4Status,
                            nextHop, ip6Address, ip6Status, lastSeen);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DhcpRecord)) {
            return false;
        }
        DhcpRecord that = (DhcpRecord) obj;
        return Objects.equals(locations, that.locations) &&
                Objects.equals(macAddress, that.macAddress) &&
                Objects.equals(vlanId, that.vlanId) &&
                Objects.equals(ip4Address, that.ip4Address) &&
                Objects.equals(ip4Status, that.ip4Status) &&
                Objects.equals(nextHop, that.nextHop) &&
                Objects.equals(ip6Address, that.ip6Address) &&
                Objects.equals(ip6Status, that.ip6Status) &&
                Objects.equals(lastSeen, that.lastSeen) &&
                Objects.equals(directlyConnected, that.directlyConnected);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("locations", locations)
                .add("macAddress", macAddress)
                .add("vlanId", vlanId)
                .add("ip4Address", ip4Address)
                .add("ip4State", ip4Status)
                .add("nextHop", nextHop)
                .add("ip6Address", ip6Address)
                .add("ip6State", ip6Status)
                .add("lastSeen", lastSeen)
                .add("directlyConnected", directlyConnected)
                .toString();
    }
}
