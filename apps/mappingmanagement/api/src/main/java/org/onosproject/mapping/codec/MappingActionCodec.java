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
package org.onosproject.mapping.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.mapping.actions.MappingAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapping action codec.
 */
public final class MappingActionCodec extends JsonCodec<MappingAction> {

    protected static final Logger log =
                            LoggerFactory.getLogger(MappingActionCodec.class);

    protected static final String TYPE = "type";
    protected static final String ERROR_MESSAGE =
                                    " not specified in MappingAction";

    @Override
    public ObjectNode encode(MappingAction action, CodecContext context) {
        EncodeMappingActionCodecHelper encoder =
                            new EncodeMappingActionCodecHelper(action, context);
        return encoder.encode();
    }

    @Override
    public MappingAction decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        DecodeMappingActionCodecHelper decoder =
                new DecodeMappingActionCodecHelper(json);
        return decoder.decode();
    }
}
