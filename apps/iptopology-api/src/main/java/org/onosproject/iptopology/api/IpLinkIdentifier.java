/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;

/**
 * Represents Ip Link Identifier.
 */
public class IpLinkIdentifier {
    private final InterfaceIdentifier localIndentifier;
    private final InterfaceIdentifier remoteIndentifier;
    private final Ip4Address localIpv4Addr;
    private final Ip4Address remoteIpv4Addr;
    private final Ip6Address localIpv6Addr;
    private final Ip6Address remoteIpv6Addr;
    private final TopologyId topologyId;

    /**
     * Constructor to initialize its parameters.
     *
     * @param localIndentifier  local interface identifier of the link
     * @param remoteIndentifier remote interface identifier of the link
     * @param localIpv4Addr     local IPv4 address of the link
     * @param remoteIpv4Addr    remote IPv4 address of the link
     * @param localIpv6Addr     local IPv6 address of the link
     * @param remoteIpv6Addr    remote IPv6 address of the link
     * @param topologyId        link topology identifier
     */
    public IpLinkIdentifier(InterfaceIdentifier localIndentifier, InterfaceIdentifier remoteIndentifier,
                            Ip4Address localIpv4Addr, Ip4Address remoteIpv4Addr, Ip6Address localIpv6Addr,
                            Ip6Address remoteIpv6Addr, TopologyId topologyId) {
        this.localIndentifier = localIndentifier;
        this.remoteIndentifier = remoteIndentifier;
        this.localIpv4Addr = localIpv4Addr;
        this.remoteIpv4Addr = remoteIpv4Addr;
        this.localIpv6Addr = localIpv6Addr;
        this.remoteIpv6Addr = remoteIpv6Addr;
        this.topologyId = topologyId;
    }

    /**
     * Obtains link local identifier.
     *
     * @return link local identifier
     */
    public InterfaceIdentifier localIndentifier() {
        return localIndentifier;
    }

    /**
     * Obtains link local identifier.
     *
     * @return link local identifier
     */
    public InterfaceIdentifier remoteIndentifier() {
        return remoteIndentifier;
    }

    /**
     * Obtains local IPv4 address of the link.
     *
     * @return local IPv4  address of the link
     */
    public Ip4Address localIpv4Addr() {
        return localIpv4Addr;
    }

    /**
     * Obtains remote IPv4 address of the link.
     *
     * @return remote IPv4  address of the link
     */
    public Ip4Address remoteIpv4Addr() {
        return remoteIpv4Addr;
    }

    /**
     * Obtains local IPv6 address of the link.
     *
     * @return local IPv6 address of the link
     */
    public Ip6Address localIpv6Addr() {
        return localIpv6Addr;
    }

    /**
     * Obtains remote IPv6 address of the link.
     *
     * @return remote IPv6 address of the link
     */
    public Ip6Address remoteIpv6Addr() {
        return remoteIpv6Addr;
    }

    /**
     * Obtains Topology ID of the link.
     *
     * @return Topology ID of the link
     */
    public TopologyId topologyId() {
        return topologyId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(localIndentifier, remoteIndentifier, localIpv4Addr, remoteIpv4Addr,
                localIpv6Addr, remoteIpv6Addr, topologyId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof IpLinkIdentifier) {
            IpLinkIdentifier other = (IpLinkIdentifier) obj;
            return Objects.equals(topologyId, other.topologyId)
                    && Objects.equals(localIndentifier, other.localIndentifier)
                    && Objects.equals(remoteIndentifier, other.remoteIndentifier)
                    && Objects.equals(localIpv4Addr, other.localIpv4Addr)
                    && Objects.equals(remoteIpv4Addr, other.remoteIpv4Addr)
                    && Objects.equals(localIpv6Addr, other.localIpv6Addr)
                    && Objects.equals(remoteIpv6Addr, other.remoteIpv6Addr);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .add("localIndentifier", localIndentifier)
                .add("remoteIndentifier", remoteIndentifier)
                .add("localIpv4Addr", localIpv4Addr)
                .add("remoteIpv4Addr", remoteIpv4Addr)
                .add("localIpv6Addr", localIpv6Addr)
                .add("remoteIpv6Addr", remoteIpv6Addr)
                .add("topologyId", topologyId)
                .toString();
    }
}