/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.provider.ProviderId;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default representation of a virtual host.
 */
public final class DefaultVirtualHost extends DefaultHost implements VirtualHost {

    private static final String VIRTUAL = "virtual";
    private static final ProviderId PID = new ProviderId(VIRTUAL, VIRTUAL);

    private final NetworkId networkId;

    /**
     * Creates a virtual host attributed to the specified provider.
     *
     * @param networkId network identifier
     * @param id        host identifier
     * @param mac       host MAC address
     * @param vlan      host VLAN identifier
     * @param location  host location
     * @param ips       host IP addresses
     */
    public DefaultVirtualHost(NetworkId networkId, HostId id, MacAddress mac,
                              VlanId vlan, HostLocation location, Set<IpAddress> ips) {
        super(PID, id, mac, vlan, location, ips, DefaultAnnotations.builder().build());
        this.networkId = networkId;
    }

    @Override
    public NetworkId networkId() {
        return networkId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultVirtualHost) {
            DefaultVirtualHost that = (DefaultVirtualHost) obj;
            return super.equals(that) && Objects.equals(this.networkId, that.networkId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("networkId", networkId).toString();
    }
}
