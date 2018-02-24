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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;

import java.util.BitSet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encode and decode to/from JSON to MepLtCreate object.
 */
public class MepLtCreateCodec extends JsonCodec<MepLtCreate> {

    private static final String REMOTE_MEP_ID = "remoteMepId";
    private static final String REMOTE_MEP_MAC = "remoteMepMac";
    private static final String DEFAULT_TTL = "defaultTtl";
    private static final String TRANSMIT_LTM_FLAGS = "transmitLtmFlags";
    private static final String LINKTRACE = "linktrace";
    private static final String USE_FDB_ONLY = "use-fdb-only";

    /**
     * Encodes the MepLtCreate entity into JSON.
     *
     * @param mepLtCreate MepLtCreate to encode
     * @param context encoding context
     * @return JSON node
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ObjectNode encode(MepLtCreate mepLtCreate, CodecContext context) {
        checkNotNull(mepLtCreate, "Mep Lt Create cannot be null");
        ObjectNode result = context.mapper().createObjectNode();

        if (mepLtCreate.remoteMepId() != null) {
            result.put(REMOTE_MEP_ID, mepLtCreate.remoteMepId().value());
        } else {
            result.put(REMOTE_MEP_MAC, mepLtCreate.remoteMepAddress().toString());
        }

        if (mepLtCreate.defaultTtl() != null) {
            result.put(DEFAULT_TTL, mepLtCreate.defaultTtl());
        }
        if (mepLtCreate.transmitLtmFlags() != null) {
            result.put(TRANSMIT_LTM_FLAGS,
                    mepLtCreate.transmitLtmFlags().get(0) ? USE_FDB_ONLY : "");
        }

        return result;
    }

    /**
     * Decodes the MepLtCreate entity from JSON.
     *
     * @param json    JSON to decode
     * @param context decoding context
     * @return decoded MepLtCreate
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support decode operations
     */
    @Override
    public MepLtCreate decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        JsonNode linktraceNode = json.get(LINKTRACE);

        JsonNode remoteMepIdNode = linktraceNode.get(REMOTE_MEP_ID);
        JsonNode remoteMepMacNode = linktraceNode.get(REMOTE_MEP_MAC);

        MepLtCreate.MepLtCreateBuilder ltCreateBuilder;
        if (remoteMepIdNode != null) {
            MepId remoteMepId = MepId.valueOf((short) remoteMepIdNode.asInt());
            ltCreateBuilder = DefaultMepLtCreate.builder(remoteMepId);
        } else if (remoteMepMacNode != null) {
            MacAddress remoteMepMac = MacAddress.valueOf(
                                            remoteMepMacNode.asText());
            ltCreateBuilder = DefaultMepLtCreate.builder(remoteMepMac);
        } else {
            throw new IllegalArgumentException(
                    "Either a remoteMepId or a remoteMepMac");
        }

        JsonNode defaultTtlNode = linktraceNode.get(DEFAULT_TTL);
        if (defaultTtlNode != null) {
            short defaultTtl = (short) defaultTtlNode.asInt();
            ltCreateBuilder.defaultTtl(defaultTtl);
        }

        JsonNode transmitLtmFlagsNode = linktraceNode.get(TRANSMIT_LTM_FLAGS);
        if (transmitLtmFlagsNode != null) {
            if (transmitLtmFlagsNode.asText().isEmpty()) {
                ltCreateBuilder.transmitLtmFlags(BitSet.valueOf(new long[]{0}));
            } else if (transmitLtmFlagsNode.asText().equals(USE_FDB_ONLY)) {
               ltCreateBuilder.transmitLtmFlags(BitSet.valueOf(new long[]{1}));
            } else {
                throw new IllegalArgumentException("Expecting value 'use-fdb-only' " +
                        "or '' for " + TRANSMIT_LTM_FLAGS);
            }
        }

        return ltCreateBuilder.build();
    }
}
