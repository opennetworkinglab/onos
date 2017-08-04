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
package org.onosproject.mapping.addresses;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Type of mapping address extensions.
 */
public class ExtensionMappingAddressType {

    /**
     * A list of well-known named extension mapping address type codes.
     * These numbers have no impact on the actual LISP type id.
     */
    public enum ExtensionMappingAddressTypes {
        LIST_ADDRESS(1),
        SEGMENT_ADDRESS(2),
        AS_ADDRESS(3),
        APPLICATION_DATA_ADDRESS(4),
        GEO_COORDINATE_ADDRESS(5),
        NAT_ADDRESS(7),
        NONCE_ADDRESS(8),
        MULTICAST_ADDRESS(9),
        TRAFFIC_ENGINEERING_ADDRESS(10),
        SECURITY_ADDRESS(11),
        SOURCE_DEST_ADDRESS(12),

        UNRESOLVED_TYPE(200);

        private ExtensionMappingAddressType type;

        /**
         * Creates a new named extension mapping address type.
         *
         * @param type type code
         */
        ExtensionMappingAddressTypes(int type) {
            this.type = new ExtensionMappingAddressType(type);
        }

        /**
         * Obtains the extension type object for this named type code.
         *
         * @return extension type object
         */
        public ExtensionMappingAddressType type() {
            return type;
        }
    }

    private final int type;

    /**
     * Creates an extension type with the given int type code.
     *
     * @param type type code
     */
    public ExtensionMappingAddressType(int type) {
        this.type = type;
    }

    /**
     * Obtains the integer value associated with this type.
     *
     * @return an integer value
     */
    public int toInt() {
        return this.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ExtensionMappingAddressType) {
            final ExtensionMappingAddressType that = (ExtensionMappingAddressType) obj;
            return this.type == that.type;
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(ExtensionMappingAddressType.class)
                    .add("type", type)
                    .toString();
    }
}
