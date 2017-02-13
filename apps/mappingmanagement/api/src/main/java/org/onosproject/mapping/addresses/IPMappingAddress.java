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

import java.util.Objects;

/**
 * Implementation of IP mapping address.
 */
public final class IPMappingAddress implements MappingAddress {

    private final IpPrefix ip;
    private final Type type;

    /**
     * Default constructor of IPMappingAddress.
     *
     * @param ip   the IP prefix to match. Could be either IPv4 or IPv6
     * @param type the match type, the type can be one of the following:
     *             Type.IPV4, Type.IPV6
     */
    IPMappingAddress(IpPrefix ip, Type type) {
        this.ip = ip;
        this.type = type;
    }

    @Override
    public Type type() {
        return type;
    }

    /**
     * Obtains the IP prefix to look up.
     *
     * @return the IP prefix to look up
     */
    public IpPrefix ip() {
        return this.ip;
    }

    @Override
    public String toString() {
        return type().toString() + TYPE_SEPARATOR + ip;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), ip);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPMappingAddress) {
            IPMappingAddress that = (IPMappingAddress) obj;
            return Objects.equals(ip, that.ip) &&
                    Objects.equals(type, that.type);
        }
        return false;
    }
}
