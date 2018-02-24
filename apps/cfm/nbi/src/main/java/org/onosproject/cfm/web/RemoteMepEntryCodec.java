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
package org.onosproject.cfm.web;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Encode and decode to/from JSON to RemoteMepEntry object.
 */
public class RemoteMepEntryCodec extends JsonCodec<RemoteMepEntry> {

    /**
     * Encodes the RemoteMepEntry entity into JSON.
     *
     * @param remoteMepEntry  RemoteMepEntry to encode
     * @param context encoding context
     * @return JSON node
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ObjectNode encode(RemoteMepEntry remoteMepEntry, CodecContext context) {
        checkNotNull(remoteMepEntry, "Mep cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("remoteMepId", remoteMepEntry.remoteMepId().toString())
                .put("remoteMepState", remoteMepEntry.state().name())
                .put("rdi", remoteMepEntry.rdi());

        if (remoteMepEntry.failedOrOkTime() != null) {
            result = result.put("failedOrOkTime",
                    remoteMepEntry.failedOrOkTime().toString());
        }

        if (remoteMepEntry.macAddress() != null) {
            result = result.put("macAddress", remoteMepEntry.macAddress().toString());
        }

        if (remoteMepEntry.portStatusTlvType() != null) {
            result = result.put("portStatusTlvType",
                    remoteMepEntry.portStatusTlvType().name());
        }
        if (remoteMepEntry.interfaceStatusTlvType() != null) {
            result = result.put("interfaceStatusTlvType",
                    remoteMepEntry.interfaceStatusTlvType().name());
        }
        if (remoteMepEntry.senderIdTlvType() != null) {
            result = result.put("senderIdTlvType",
                    remoteMepEntry.senderIdTlvType().name());
        }

        return result;
    }

    /**
     * Encodes the collection of the RemoteMepEntry entities.
     *
     * @param remoteMepEntries collection of RemoteMepEntry to encode
     * @param context  encoding context
     * @return JSON array
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ArrayNode encode(Iterable<RemoteMepEntry> remoteMepEntries,
            CodecContext context) {
        ArrayNode an = context.mapper().createArrayNode();
        remoteMepEntries.forEach(remoteMepEntry ->
                        an.add(encode(remoteMepEntry, context)));
        return an;
    }

}
