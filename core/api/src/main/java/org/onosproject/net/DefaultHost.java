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
package org.onosproject.net;

import org.onlab.packet.EthType;
import org.onosproject.net.provider.ProviderId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * A basic implementation of a Host.
 */
public class DefaultHost extends AbstractElement implements Host {

    private final MacAddress mac;
    private final VlanId vlan;
    private final Set<HostLocation> locations;
    private final Set<HostLocation> auxLocations;
    private final Set<IpAddress> ips;
    private final VlanId innerVlan;
    private final EthType tpid;
    private final boolean configured;
    private final boolean suspended;

    // TODO consider moving this constructor to a builder pattern.
    /**
     * Creates an end-station host using the supplied information.
     *
     * @param providerId provider identity
     * @param id         host identifier
     * @param mac        host MAC address
     * @param vlan       host VLAN identifier
     * @param location   host location
     * @param ips        host IP addresses
     * @param annotations optional key/value annotations
     */
    public DefaultHost(ProviderId providerId, HostId id, MacAddress mac,
                       VlanId vlan, HostLocation location, Set<IpAddress> ips,
                       Annotations... annotations) {
        this(providerId, id, mac, vlan, location, ips, false, annotations);
    }

    /**
     * Creates an end-station host using the supplied information.
     *
     * @param providerId  provider identity
     * @param id          host identifier
     * @param mac         host MAC address
     * @param vlan        host VLAN identifier
     * @param location    host location
     * @param ips         host IP addresses
     * @param configured  true if configured via NetworkConfiguration
     * @param annotations optional key/value annotations
     */
    public DefaultHost(ProviderId providerId, HostId id, MacAddress mac,
                       VlanId vlan, HostLocation location, Set<IpAddress> ips,
                       boolean configured, Annotations... annotations) {
        this(providerId, id, mac, vlan, Collections.singleton(location), ips,
                configured, annotations);
    }

    /**
     * Creates an end-station host using the supplied information.
     *
     * @param providerId provider identity
     * @param id         host identifier
     * @param mac        host MAC address
     * @param vlan       host VLAN identifier
     * @param locations  set of host locations
     * @param ips        host IP addresses
     * @param configured  true if configured via NetworkConfiguration
     * @param annotations optional key/value annotations
     */
    public DefaultHost(ProviderId providerId, HostId id, MacAddress mac,
                       VlanId vlan, Set<HostLocation> locations, Set<IpAddress> ips,
                       boolean configured, Annotations... annotations) {
        this(providerId, id, mac, vlan, locations, ips, VlanId.NONE,
                EthType.EtherType.UNKNOWN.ethType(), configured, annotations);
    }

    /**
     * Creates an end-station host using the supplied information.
     *
     * @param providerId  provider identity
     * @param id          host identifier
     * @param mac         host MAC address
     * @param vlan        host VLAN identifier
     * @param locations   set of host locations
     * @param ips         host IP addresses
     * @param innerVlan   host inner VLAN identifier
     * @param tpid        outer TPID of a host
     * @param configured  true if configured via NetworkConfiguration
     * @param annotations optional key/value annotations
     */
    public DefaultHost(ProviderId providerId, HostId id, MacAddress mac, VlanId vlan,
                       Set<HostLocation> locations, Set<IpAddress> ips, VlanId innerVlan,
                       EthType tpid, boolean configured, Annotations... annotations) {
        this(providerId, id, mac, vlan, locations, ips, innerVlan, tpid, configured, false, annotations);
    }

    /**
     * Creates an end-station host using the supplied information.
     *
     * @param providerId  provider identity
     * @param id          host identifier
     * @param mac         host MAC address
     * @param vlan        host VLAN identifier
     * @param locations   set of host locations
     * @param ips         host IP addresses
     * @param configured  true if configured via NetworkConfiguration
     * @param innerVlan   host inner VLAN identifier
     * @param tpid        outer TPID of a host
     * @param suspended   true if the host is suspended due to policy violation.
     * @param annotations optional key/value annotations
     */
    public DefaultHost(ProviderId providerId, HostId id, MacAddress mac,
                       VlanId vlan, Set<HostLocation> locations, Set<IpAddress> ips, VlanId innerVlan,
                       EthType tpid, boolean configured, boolean suspended, Annotations... annotations) {
        this(providerId, id, mac, vlan, locations, null, ips, innerVlan, tpid, configured, suspended, annotations);
    }

    /**
     * Creates an end-station host using the supplied information.
     *
     * @param providerId    provider identity
     * @param id            host identifier
     * @param mac           host MAC address
     * @param vlan          host VLAN identifier
     * @param locations     set of host locations
     * @param auxLocations  set of auxiliary locations, or null if unspecified
     * @param ips           host IP addresses
     * @param configured    true if configured via NetworkConfiguration
     * @param innerVlan     host inner VLAN identifier
     * @param tpid          outer TPID of a host
     * @param suspended     true if the host is suspended due to policy violation.
     * @param annotations   optional key/value annotations
     */
    public DefaultHost(ProviderId providerId, HostId id, MacAddress mac,
                       VlanId vlan, Set<HostLocation> locations, Set<HostLocation> auxLocations,
                       Set<IpAddress> ips, VlanId innerVlan,
                       EthType tpid, boolean configured, boolean suspended, Annotations... annotations) {
        super(providerId, id, annotations);
        this.mac = mac;
        this.vlan = vlan;
        this.locations = new HashSet<>(locations);
        this.auxLocations = (auxLocations != null) ? new HashSet<>(auxLocations) : null;
        this.ips = new HashSet<>(ips);
        this.configured = configured;
        this.innerVlan = innerVlan;
        this.tpid = tpid;
        this.suspended = suspended;
    }

    @Override
    public HostId id() {
        return (HostId) id;
    }

    @Override
    public MacAddress mac() {
        return mac;
    }

    /**
     * Returns an unmodifiable set of IP addresses currently bound to the
     * host MAC address.
     */
    @Override
    public Set<IpAddress> ipAddresses() {
        return Collections.unmodifiableSet(ips);
    }

    @Override
    public HostLocation location() {
        return locations.stream()
                .sorted(Comparator.comparingLong(HostLocation::time).reversed())
                .findFirst().orElse(null);
    }

    @Override
    public Set<HostLocation> locations() {
        return locations;
    }

    @Override
    public Set<HostLocation> auxLocations() {
        return auxLocations;
    }

    @Override
    public VlanId vlan() {
        return vlan;
    }

    @Override
    public VlanId innerVlan() {
        return innerVlan;
    }

    @Override
    public EthType tpid() {
        return tpid;
    }

    @Override
    public boolean configured() {
        return configured;
    }

    @Override
    public boolean suspended() {
        return this.suspended;
    }


    @Override
    public int hashCode() {
        return Objects.hash(id, mac, vlan, locations, auxLocations, ips, innerVlan, tpid, suspended);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultHost) {
            final DefaultHost other = (DefaultHost) obj;
            return Objects.equals(this.id, other.id) &&
                    Objects.equals(this.mac, other.mac) &&
                    Objects.equals(this.vlan, other.vlan) &&
                    Objects.equals(this.locations, other.locations) &&
                    Objects.equals(this.auxLocations, other.auxLocations) &&
                    Objects.equals(this.ipAddresses(), other.ipAddresses()) &&
                    Objects.equals(this.innerVlan, other.innerVlan) &&
                    Objects.equals(this.tpid, other.tpid) &&
                    Objects.equals(this.annotations(), other.annotations()) &&
                    Objects.equals(this.suspended, other.suspended);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id())
                .add("mac", mac())
                .add("vlan", vlan())
                .add("locations", locations())
                .add("auxLocations", auxLocations())
                .add("ipAddresses", ipAddresses())
                .add("annotations", annotations())
                .add("configured", configured())
                .add("innerVlanId", innerVlan())
                .add("outerTPID", tpid())
                .add("suspended", suspended())
                .toString();
    }

}
