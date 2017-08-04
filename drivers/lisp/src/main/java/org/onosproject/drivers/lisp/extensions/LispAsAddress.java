/*
 * Copyright 2017-present Open Networking Foundation
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
                                        .ExtensionMappingAddressTypes.AS_ADDRESS;

/**
 * Implementation of LISP Autonomous System (AS) address.
 * When an AS number is stored in the LISP Mapping Database System for either
 * policy or documentation reasons, it can be encoded in a LISP Canonical Address.
 */
public final class LispAsAddress extends AbstractExtension
                                            implements ExtensionMappingAddress {

    private static final String AS_NUMBER = "asNumber";
    private static final String ADDRESS = "address";

    private int asNumber;
    private MappingAddress address;

    /**
     * Default constructor.
     */
    public LispAsAddress() {
    }

    /**
     * Creates an instance with initialized parameters.
     *
     * @param asNumber AS number
     */
    private LispAsAddress(int asNumber, MappingAddress address) {
        this.asNumber = asNumber;
        this.address = address;
    }

    /**
     * Obtains AS number.
     *
     * @return AS number
     */
    public int getAsNumber() {
        return asNumber;
    }

    /**
     * Obtains mapping address.
     *
     * @return mapping address
     */
    public MappingAddress getAddress() {
        return address;
    }

    @Override
    public ExtensionMappingAddressType type() {
        return AS_ADDRESS.type();
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> parameterMap = Maps.newHashMap();

        parameterMap.put(AS_NUMBER, asNumber);
        parameterMap.put(ADDRESS, address);
        return APP_KRYO.serialize(parameterMap);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> parameterMap = APP_KRYO.deserialize(data);

        this.asNumber = (int) parameterMap.get(AS_NUMBER);
        this.address = (MappingAddress) parameterMap.get(ADDRESS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asNumber, address);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispAsAddress) {
            final LispAsAddress other = (LispAsAddress) obj;
            return Objects.equals(asNumber, other.asNumber) &&
                    Objects.equals(address, other.address);
        }

        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("AS number", asNumber)
                .add("address", address)
                .toString();
    }

    /**
     * A builder for building LispAsAddress.
     */
    public static final class Builder {
        private int asNumber;
        private MappingAddress address;

        /**
         * Sets AS number.
         *
         * @param asNumber AS number
         * @return Builder object
         */
        public Builder withAsNumber(int asNumber) {
            this.asNumber = asNumber;
            return this;
        }

        /**
         * Sets mapping address.
         *
         * @param address mapping address
         * @return Builder object
         */
        public Builder withAddress(MappingAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Builds LispAsAddress instance.
         *
         * @return LispAsAddress instance
         */
        public LispAsAddress build() {

            return new LispAsAddress(asNumber, address);
        }
    }
}
