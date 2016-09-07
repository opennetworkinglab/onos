/*
 * Copyright 2014-present Open Networking Laboratory
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
import java.util.Set;

import org.onosproject.net.AbstractDescription;
import org.onosproject.net.HostLocation;
import org.onosproject.net.SparseAnnotations;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.MoreObjects.toStringHelper;
import com.google.common.base.Objects;

/**
 * Default implementation of an immutable host description.
 */
public class DefaultHostDescription extends AbstractDescription
        implements HostDescription {

    private final MacAddress mac;
    private final VlanId vlan;
    private final HostLocation location;
    private final Set<IpAddress> ip;

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
        this(mac, vlan, location, Collections.emptySet(),
             annotations);
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
        super(annotations);
        this.mac = mac;
        this.vlan = vlan;
        this.location = location;
        this.ip = ImmutableSet.copyOf(ip);
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
        return location;
    }

    @Override
    public Set<IpAddress> ipAddress() {
        return ip;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("mac", mac)
                .add("vlan", vlan)
                .add("location", location)
                .add("ipAddress", ip)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), mac, vlan, location, ip);
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && getClass() == object.getClass()) {
            if (!super.equals(object)) {
                return false;
            }
            DefaultHostDescription that = (DefaultHostDescription) object;
            return Objects.equal(this.mac, that.mac)
                    && Objects.equal(this.vlan, that.vlan)
                    && Objects.equal(this.location, that.location)
                    && Objects.equal(this.ip, that.ip);
        }
        return false;
    }

}
