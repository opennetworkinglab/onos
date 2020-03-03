/*
 * Copyright 2015-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Flow entry JSON codec.
 */
public final class FlowEntryCodec extends JsonCodec<FlowEntry> {

    public static final String GROUP_ID = "groupId";
    public static final String STATE = "state";
    public static final String LIFE = "life";
    public static final String LIVE_TYPE = "liveType";
    public static final String LAST_SEEN = "lastSeen";
    public static final String PACKETS = "packets";
    public static final String BYTES = "bytes";
    public static final String TREATMENT = "treatment";
    public static final String SELECTOR = "selector";

    @Override
    public ObjectNode encode(FlowEntry flowEntry, CodecContext context) {
        checkNotNull(flowEntry, "Flow entry cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(GROUP_ID, flowEntry.groupId().id())
                .put(STATE, flowEntry.state().toString())
                //FIXME life is destroying precision (seconds granularity is default)
                .put(LIFE, flowEntry.life())
                .put(LIVE_TYPE, flowEntry.liveType().toString())
                .put(LAST_SEEN, flowEntry.lastSeen())
                .put(PACKETS, flowEntry.packets())
                .put(BYTES, flowEntry.bytes())
                // encode FlowRule-specific fields using the FlowRule codec
                .setAll(context.codec(FlowRule.class).encode((FlowRule) flowEntry, context));

        if (flowEntry.treatment() != null) {
            final JsonCodec<TrafficTreatment> treatmentCodec =
                    context.codec(TrafficTreatment.class);
            result.set(TREATMENT, treatmentCodec.encode(flowEntry.treatment(), context));
        }

        if (flowEntry.selector() != null) {
            final JsonCodec<TrafficSelector> selectorCodec =
                    context.codec(TrafficSelector.class);
            result.set(SELECTOR, selectorCodec.encode(flowEntry.selector(), context));
        }

        return result;
    }

    @Override
    public FlowEntry decode(ObjectNode json, CodecContext context) {
        checkNotNull(json, "JSON object cannot be null");

        // decode FlowRule-specific fields using the FlowRule codec
        FlowRule flowRule = context.codec(FlowRule.class).decode(json, context);

        JsonNode stateNode = json.get(STATE);
        FlowEntry.FlowEntryState state = (null == stateNode) ?
                FlowEntry.FlowEntryState.ADDED : FlowEntry.FlowEntryState.valueOf(stateNode.asText());

        JsonNode lifeNode = json.get(LIFE);
        long life = (null == lifeNode) ? 0 : lifeNode.asLong();

        JsonNode liveTypeNode = json.get(LIVE_TYPE);
        FlowEntry.FlowLiveType liveType = (null == liveTypeNode) ?
                FlowEntry.FlowLiveType.UNKNOWN : FlowEntry.FlowLiveType.valueOf(liveTypeNode.asText());

        JsonNode packetsNode = json.get(PACKETS);
        long packets = (null == packetsNode) ? 0 : packetsNode.asLong();

        JsonNode bytesNode = json.get(BYTES);
        long bytes = (null == bytesNode) ? 0 : bytesNode.asLong();

        return new DefaultFlowEntry(flowRule, state, life,
                                    liveType, packets, bytes);
    }

}

