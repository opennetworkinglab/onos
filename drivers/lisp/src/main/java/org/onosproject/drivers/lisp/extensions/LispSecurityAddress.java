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

import org.onosproject.mapping.addresses.ExtensionMappingAddress;
import org.onosproject.mapping.addresses.ExtensionMappingAddressType;
import org.onosproject.net.flow.AbstractExtension;

import static org.onosproject.mapping.addresses.ExtensionMappingAddressType
                            .ExtensionMappingAddressTypes.SECURITY_ADDRESS;

/**
 * Implementation LISP security address.
 */
public class LispSecurityAddress extends AbstractExtension
                                            implements ExtensionMappingAddress {
    @Override
    public ExtensionMappingAddressType type() {
        return SECURITY_ADDRESS.type();
    }

    @Override
    public byte[] serialize() {
        return new byte[0];
    }

    @Override
    public void deserialize(byte[] data) {

    }
}
