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
                                .ExtensionMappingAddressTypes.MULTICAST_ADDRESS;

/**
 * Implementation of LISP multicast address.
 * The intent of this type of unicast replication is to deliver packets to
 * multiple ETRs at receiver LISP multicast sites.
 */
public class LispMulticastAddress extends AbstractExtension
        implements ExtensionMappingAddress {

    private static final String INSTANCE_ID = "instanceId";
    private static final String SRC_MASK_LENGTH = "srcMaskLength";
    private static final String GRP_MASK_LENGTH = "grpMaskLength";
    private static final String SRC_ADDRESS = "srcAddress";
    private static final String GRP_ADDRESS = "grpAddress";

    private int instanceId;
    private byte srcMaskLength;
    private byte grpMaskLength;
    private MappingAddress srcAddress;
    private MappingAddress grpAddress;

    /**
     * Default constructor.
     */
    public LispMulticastAddress() {
    }

    /**
     * Creates an instance with initialized parameters.
     *
     * @param instanceId    instance identifier
     * @param srcMaskLength source mask length
     * @param grpMaskLength group mask length
     * @param srcAddress    source address
     * @param grpAddress    group address
     */
    private LispMulticastAddress(int instanceId, byte srcMaskLength,
                                 byte grpMaskLength, MappingAddress srcAddress,
                                 MappingAddress grpAddress) {
        this.instanceId = instanceId;
        this.srcMaskLength = srcMaskLength;
        this.grpMaskLength = grpMaskLength;
        this.srcAddress = srcAddress;
        this.grpAddress = grpAddress;
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
     * Obtains source mask length.
     *
     * @return source mask length
     */
    public byte getSrcMaskLength() {
        return srcMaskLength;
    }

    /**
     * Obtains group mask length.
     *
     * @return group mask length
     */
    public byte getGrpMaskLength() {
        return grpMaskLength;
    }

    /**
     * Obtains source address.
     *
     * @return source address
     */
    public MappingAddress getSrcAddress() {
        return srcAddress;
    }

    /**
     * Obtains group address.
     *
     * @return group address
     */
    public MappingAddress getGrpAddress() {
        return grpAddress;
    }

    @Override
    public ExtensionMappingAddressType type() {
        return MULTICAST_ADDRESS.type();
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> parameterMap = Maps.newHashMap();

        parameterMap.put(INSTANCE_ID, instanceId);
        parameterMap.put(SRC_MASK_LENGTH, srcMaskLength);
        parameterMap.put(GRP_MASK_LENGTH, grpMaskLength);
        parameterMap.put(SRC_ADDRESS, srcAddress);
        parameterMap.put(GRP_ADDRESS, grpAddress);

        return APP_KRYO.serialize(parameterMap);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> parameterMap = APP_KRYO.deserialize(data);

        this.instanceId = (int) parameterMap.get(INSTANCE_ID);
        this.srcMaskLength = (byte) parameterMap.get(SRC_MASK_LENGTH);
        this.grpMaskLength = (byte) parameterMap.get(GRP_MASK_LENGTH);
        this.srcAddress = (MappingAddress) parameterMap.get(SRC_ADDRESS);
        this.grpAddress = (MappingAddress) parameterMap.get(GRP_ADDRESS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId, srcMaskLength, grpMaskLength,
                srcAddress, grpAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispMulticastAddress) {
            final LispMulticastAddress other = (LispMulticastAddress) obj;
            return Objects.equals(this.instanceId, other.instanceId) &&
                    Objects.equals(this.srcMaskLength, other.srcMaskLength) &&
                    Objects.equals(this.grpMaskLength, other.grpMaskLength) &&
                    Objects.equals(this.srcAddress, other.srcAddress) &&
                    Objects.equals(this.grpAddress, other.grpAddress);

        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("instance ID", instanceId)
                .add("source mask length", srcMaskLength)
                .add("group mask length", grpMaskLength)
                .add("source address", srcAddress)
                .add("group address", grpAddress)
                .toString();
    }

    /**
     * A builder for building LispMulticastAddress.
     */
    public static final class Builder {
        private int instanceId;
        private byte srcMaskLength;
        private byte grpMaskLength;
        private MappingAddress srcAddress;
        private MappingAddress grpAddress;

        /**
         * Sets instance identifier.
         *
         * @param instanceId instance identifier
         * @return Builder object
         */
        public Builder withInstanceId(int instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        /**
         * Sets source mask length.
         *
         * @param srcMaskLength source mask length
         * @return Builder object
         */
        public Builder withSrcMaskLength(byte srcMaskLength) {
            this.srcMaskLength = srcMaskLength;
            return this;
        }

        /**
         * Sets group mask length.
         *
         * @param grpMaskLength group mask length
         * @return Builder object
         */
        public Builder withGrpMaskLength(byte grpMaskLength) {
            this.grpMaskLength = grpMaskLength;
            return this;
        }

        /**
         * Sets source address.
         *
         * @param srcAddress source address
         * @return Builder object
         */
        public Builder withSrcAddress(MappingAddress srcAddress) {
            this.srcAddress = srcAddress;
            return this;
        }

        /**
         * Sets group address.
         *
         * @param grpAddress group address
         * @return Builder object
         */
        public Builder withGrpAddress(MappingAddress grpAddress) {
            this.grpAddress = grpAddress;
            return this;
        }

        /**
         * Builds LispMulticastAddress instance.
         *
         * @return LispMulticastAddress instance
         */
        public LispMulticastAddress build() {

            return new LispMulticastAddress(instanceId, srcMaskLength,
                    grpMaskLength, srcAddress, grpAddress);
        }
    }
}
