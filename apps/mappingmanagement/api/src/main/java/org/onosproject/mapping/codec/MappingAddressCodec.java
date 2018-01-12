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
package org.onosproject.mapping.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.mapping.addresses.MappingAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapping address codec.
 */
public final class MappingAddressCodec extends JsonCodec<MappingAddress> {

    private static final Logger log =
                            LoggerFactory.getLogger(MappingAddressCodec.class);

    static final String TYPE = "type";
    static final String IPV4 = "ipv4";
    static final String IPV6 = "ipv6";
    static final String MAC = "mac";
    static final String DN = "dn";
    static final String AS = "as";

    @Override
    public ObjectNode encode(MappingAddress address, CodecContext context) {
        EncodeMappingAddressCodecHelper encoder =
                            new EncodeMappingAddressCodecHelper(address, context);
        return encoder.encode();
    }

    @Override
    public MappingAddress decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        DecodeMappingAddressCodecHelper decoder =
                            new DecodeMappingAddressCodecHelper(json);
        return decoder.decode();
    }
}
