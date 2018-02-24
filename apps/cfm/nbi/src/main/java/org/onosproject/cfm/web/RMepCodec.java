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

import static org.onlab.util.Tools.nullIsIllegal;

import java.util.ArrayList;
import java.util.List;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Encode and decode to/from JSON to RMep object.
 */
public class RMepCodec extends JsonCodec<MepId> {

    /**
     * Encodes the MepId entity into JSON.
     *
     * @param rmep  MepId to encode
     * @param context encoding context
     * @return JSON node
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ObjectNode encode(MepId rmep, CodecContext context) {
        return context.mapper().createObjectNode().put("rmep", rmep.id());
    }

    /**
     * Encodes the collection of the MepId entities.
     *
     * @param rmeps collection of MepId to encode
     * @param context  encoding context
     * @return JSON array
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ArrayNode encode(Iterable<MepId> rmeps, CodecContext context) {
        ArrayNode an = context.mapper().createArrayNode();
        rmeps.forEach(rmep -> an.add(encode(rmep, context)));
        return an;
    }

    /**
     * Decodes the MepId entity from JSON.
     *
     * @param json    JSON to decode
     * @param context decoding context
     * @return decoded MepId
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support decode operations
     */
    @Override
    public MepId decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        JsonNode rmepNode = json.get("rmep");

        return MepId.valueOf((short)
                    nullIsIllegal(rmepNode, "rmep is required").asInt());

    }

    /**
     * Decodes the MepId JSON array into a collection of entities.
     *
     * @param json    JSON array to decode
     * @param context decoding context
     * @return collection of decoded MepId
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support decode operations
     */
    @Override
    public List<MepId> decode(ArrayNode json, CodecContext context) {
        List<MepId> rmepList = new ArrayList<>();
        json.forEach(node -> rmepList.add(decode((ObjectNode) node, context)));
        return rmepList;
    }
}
