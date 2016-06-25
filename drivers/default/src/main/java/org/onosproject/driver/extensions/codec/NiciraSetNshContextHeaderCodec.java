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
package org.onosproject.driver.extensions.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.driver.extensions.NiciraSetNshContextHeader;
import org.onosproject.net.NshContextHeader;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for NiciraSetNshContextHeader class.
 */
public class NiciraSetNshContextHeaderCodec extends JsonCodec<NiciraSetNshContextHeader> {

    private static final String NSH_CONTEXT_HEADER = "nshch";
    private static final String TYPE = "type";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in NiciraSetNshContextHeader";

    @Override
    public ObjectNode encode(NiciraSetNshContextHeader niciraSetNshContextHeader, CodecContext context) {
        checkNotNull(niciraSetNshContextHeader, "Nicira Set Nsh Context Header cannot be null");
        ObjectNode root = context.mapper().createObjectNode()
                .put(NSH_CONTEXT_HEADER, niciraSetNshContextHeader.nshCh().nshContextHeader())
                .put(TYPE, niciraSetNshContextHeader.type().type());
        return root;
    }

    @Override
    public NiciraSetNshContextHeader decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse nsh context header
        int contextHeaderInt = nullIsIllegal(json.get(NSH_CONTEXT_HEADER),
                NSH_CONTEXT_HEADER + MISSING_MEMBER_MESSAGE).asInt();

        NshContextHeader contextHeader = NshContextHeader.of(contextHeaderInt);

        // parse type
        int extensionTypeInt = nullIsIllegal(json.get(TYPE),
                TYPE + MISSING_MEMBER_MESSAGE).asInt();

        ExtensionTreatmentType type = new ExtensionTreatmentType(extensionTypeInt);

        return new NiciraSetNshContextHeader(contextHeader, type);
    }
}
