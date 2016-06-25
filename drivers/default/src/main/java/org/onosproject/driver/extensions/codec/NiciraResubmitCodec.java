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
import org.onosproject.driver.extensions.NiciraResubmit;
import org.onosproject.net.PortNumber;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for NiciraResubmit class.
 */
public final class NiciraResubmitCodec extends JsonCodec<NiciraResubmit> {

    private static final String RESUBMIT_PORT = "inPort";
    private static final String RESUBMIT_TABLE = "table";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in NiciraResubmit";

    @Override
    public ObjectNode encode(NiciraResubmit niciraResubmit, CodecContext context) {
        checkNotNull(niciraResubmit, "Nicira Resubmit cannot be null");
        ObjectNode root = context.mapper().createObjectNode()
                .put(RESUBMIT_PORT, niciraResubmit.inPort().toLong())
                .put(RESUBMIT_TABLE, niciraResubmit.table());
        return root;
    }

    @Override
    public NiciraResubmit decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse in port number
        long portNumberLong = nullIsIllegal(json.get(RESUBMIT_PORT), RESUBMIT_PORT + MISSING_MEMBER_MESSAGE).asLong();
        PortNumber portNumber = PortNumber.portNumber(portNumberLong);

        return new NiciraResubmit(portNumber);
    }
}
