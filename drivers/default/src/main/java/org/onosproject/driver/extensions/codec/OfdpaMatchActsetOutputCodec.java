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
package org.onosproject.driver.extensions.codec;

import org.onosproject.driver.extensions.OfdpaMatchActsetOutput;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.PortNumber;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for Ofdpa match actset output class.
 */
public class OfdpaMatchActsetOutputCodec extends JsonCodec<OfdpaMatchActsetOutput> {

    private static final String ACTSET_OUTPUT = "actsetOutput";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in OfdpaMatchActsetOutput";
    private static final String MISSING_ACTSET_OUTPUT_MESSAGE = "Actset Output cannot be null";

    @Override
    public ObjectNode encode(OfdpaMatchActsetOutput actsetOutput, CodecContext context) {
        checkNotNull(actsetOutput, MISSING_ACTSET_OUTPUT_MESSAGE);
        return context.mapper().createObjectNode()
                .put(ACTSET_OUTPUT, actsetOutput.port().toLong());
    }

    @Override
    public OfdpaMatchActsetOutput decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse ofdpa match actset output
        long portNumber = nullIsIllegal(json.get(ACTSET_OUTPUT),
                                              ACTSET_OUTPUT + MISSING_MEMBER_MESSAGE).asLong();
        return new OfdpaMatchActsetOutput(PortNumber.portNumber(portNumber));
    }
}
