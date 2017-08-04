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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.mastership.MastershipTerm;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Codec for mastership term.
 */
public class MastershipTermCodec extends JsonCodec<MastershipTerm> {

    // JSON field names
    private static final String MASTER = "master";
    private static final String TERM_NUMBER = "termNumber";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in MastershipTerm";

    @Override
    public ObjectNode encode(MastershipTerm term, CodecContext context) {
        checkNotNull(term, "Mastership term cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(MASTER, term.master().id())
                .put(TERM_NUMBER, term.termNumber());

        return result;
    }

    @Override
    public MastershipTerm decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // node identifier of master
        NodeId nodeId = NodeId.nodeId(nullIsIllegal(json.get(MASTER),
                MASTER + MISSING_MEMBER_MESSAGE).asText());

        // term number
        long termNumber = nullIsIllegal(json.get(TERM_NUMBER),
                TERM_NUMBER + MISSING_MEMBER_MESSAGE).asLong();

        return MastershipTerm.of(nodeId, termNumber);
    }
}
