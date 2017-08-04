/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onosproject.driver.extensions.NiciraSetNshSpi;
import org.onosproject.net.NshServicePathId;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for NiciraSetNshSpi class.
 */
public final class NiciraSetNshSpiCodec extends JsonCodec<NiciraSetNshSpi> {

    private static final String NSH_PATH_ID = "setNsp";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in NiciraSetNshSpi";

    @Override
    public ObjectNode encode(NiciraSetNshSpi niciraSetNshSpi, CodecContext context) {
        checkNotNull(niciraSetNshSpi, "Nicira Set Nsh Spi cannot be null");
        ObjectNode root = context.mapper().createObjectNode()
                .put(NSH_PATH_ID, niciraSetNshSpi.nshSpi().servicePathId());
        return root;
    }

    @Override
    public NiciraSetNshSpi decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse service path id
        int servicePathIdInt = nullIsIllegal(json.get(NSH_PATH_ID),
                NSH_PATH_ID + MISSING_MEMBER_MESSAGE).asInt();

        NshServicePathId pathId = NshServicePathId.of(servicePathIdInt);

        return new NiciraSetNshSpi(pathId);
    }
}
