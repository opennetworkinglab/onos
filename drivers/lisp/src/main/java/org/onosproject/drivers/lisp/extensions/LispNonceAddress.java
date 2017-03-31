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
        .ExtensionMappingAddressTypes.NONCE_ADDRESS;

/**
 * Implementation of LISP nonce address.
 * When a public PETR device wants to verify who is encapsulating to it, it can
 * check for a specific nonce value in the LISP encapsulated packet.
 */
public class LispNonceAddress extends AbstractExtension
        implements ExtensionMappingAddress {

    private static final String NONCE = "nonce";
    private static final String ADDRESS = "address";

    private int nonce;
    private MappingAddress address;

    /**
     * Default constructor.
     */
    public LispNonceAddress() {
    }

    /**
     * Creates an instance with initialized parameters.
     *
     * @param nonce   nonce
     * @param address address
     */
    private LispNonceAddress(int nonce, MappingAddress address) {
        this.nonce = nonce;
        this.address = address;
    }

    /**
     * Obtains nonce.
     *
     * @return nonce
     */
    public int getNonce() {
        return nonce;
    }

    /**
     * Obtains address.
     *
     * @return address
     */
    public MappingAddress getAddress() {
        return address;
    }

    @Override
    public ExtensionMappingAddressType type() {
        return NONCE_ADDRESS.type();
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> parameterMap = Maps.newHashMap();

        parameterMap.put(NONCE, nonce);
        parameterMap.put(ADDRESS, address);

        return APP_KRYO.serialize(parameterMap);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> parameterMap = APP_KRYO.deserialize(data);

        this.nonce = (int) parameterMap.get(NONCE);
        this.address = (MappingAddress) parameterMap.get(ADDRESS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonce, address);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispNonceAddress) {
            final LispNonceAddress other = (LispNonceAddress) obj;
            return Objects.equals(this.nonce, other.nonce) &&
                    Objects.equals(this.address, other.address);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("nonce", nonce)
                .add("address", address)
                .toString();
    }

    /**
     * A builder for building LispNonceAddress.
     */
    public static final class Builder {
        private int nonce;
        private MappingAddress address;

        /**
         * Sets nonce.
         *
         * @param nonce nonce
         * @return Builder object
         */
        public Builder withNonce(int nonce) {
            this.nonce = nonce;
            return this;
        }

        /**
         * Sets address.
         *
         * @param address address
         * @return Builder object
         */
        public Builder withAddress(MappingAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Builds LispNonceAddress instance.
         *
         * @return LispNonceAddress instance
         */
        public LispNonceAddress build() {

            return new LispNonceAddress(nonce, address);
        }
    }
}
