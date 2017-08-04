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
                            .ExtensionMappingAddressTypes.SOURCE_DEST_ADDRESS;

/**
 * Implementation of LISP source and destination address.
 * When both a source and destination address of a flow need consideration for
 * different locator-sets, this 2-tuple key is used in EID fields in LISP
 * control messages.
 */
public class LispSrcDstAddress extends AbstractExtension
                                            implements ExtensionMappingAddress {

    private static final String SRC_MASK_LENGTH = "srcMaskLength";
    private static final String DST_MASK_LENGTH = "dstMaskLength";
    private static final String SRC_PREFIX = "srcPrefix";
    private static final String DST_PREFIX = "dstPrefix";

    private byte srcMaskLength;
    private byte dstMaskLength;
    private MappingAddress srcPrefix;
    private MappingAddress dstPrefix;

    /**
     * Default constructor.
     */
    public LispSrcDstAddress() {
    }

    /**
     * Creates an instance with initialized parameters.
     *
     * @param srcMaskLength source mask length
     * @param dstMaskLength destination mask length
     * @param srcPrefix     source address prefix
     * @param dstPrefix     destination address prefix
     */
    private LispSrcDstAddress(byte srcMaskLength, byte dstMaskLength,
                              MappingAddress srcPrefix, MappingAddress dstPrefix) {
        this.srcMaskLength = srcMaskLength;
        this.dstMaskLength = dstMaskLength;
        this.srcPrefix = srcPrefix;
        this.dstPrefix = dstPrefix;
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
     * Obtains destination mask length.
     *
     * @return destination mask length
     */
    public byte getDstMaskLength() {
        return dstMaskLength;
    }

    /**
     * Obtains source address prefix.
     *
     * @return source address prefix
     */
    public MappingAddress getSrcPrefix() {
        return srcPrefix;
    }

    /**
     * Obtains destination address prefix.
     *
     * @return destination address prefix
     */
    public MappingAddress getDstPrefix() {
        return dstPrefix;
    }

    @Override
    public ExtensionMappingAddressType type() {
        return SOURCE_DEST_ADDRESS.type();
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> parameterMap = Maps.newHashMap();

        parameterMap.put(SRC_MASK_LENGTH, srcMaskLength);
        parameterMap.put(DST_MASK_LENGTH, dstMaskLength);
        parameterMap.put(SRC_PREFIX, srcPrefix);
        parameterMap.put(DST_PREFIX, dstPrefix);

        return APP_KRYO.serialize(parameterMap);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> parameterMap = APP_KRYO.deserialize(data);

        this.srcMaskLength = (byte) parameterMap.get(SRC_MASK_LENGTH);
        this.dstMaskLength = (byte) parameterMap.get(DST_MASK_LENGTH);
        this.srcPrefix = (MappingAddress) parameterMap.get(SRC_PREFIX);
        this.dstPrefix = (MappingAddress) parameterMap.get(DST_PREFIX);
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcPrefix, dstPrefix, srcMaskLength, dstMaskLength);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispSrcDstAddress) {
            final LispSrcDstAddress other = (LispSrcDstAddress) obj;
            return Objects.equals(this.srcPrefix, other.srcPrefix) &&
                    Objects.equals(this.dstPrefix, other.dstPrefix) &&
                    Objects.equals(this.srcMaskLength, other.srcMaskLength) &&
                    Objects.equals(this.dstMaskLength, other.dstMaskLength);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("source prefix", srcPrefix)
                .add("destination prefix", dstPrefix)
                .add("source mask length", srcMaskLength)
                .add("destination mask length", dstMaskLength)
                .toString();
    }

    /**
     * A builder for building LispSrcDstAddress.
     */
    public static final class Builder {
        private byte srcMaskLength;
        private byte dstMaskLength;
        private MappingAddress srcPrefix;
        private MappingAddress dstPrefix;

        /**
         * Sets source address prefix.
         *
         * @param srcPrefix source prefix
         * @return Builder object
         */
        public Builder withSrcPrefix(MappingAddress srcPrefix) {
            this.srcPrefix = srcPrefix;
            return this;
        }

        /**
         * Sets destination address prefix.
         *
         * @param dstPrefix destination prefix
         * @return Builder object
         */
        public Builder withDstPrefix(MappingAddress dstPrefix) {
            this.dstPrefix = dstPrefix;
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
         * Sets destination mask length.
         *
         * @param dstMaskLength destination mask length
         * @return Builder object
         */
        public Builder withDstMaskLength(byte dstMaskLength) {
            this.dstMaskLength = dstMaskLength;
            return this;
        }

        /**
         * Builds LispSrcDstAddress instance.
         *
         * @return LispSrcDstAddress instance
         */
        public LispSrcDstAddress build() {

            return new LispSrcDstAddress(srcMaskLength, dstMaskLength,
                                                        srcPrefix, dstPrefix);
        }
    }
}
