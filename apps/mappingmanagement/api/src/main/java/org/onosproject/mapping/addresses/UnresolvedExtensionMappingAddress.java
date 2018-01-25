/*
 * Copyright 2016-present Open Networking Foundation
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

import org.onosproject.net.flow.AbstractExtension;

import java.util.Arrays;
import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.mapping.addresses.ExtensionMappingAddressType
                             .ExtensionMappingAddressTypes.UNRESOLVED_TYPE;

/**
 * Unresolved extension mapping address.
 */
public class UnresolvedExtensionMappingAddress extends AbstractExtension
                                            implements ExtensionMappingAddress {

    private byte[] bytes;
    private ExtensionMappingAddressType unresolvedAddressType;

    /**
     * Creates a new unresolved extension mapping address with given data
     * in byte form.
     *
     * @param arrayByte byte data for the extension mapping address
     * @param type      unresolved extension data type
     */
    public UnresolvedExtensionMappingAddress(byte[] arrayByte,
                                             ExtensionMappingAddressType type) {
        this.bytes = arrayByte;
        this.unresolvedAddressType = type;
    }

    @Override
    public ExtensionMappingAddressType type() {
        return UNRESOLVED_TYPE.type();
    }

    @Override
    public byte[] serialize() {
        return bytes;
    }

    @Override
    public void deserialize(byte[] data) {
        bytes = data;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UnresolvedExtensionMappingAddress) {
            UnresolvedExtensionMappingAddress that =
                                        (UnresolvedExtensionMappingAddress) obj;
            return Arrays.equals(bytes, that.bytes);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("bytes", bytes)
                .add("unresolvedAddressType", unresolvedAddressType)
                .toString();
    }
}
