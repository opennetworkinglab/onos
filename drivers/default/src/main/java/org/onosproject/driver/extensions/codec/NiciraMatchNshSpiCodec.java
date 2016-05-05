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
import org.onosproject.driver.extensions.NiciraMatchNshSpi;
import org.onosproject.net.NshServicePathId;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for NiciraMatchNshSpi class.
 */
public final class NiciraMatchNshSpiCodec extends JsonCodec<NiciraMatchNshSpi> {

    private static final String NSH_PATH_ID = "nsp";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in NiciraMatchNshSpi";

    @Override
    public ObjectNode encode(NiciraMatchNshSpi niciraMatchNshSpi, CodecContext context) {
        checkNotNull(niciraMatchNshSpi, "Nicira Match Nsh Spi cannot be null");
        ObjectNode root = context.mapper().createObjectNode()
                .put(NSH_PATH_ID, niciraMatchNshSpi.nshSpi().servicePathId());
        return root;
    }

    @Override
    public NiciraMatchNshSpi decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse nsh path id
        int nshSpiInt = nullIsIllegal(json.get(NSH_PATH_ID),
                NSH_PATH_ID + MISSING_MEMBER_MESSAGE).asInt();
        NshServicePathId nshSpi = NshServicePathId.of(nshSpiInt);

        return new NiciraMatchNshSpi(nshSpi);
    }
}
