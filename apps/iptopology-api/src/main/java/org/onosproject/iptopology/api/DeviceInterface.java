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

import java.util.Objects;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;

import com.google.common.base.MoreObjects;

/**
 * Representation of device interface.
 */
public class DeviceInterface {
    private final Ip4Address ip4Address;
    private final Ip6Address ip6Address;
    private final InterfaceIdentifier interfaceId;

    /**
     * Constructor to initialize its parameter.
     *
     * @param ip4Address ipv4 interface address
     * @param ip6Address ipv6 interface address
     * @param interfaceId interface Identifier
     */
    public DeviceInterface(Ip4Address ip4Address, Ip6Address ip6Address, InterfaceIdentifier interfaceId) {
        this.ip4Address = ip4Address;
        this.ip6Address = ip6Address;
        this.interfaceId = interfaceId;
    }

    /**
     * obtains ipv4 address of an interface.
     *
     * @return ipv4 interface address
     */
    public Ip4Address ip4Address() {
        return ip4Address;
    }

    /**
     * obtains ipv6 interface address.
     *
     * @return ipv6 interface address
     */
    public Ip6Address ip6Address() {
        return ip6Address;
    }

    /**
     * obtains interface identifier.
     *
     * @return interface identifier
     */
    public InterfaceIdentifier interfaceId() {
        return interfaceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip4Address, ip6Address, interfaceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DeviceInterface) {
            final DeviceInterface other = (DeviceInterface) obj;
            return Objects.equals(this.ip4Address, other.ip4Address)
                    && Objects.equals(this.ip6Address, other.ip6Address)
                    && Objects.equals(this.interfaceId, other.interfaceId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("ip4Address", ip4Address)
                .add("ip6Address", ip6Address)
                .add("interfaceId", interfaceId)
                .toString();
    }
}