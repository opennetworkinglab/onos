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

import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * Mapping address for implementing extensions.
 */
public final class ExtensionMappingAddressWrapper implements MappingAddress {

    private final ExtensionMappingAddress extensionMappingAddress;
    private final DeviceId deviceId;

    /**
     * Default constructor of ExtensionMappingAddressWrapper.
     *
     * @param extensionMappingAddress extension mapping address
     * @param deviceId device identifier
     */
    public ExtensionMappingAddressWrapper(ExtensionMappingAddress extensionMappingAddress,
                                          DeviceId deviceId) {
        this.extensionMappingAddress = extensionMappingAddress;
        this.deviceId = deviceId;
    }

    @Override
    public Type type() {
        return Type.EXTENSION;
    }

    /**
     * Returns the extension mapping address.
     *
     * @return extension mapping address
     */
    public ExtensionMappingAddress extensionMappingAddress() {
        return extensionMappingAddress;
    }

    /**
     * Returns the device identifier.
     *
     * @return the device identifier
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public String toString() {
        return type().toString() + TYPE_SEPARATOR + extensionMappingAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), extensionMappingAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ExtensionMappingAddressWrapper) {
            ExtensionMappingAddressWrapper that = (ExtensionMappingAddressWrapper) obj;
            return Objects.equals(extensionMappingAddress, that.extensionMappingAddress) &&
                    Objects.equals(deviceId, that.deviceId);
        }
        return false;
    }
}
