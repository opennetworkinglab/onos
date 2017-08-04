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
package org.onosproject.lisp.ctl;

import org.onosproject.lisp.msg.types.lcaf.LispLcafAddress;
import org.onosproject.mapping.addresses.ExtensionMappingAddress;
import org.onosproject.mapping.addresses.ExtensionMappingAddressCodec;
import org.onosproject.mapping.addresses.ExtensionMappingAddressType;

/**
 * Interprets extension addresses and converts them to/from LISP objects.
 */
public interface ExtensionMappingAddressInterpreter extends ExtensionMappingAddressCodec {

    /**
     * Returns true if the given extension mapping address is supported by this
     * driver.
     *
     * @param extensionMappingAddressType extension mapping address type
     * @return true if the address is supported, otherwise false
     */
    boolean supported(ExtensionMappingAddressType extensionMappingAddressType);

    /**
     * Maps an extension mapping address to a LCAF address.
     *
     * @param mappingAddress extension mapping address
     * @return LISP LCAF address
     */
    LispLcafAddress mapMappingAddress(ExtensionMappingAddress mappingAddress);

    /**
     * Maps a LCAF address to an extension mapping address.
     *
     * @param lcafAddress LCAF address
     * @return extension mapping address
     */
    ExtensionMappingAddress mapLcafAddress(LispLcafAddress lcafAddress);
}
