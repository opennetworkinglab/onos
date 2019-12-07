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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.onlab.packet.EthType;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.HostLocation;
import org.onosproject.net.SparseAnnotations;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of an immutable host description.
 */
public class DefaultHostDescription extends AbstractDescription
        implements HostDescription {

    private final MacAddress mac;
    private final VlanId vlan;
    private final Set<HostLocation> locations;
    private final Set<HostLocation> auxLocations;
    private final Set<IpAddress> ip;
    private final VlanId innerVlan;
    private final EthType tpid;
    private final boolean configured;

    /**
     * Creates a host description using the supplied information.
     *
     * @param mac         host MAC address
     * @param vlan        host VLAN identifier
     * @param location    host location
     * @param annotations optional key/value annotations map
     */
    public DefaultHostDescription(MacAddress mac, VlanId vlan,
                                  HostLocation location,
                                  SparseAnnotations... annotations) {
        this(mac, vlan, location, Collections.emptySet(), annotations);
    }

    /**
     * Creates a host description using the supplied information.
     *
     * @param mac         host MAC address
     * @param vlan        host VLAN identifier
     * @param location    host location
     * @param ip          host IP address
     * @param annotations optional key/value annotations map
     */
    public DefaultHostDescription(MacAddress mac, VlanId vlan,
                                  HostLocation location, IpAddress ip,
                                  SparseAnnotations... annotations) {
        this(mac, vlan, location, ImmutableSet.of(ip), annotations);
    }

    /**
     * Creates a host description using the supplied information.
     *
     * @param mac         host MAC address
     * @param vlan        host VLAN identifier
     * @param location    host location
     * @param ip          host IP addresses
     * @param annotations optional key/value annotations map
     */
    public DefaultHostDescription(MacAddress mac, VlanId vlan,
                                  HostLocation location, Set<IpAddress> ip,
                                  SparseAnnotations... annotations) {
        this(mac, vlan, location, ip, false, annotations);
    }

    /**
     * Creates a host description using the supplied information.
     *
     * @param mac          host MAC address
     * @param vlan         host VLAN identifier
     * @param location     host location
     * @param configured   true if configured via NetworkConfiguration
     * @param annotations  optional key/value annotations map
     */
    public DefaultHostDescription(MacAddress mac, VlanId vlan,
                                  HostLocation location,
                                  boolean configured,
                                  SparseAnnotations... annotations) {
        this(mac, vlan, location, Collections.emptySet(), configured, annotations);
    }

    /**
     * Creates a host description using the supplied information.
     *
     * @param mac          host MAC address
     * @param vlan         host VLAN identifier
     * @param location     host location
     * @param ip           host IP address
     * @param configured   true if configured via NetworkConfiguration
     * @param annotations  optional key/value annotations map
     */
    public DefaultHostDescription(MacAddress mac, VlanId vlan,
                                  HostLocation location, Set<IpAddress> ip,
                                  boolean configured,
                                  SparseAnnotations... annotations) {
        this(mac, vlan, Collections.singleton(location), ip, configured, annotations);
    }

    /**
     * Creates a host description using the supplied information.
     *
     * @param mac          host MAC address
     * @param vlan         host VLAN identifier
     * @param locations    host locations
     * @param ip           host IP address
     * @param configured   true if configured via NetworkConfiguration
     * @param annotations  optional key/value annotations map
     */
    public DefaultHostDescription(MacAddress mac, VlanId vlan,
                                  Set<HostLocation> locations,
                                  Set<IpAddress> ip, boolean configured,
                                  SparseAnnotations... annotations) {
        this(mac, vlan, locations, ip, VlanId.NONE, EthType.EtherType.UNKNOWN.ethType(),
             configured, annotations);
    }

    /**
     * Creates a host description using the supplied information.
     *
     * @param mac          host MAC address
     * @param vlan         host VLAN identifier
     * @param locations    host locations
     * @param ip           host IP address
     * @param innerVlan    host inner VLAN identifier
     * @param tpid         outer TPID of a host
     * @param configured   true if configured via NetworkConfiguration
     * @param annotations  optional key/value annotations map
     */
    public DefaultHostDescription(MacAddress mac, VlanId vlan, Set<HostLocation> locations,
                                  Set<IpAddress> ip, VlanId innerVlan, EthType tpid,
                                  boolean configured, SparseAnnotations... annotations) {
        this(mac, vlan, locations, null, ip, innerVlan, tpid, configured, annotations);
    }

    /**
     * Creates a host description using the supplied information.
     *
     * @param mac          host MAC address
     * @param vlan         host VLAN identifier
     * @param locations    host locations
     * @param auxLocations  set of auxiliary locations, or null if unspecified
     * @param ip           host IP address
     * @param innerVlan    host inner VLAN identifier
     * @param tpid         outer TPID of a host
     * @param configured   true if configured via NetworkConfiguration
     * @param annotations  optional key/value annotations map
     */
    public DefaultHostDescription(MacAddress mac, VlanId vlan,
                                  Set<HostLocation> locations, Set<HostLocation> auxLocations,
                                  Set<IpAddress> ip, VlanId innerVlan, EthType tpid,
                                  boolean configured, SparseAnnotations... annotations) {
        super(annotations);
        this.mac = mac;
        this.vlan = vlan;
        this.locations = new HashSet<>(locations);
        this.auxLocations = (auxLocations != null) ? new HashSet<>(auxLocations) : null;
        this.ip = new HashSet<>(ip);
        this.innerVlan = innerVlan;
        this.tpid = tpid;
        this.configured = configured;
    }

    /**
     * Creates a host description using the supplied information.
     * @param base HostDescription to basic information
     * @param annotations Annotations to use.
     */
    public DefaultHostDescription(HostDescription base, SparseAnnotations annotations) {
        this(base.hwAddress(), base.vlan(), base.locations(), base.ipAddress(), base.innerVlan(), base.tpid(),
                base.configured(), annotations);
    }

    /**
     * Creates a host description using the supplied information.
     *
     * @param base base
     * @param annotations annotations
     * @return host description
     */
    public static DefaultHostDescription copyReplacingAnnotation(HostDescription base,
                                                                 SparseAnnotations annotations) {
        return new DefaultHostDescription(base, annotations);
    }

    @Override
    public MacAddress hwAddress() {
        return mac;
    }

    @Override
    public VlanId vlan() {
        return vlan;
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
    public Set<IpAddress> ipAddress() {
        return ip;
    }

    @Override
    public boolean configured() {
        return configured;
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
    public String toString() {
        return toStringHelper(this)
                .add("mac", mac)
                .add("vlan", vlan)
                .add("locations", locations)
                .add("auxLocations", auxLocations)
                .add("ipAddress", ip)
                .add("configured", configured)
                .add("innerVlanId", innerVlan)
                .add("outerTPID", tpid)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mac, vlan, locations, auxLocations, ip);
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && getClass() == object.getClass()) {
            if (!super.equals(object)) {
                return false;
            }
            DefaultHostDescription that = (DefaultHostDescription) object;
            return Objects.equals(this.mac, that.mac)
                    && Objects.equals(this.vlan, that.vlan)
                    && Objects.equals(this.locations, that.locations)
                    && Objects.equals(this.auxLocations, that.auxLocations)
                    && Objects.equals(this.ip, that.ip)
                    && Objects.equals(this.innerVlan, that.innerVlan)
                    && Objects.equals(this.tpid, that.tpid);
        }
        return false;
    }
}
