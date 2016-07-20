/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnweb.web;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.vtnrsc.AllowedAddressPair;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * VirtualPort AllowedAddressPair codec.
 */
public final class AllowedAddressPairCodec extends JsonCodec<AllowedAddressPair> {

    @Override
    public ObjectNode encode(AllowedAddressPair alocAddPair, CodecContext context) {
        checkNotNull(alocAddPair, "AllowedAddressPair cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("ip_address", alocAddPair.ip().toString())
                .put("mac_address", alocAddPair.mac().toString());
        return result;
    }

}
