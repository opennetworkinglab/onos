/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.flow.criteria.ExtensionSelector;

/**
 * Interface for encode and decode extension selector.
 */
public interface ExtensionSelectorCodec extends HandlerBehaviour {

    /**
     * Encodes an extension selector to an JSON object.
     *
     * @param extensionSelector extension selector
     * @param  context encoding context
     * @return JSON object
     */
    default ObjectNode encode(ExtensionSelector extensionSelector, CodecContext context) {
        return null;
    }

    /**
     * Decodes an JSON object to an extension selector.
     *
     * @param objectNode JSON object
     * @param  context decoding context
     * @return extension selector
     */
    default ExtensionSelector decode(ObjectNode objectNode, CodecContext context) {
        return null;
    }
}
