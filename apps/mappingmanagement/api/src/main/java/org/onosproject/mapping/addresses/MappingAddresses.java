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
package org.onosproject.mapping.addresses;

import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.criteria.ExtensionSelector;

/**
 * Factory class for creating various mapping addresses.
 */
public final class MappingAddresses {

    /**
     * Prevents instantiation from external.
     */
    private MappingAddresses() {}

    /**
     * Creates an ASMappingAddress using the specified value.
     *
     * @param asn Autonomous System Number
     * @return mapping address
     */
    public static ASMappingAddress asMappingAddress(String asn) {
        return new ASMappingAddress(asn);
    }

    /**
     * Creates a DNMappingAddress using the specified value.
     *
     * @param dn Distinguished Name
     * @return mapping address
     */
    public static DNMappingAddress dnMappingAddress(String dn) {
        return new DNMappingAddress(dn);
    }

    /**
     * Creates an EthMappingAddress using the specified value.
     *
     * @param mac MAC address
     * @return mapping address
     */
    public static EthMappingAddress ethMappingAddress(MacAddress mac) {
        return new EthMappingAddress(mac);
    }

    /**
     * Creates an IPv4MappingAddress using the specified value.
     *
     * @param ip IP address
     * @return mapping address
     */
    public static IPMappingAddress ipv4MappingAddress(IpPrefix ip) {
        return new IPMappingAddress(ip, MappingAddress.Type.IPV4);
    }

    /**
     * Creates an IPv6MappingAddress using the specified value.
     *
     * @param ip IP address
     * @return mapping address
     */
    public static IPMappingAddress ipv6MappingAddress(IpPrefix ip) {
        return new IPMappingAddress(ip, MappingAddress.Type.IPV6);
    }

    /**
     * Creates an ExtensionMappingAddress using the specified value.
     *
     * @param selector extension selector
     * @param deviceId device identifier
     * @return mapping address
     */
    public static ExtensionMappingAddress extension(ExtensionSelector selector,
                                                    DeviceId deviceId) {
        return new ExtensionMappingAddress(selector, deviceId);
    }
}
