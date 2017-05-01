/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.ofagent.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFController;
import org.onosproject.ofagent.impl.DefaultOFAgent;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * OpenFlow agent JSON codec.
 */
public final class OFAgentCodec extends JsonCodec<OFAgent> {

    @Override
    public ObjectNode encode(OFAgent ofAgent, CodecContext context) {
        checkNotNull(ofAgent, "OFAgent cannot be null");

        ObjectMapper mapper = context.mapper();
        ObjectNode ofAgentNode = mapper.createObjectNode();
        ofAgentNode
                .put("networkId", ofAgent.networkId().toString())
                .put("state", ofAgent.state().toString());

        ArrayNode controllers = mapper.createArrayNode();
        ofAgent.controllers().forEach(ofController -> controllers.add((new OFControllerCodec()).encode(ofController,
                                                                                                       context)));
        ofAgentNode.set("controllers", controllers);

        return ofAgentNode;
    }

    public OFAgent decode(ObjectNode json, CodecContext context) {
        JsonNode networkId = json.get("networkId");
        checkNotNull(networkId);

        checkNotNull(json.get("controllers"));
        checkState(json.get("controllers").isArray());
        Set<OFController> controllers = Sets.newHashSet();
        json.get("controllers").forEach(jsonController -> controllers.add((new
                OFControllerCodec()).decode((ObjectNode) jsonController, context)));

        return DefaultOFAgent.builder()
                .networkId(NetworkId.networkId(networkId.asLong()))
                .controllers(controllers)
                .state(OFAgent.State.STOPPED)
                .build();
    }

}
