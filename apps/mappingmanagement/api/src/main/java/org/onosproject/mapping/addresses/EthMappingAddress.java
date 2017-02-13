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

import org.onlab.packet.MacAddress;

import java.util.Objects;

/**
 * Implementation of MAC mapping address.
 */
public final class EthMappingAddress implements MappingAddress {
    private final MacAddress mac;

    /**
     * Default constructor of EthMappingAddress.
     *
     * @param mac the MAC address to look up
     */
    EthMappingAddress(MacAddress mac) {
        this.mac = mac;
    }

    @Override
    public Type type() {
        return Type.ETH;
    }

    /**
     * Obtains the MAC address to look up.
     *
     * @return the MAC address to look up
     */
    public MacAddress mac() {
        return this.mac;
    }

    @Override
    public String toString() {
        return type().toString() + TYPE_SEPARATOR + mac;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Type.ETH, mac);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EthMappingAddress) {
            EthMappingAddress that = (EthMappingAddress) obj;
            return Objects.equals(mac, that.mac);
        }
        return false;
    }
}
