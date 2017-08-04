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
                                .ExtensionMappingAddressTypes.SEGMENT_ADDRESS;

/**
 * Implementation of LISP segment address.
 * When multiple organizations inside of a LISP site are using private addresses
 * [RFC1918] as EID-prefixes, their address spaces must remain segregated due
 * to possible address duplication.
 */
public class LispSegmentAddress extends AbstractExtension
                                            implements ExtensionMappingAddress {

    private static final String INSTANCE_ID = "instanceId";
    private static final String ADDRESS = "address";

    private int instanceId;
    private MappingAddress address;

    /**
     * Default constructor.
     */
    public LispSegmentAddress() {
    }

    /**
     * Creates an instance with initialized parameters.
     *
     * @param instanceId instance id
     * @param address    address
     */
    private LispSegmentAddress(int instanceId, MappingAddress address) {
        this.instanceId = instanceId;
        this.address = address;
    }

    /**
     * Obtains instance identifier.
     *
     * @return instance identifier
     */
    public int getInstanceId() {
        return instanceId;
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
        return SEGMENT_ADDRESS.type();
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> parameterMap = Maps.newHashMap();

        parameterMap.put(INSTANCE_ID, instanceId);
        parameterMap.put(ADDRESS, address);

        return APP_KRYO.serialize(parameterMap);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> parameterMap = APP_KRYO.deserialize(data);

        this.instanceId = (int) parameterMap.get(INSTANCE_ID);
        this.address = (MappingAddress) parameterMap.get(ADDRESS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, instanceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispSegmentAddress) {
            final LispSegmentAddress other = (LispSegmentAddress) obj;
            return Objects.equals(this.address, other.address) &&
                    Objects.equals(this.instanceId, other.instanceId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("address", address)
                .add("instanceId", instanceId)
                .toString();
    }

    /**
     * A builder for building LispSegmentAddress.
     */
    public static final class Builder {
        private int instanceId;
        private MappingAddress address;

        /**
         * Sets instance identifer.
         *
         * @param instanceId instance identifier
         * @return Builder object
         */
        public Builder withInstanceId(int instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        /**
         * Sets address.
         *
         * @param address mapping address
         * @return Builder object
         */
        public Builder withAddress(MappingAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Builds LispSegmentAddress instance.
         *
         * @return LispSegmentAddress instance
         */
        public LispSegmentAddress build() {

            return new LispSegmentAddress(instanceId, address);
        }
    }
}
