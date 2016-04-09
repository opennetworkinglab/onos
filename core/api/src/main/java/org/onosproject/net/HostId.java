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
package org.onosproject.net;

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Immutable representation of a host identity.
 */
public final class HostId extends ElementId {

    /**
     * Represents either no host, or an unspecified host; used for creating
     * open ingress/egress edge links.
     */
    public static final HostId NONE = new HostId(MacAddress.ZERO, VlanId.NONE);

    private static final int MAC_LENGTH = 17;
    private static final int MIN_ID_LENGTH = 19;

    private final MacAddress mac;
    private final VlanId vlanId;

    // Public construction is prohibited
    private HostId(MacAddress mac, VlanId vlanId) {
        this.mac = mac;
        this.vlanId = vlanId;
    }

    // Default constructor for serialization
    private HostId() {
        this.mac = null;
        this.vlanId = null;
    }

    /**
     * Returns the host MAC address.
     *
     * @return MAC address
     */
    public MacAddress mac() {
        return mac;
    }

    /**
     * Returns the host vlan Id.
     *
     * @return vlan Id
     */
    public VlanId vlanId() {
        return vlanId;
    }

    /**
     * Creates a device id using the supplied ID string.
     *
     * @param string device URI string
     * @return host identifier
     */
    public static HostId hostId(String string) {
        checkArgument(string.length() >= MIN_ID_LENGTH,
                      "Host ID must be at least %s characters", MIN_ID_LENGTH);
        MacAddress mac = MacAddress.valueOf(string.substring(0, MAC_LENGTH));
        VlanId vlanId = VlanId.vlanId(string.substring(MAC_LENGTH + 1));
        return new HostId(mac, vlanId);
    }

    /**
     * Creates a device id using the supplied MAC &amp; VLAN ID.
     *
     * @param mac    mac address
     * @param vlanId vlan identifier
     * @return host identifier
     */
    public static HostId hostId(MacAddress mac, VlanId vlanId) {
        return new HostId(mac, vlanId);
    }

    /**
     * Creates a device id using the supplied MAC and default VLAN.
     *
     * @param mac mac address
     * @return host identifier
     */
    public static HostId hostId(MacAddress mac) {
        return hostId(mac, VlanId.vlanId(VlanId.UNTAGGED));
    }

    public String toString() {
        return mac + "/" + vlanId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mac, vlanId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof HostId) {
            final HostId other = (HostId) obj;
            return Objects.equals(this.mac, other.mac) &&
                    Objects.equals(this.vlanId, other.vlanId);
        }
        return false;
    }

}
