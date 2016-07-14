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
import org.onosproject.driver.extensions.NiciraMatchNshSi;
import org.onosproject.net.NshServiceIndex;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for NiciraMatchNshSi class.
 */
public final class NiciraMatchNshSiCodec extends JsonCodec<NiciraMatchNshSi> {

    private static final String NSH_SERVICE_INDEX = "nsi";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in NiciraMatchNshSi";

    @Override
    public ObjectNode encode(NiciraMatchNshSi niciraMatchNshSi, CodecContext context) {
        checkNotNull(niciraMatchNshSi, "Nicira Match Nsh Si cannot be null");
        ObjectNode root = context.mapper().createObjectNode()
                .put(NSH_SERVICE_INDEX, niciraMatchNshSi.nshSi().serviceIndex());
        return root;
    }

    @Override
    public NiciraMatchNshSi decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse nsh service index
        short nshSiShort = (short) nullIsIllegal(json.get(NSH_SERVICE_INDEX),
                NSH_SERVICE_INDEX + MISSING_MEMBER_MESSAGE).asInt();
        NshServiceIndex nshSi = NshServiceIndex.of(nshSiShort);

        return new NiciraMatchNshSi(nshSi);
    }
}
