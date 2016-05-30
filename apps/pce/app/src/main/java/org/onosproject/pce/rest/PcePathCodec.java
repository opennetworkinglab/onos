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
package org.onosproject.pce.rest;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.pce.pceservice.PcePath;
import org.onosproject.pce.pceservice.DefaultPcePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * PCE path json codec.
 */
public final class PcePathCodec extends JsonCodec<PcePath> {
    private final Logger log = LoggerFactory.getLogger(PcePathCodec.class);
    private static final String SOURCE = "source";
    private static final String DESTINATION = "destination";
    private static final String LSP_TYPE = "pathType";
    private static final String SYMBOLIC_PATH_NAME = "name";
    private static final String CONSTRAINT = "constraint";
    private static final String COST = "cost";
    private static final String BANDWIDTH = "bandwidth";
    private static final String PATH_ID = "pathId";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in pce-path";

    @Override
    public PcePath decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            log.error("Empty json input");
            return null;
        }

        // build pce-path
        PcePath.Builder resultBuilder = new DefaultPcePath.Builder();

        // retrieve source
        JsonNode jNode = json.get(SOURCE);
        if (jNode != null) {
            String src = jNode.asText();
            resultBuilder.source(src);
        }

        // retrieve destination
        jNode = json.get(DESTINATION);
        if (jNode != null) {
            String dst = jNode.asText();
            resultBuilder.destination(dst);
        }

        // retrieve lsp-type
        jNode = json.get(LSP_TYPE);
        if (jNode != null) {
            String lspType = jNode.asText();
            resultBuilder.lspType(lspType);
        }

        // retrieve symbolic-path-name
        jNode = json.get(SYMBOLIC_PATH_NAME);
        if (jNode != null) {
            String name = jNode.asText();
            resultBuilder.name(name);
        }

        // retrieve constraint
        JsonNode constraintJNode = (JsonNode) json.path(CONSTRAINT);
        if ((constraintJNode != null) && (!constraintJNode.isMissingNode())) {
            // retrieve cost
            jNode = constraintJNode.get(COST);
            if (jNode != null) {
                String cost = jNode.asText();
                resultBuilder.costConstraint(cost);
            }

            // retrieve bandwidth
            jNode = constraintJNode.get(BANDWIDTH);
            if (jNode != null) {
                String bandwidth = jNode.asText();
                resultBuilder.bandwidthConstraint(bandwidth);
            }
        }

        return resultBuilder.build();
    }

    @Override
    public ObjectNode encode(PcePath path, CodecContext context) {
        checkNotNull(path, "path output cannot be null");
        ObjectNode result = context.mapper()
                .createObjectNode()
                .put(PATH_ID, path.id().id())
                .put(SOURCE, path.source())
                .put(DESTINATION, path.destination())
                .put(LSP_TYPE, path.lspType().type())
                .put(SYMBOLIC_PATH_NAME, path.name());

        ObjectNode constraintNode = context.mapper()
                .createObjectNode()
                .put(COST, path.costConstraint().toString())
                .put(BANDWIDTH, path.bandwidthConstraint().toString());

        result.set(CONSTRAINT, constraintNode);
        return result;
    }
}
