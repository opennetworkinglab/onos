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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Interface for encode and decode extension mapping address.
 */
public interface ExtensionMappingAddressCodec extends HandlerBehaviour {

    /**
     * Encodes an extension mapping address to an JSON object.
     *
     * @param extensionMappingAddress extension mapping address
     * @param context                 encoding context
     * @return JSON object
     */
    default ObjectNode encode(ExtensionMappingAddress extensionMappingAddress,
                              CodecContext context) {
        return null;
    }

    /**
     * Decodes a JSON object to an extension mapping address.
     *
     * @param objectNode JSON object
     * @param context    decoding context
     * @return extension mapping address
     */
    default ExtensionMappingAddress decode(ObjectNode objectNode, CodecContext context) {
        return null;
    }
}
