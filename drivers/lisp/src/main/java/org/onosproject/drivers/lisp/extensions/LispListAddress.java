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

package org.onosproject.drivers.lisp.extensions;

import com.google.common.collect.Maps;
import org.onosproject.mapping.addresses.ExtensionMappingAddress;
import org.onosproject.mapping.addresses.ExtensionMappingAddressType;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.net.flow.AbstractExtension;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.mapping.addresses.ExtensionMappingAddressType
                                    .ExtensionMappingAddressTypes.LIST_ADDRESS;

/**
 * Implementation of LISP list address.
 * When header translation between IPv4 and IPv6 is desirable a LISP Canonical
 * Address can use the AFI List Type to carry a variable number of AFIs in one
 * LCAF AFI.
 */
public class LispListAddress extends AbstractExtension
                                            implements ExtensionMappingAddress {

    private static final String IPV4 = "ipv4";
    private static final String IPV6 = "ipv6";

    private MappingAddress ipv4;
    private MappingAddress ipv6;

    /**
     * Default constructor.
     */
    public LispListAddress() {
    }

    /**
     * Creates an instance with initialized parameters.
     *
     * @param ipv4 IPv4 address
     * @param ipv6 IPv6 address
     */
    private LispListAddress(MappingAddress ipv4, MappingAddress ipv6) {
        this.ipv4 = ipv4;
        this.ipv6 = ipv6;
    }

    /**
     * Obtains IPv4 address.
     *
     * @return IPv4 address
     */
    public MappingAddress getIpv4() {
        return ipv4;
    }

    /**
     * Obtains IPv6 address.
     *
     * @return IPv6 address
     */
    public MappingAddress getIpv6() {
        return ipv6;
    }

    @Override
    public ExtensionMappingAddressType type() {
        return LIST_ADDRESS.type();
    }

    @Override
    public byte[] serialize() {

        Map<String, Object> parameterMap = Maps.newHashMap();

        parameterMap.put(IPV4, ipv4);
        parameterMap.put(IPV6, ipv6);

        return APP_KRYO.serialize(parameterMap);
    }

    @Override
    public void deserialize(byte[] data) {

        Map<String, Object> parameterMap = APP_KRYO.deserialize(data);

        this.ipv4 = (MappingAddress) parameterMap.get(IPV4);
        this.ipv6 = (MappingAddress) parameterMap.get(IPV6);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipv4, ipv6);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LispListAddress) {
            LispListAddress that = (LispListAddress) obj;
            return Objects.equals(ipv4, that.ipv4) &&
                    Objects.equals(ipv6, that.ipv6);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("ipv4", ipv4)
                .add("ipv6", ipv6)
                .toString();
    }

    /**
     * A builder for building LispListAddress.
     */
    public static final class Builder {
        private MappingAddress ipv4;
        private MappingAddress ipv6;

        /**
         * Sets IPv4 address.
         *
         * @param ipv4 IPv4 address
         * @return Builder object
         */
        public Builder withIpv4(MappingAddress ipv4) {
            this.ipv4 = ipv4;
            return this;
        }

        /**
         * Sets IPv6 address.
         *
         * @param ipv6 IPv6 address
         * @return Builder object
         */
        public Builder withIpv6(MappingAddress ipv6) {
            this.ipv6 = ipv6;
            return this;
        }

        /**
         * Builds LispListAddress instance.
         *
         * @return LispListAddress instance
         */
        public LispListAddress build() {
            return new LispListAddress(ipv4, ipv6);
        }
    }
}
