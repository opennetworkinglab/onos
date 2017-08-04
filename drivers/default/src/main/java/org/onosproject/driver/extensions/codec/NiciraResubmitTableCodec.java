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
import org.onosproject.driver.extensions.NiciraResubmitTable;
import org.onosproject.net.PortNumber;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for NiciraResubmitTable class.
 */
public final class NiciraResubmitTableCodec extends JsonCodec<NiciraResubmitTable> {

    private static final String RESUBMIT_PORT = "inPort";
    private static final String RESUBMIT_TABLE = "table";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in NiciraResubmitTable";

    @Override
    public ObjectNode encode(NiciraResubmitTable niciraResubmitTable, CodecContext context) {
        checkNotNull(niciraResubmitTable, "Nicira Resubmit Table cannot be null");
        ObjectNode root = context.mapper().createObjectNode()
                .put(RESUBMIT_PORT, niciraResubmitTable.inPort().toLong())
                .put(RESUBMIT_TABLE, niciraResubmitTable.table());
        return root;
    }

    @Override
    public NiciraResubmitTable decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse in port number
        long portNumberLong = nullIsIllegal(json.get(RESUBMIT_PORT),
                RESUBMIT_PORT + MISSING_MEMBER_MESSAGE).asLong();
        PortNumber portNumber = PortNumber.portNumber(portNumberLong);

        // parse table id
        short tableId = (short) nullIsIllegal(json.get(RESUBMIT_TABLE),
                RESUBMIT_TABLE + MISSING_MEMBER_MESSAGE).asInt();

        return new NiciraResubmitTable(portNumber, tableId);
    }
}
