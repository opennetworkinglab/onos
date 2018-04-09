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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Forwarding Objective Codec.
 */
public final class ForwardingObjectiveCodec extends JsonCodec<ForwardingObjective> {
    private final Logger log = getLogger(getClass());

    // JSON field names
    private static final String ID = "id";
    private static final String APP_ID = "appId";
    private static final String SELECTOR = "selector";
    private static final String FLAG = "flag";
    private static final String OPERATION = "operation";
    private static final String NEXT_ID = "nextId";
    private static final String TREATMENT = "treatment";

    // messages to be printed out
    private static final String MISSING_MEMBER_MESSAGE =
            " member is required in ForwardingObjective";
    private static final String NOT_NULL_MESSAGE =
            "ForwardingObjective cannot be null";

    public static final String REST_APP_ID = "org.onosproject.rest";

    @Override
    public ObjectNode encode(ForwardingObjective forwardingObjective, CodecContext context) {

        checkNotNull(forwardingObjective, NOT_NULL_MESSAGE);

        final JsonCodec<TrafficTreatment> trafficTreatmentCodec = context.codec(TrafficTreatment.class);
        final JsonCodec<TrafficSelector> trafficSelectorCodec = context.codec(TrafficSelector.class);

        // encode common properties
        ObjectiveCodecHelper och = new ObjectiveCodecHelper();
        ObjectNode result = och.encode(forwardingObjective, context);

        // encode id
        result.put(ID, forwardingObjective.id());

        // encode flag
        result.put(FLAG, forwardingObjective.flag().toString());

        // encode op
        result.put(OPERATION, forwardingObjective.op().toString());

        // encode selector
        ObjectNode trafficSelectorNode =
                trafficSelectorCodec.encode(forwardingObjective.selector(), context);
        result.set(SELECTOR, trafficSelectorNode);

        // encode nextId
        if (forwardingObjective.nextId() != null) {
            result.put(NEXT_ID, forwardingObjective.nextId());
        }

        // encode treatment
        if (forwardingObjective.treatment() != null) {
            ObjectNode trafficTreatmentNode =
                    trafficTreatmentCodec.encode(forwardingObjective.treatment(), context);
            result.set(TREATMENT, trafficTreatmentNode);
        }

        return result;
    }

    @Override
    public ForwardingObjective decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        CoreService coreService = context.getService(CoreService.class);

        final JsonCodec<TrafficTreatment> trafficTreatmentCodec = context.codec(TrafficTreatment.class);
        final JsonCodec<TrafficSelector> trafficSelectorCodec = context.codec(TrafficSelector.class);

        ObjectiveCodecHelper och = new ObjectiveCodecHelper();

        DefaultForwardingObjective.Builder baseBuilder = DefaultForwardingObjective.builder();
        final DefaultForwardingObjective.Builder builder =
                (DefaultForwardingObjective.Builder) och.decode(json, baseBuilder, context);

        // application id
        JsonNode appIdJson = json.get(APP_ID);
        String appId = appIdJson != null ? appIdJson.asText() : REST_APP_ID;
        builder.fromApp(coreService.registerApplication(appId));

        // decode flag
        String flagStr = nullIsIllegal(json.get(FLAG), FLAG + MISSING_MEMBER_MESSAGE).asText();
        switch (flagStr) {
            case "SPECIFIC":
                builder.withFlag(ForwardingObjective.Flag.SPECIFIC);
                break;
            case "VERSATILE":
                builder.withFlag(ForwardingObjective.Flag.VERSATILE);
                break;
            case "EGRESS":
                builder.withFlag(ForwardingObjective.Flag.EGRESS);
                break;
            default:
                throw new IllegalArgumentException("The requested flag " + flagStr +
                " is not defined for ForwardingObjective.");
        }

        // decode selector
        JsonNode selectorJson = json.get(SELECTOR);
        if (selectorJson != null) {
            TrafficSelector trafficSelector = trafficSelectorCodec.decode((ObjectNode) selectorJson, context);
            builder.withSelector(trafficSelector);
        }

        // decode treatment
        JsonNode treatmentJson = json.get(TREATMENT);
        if (treatmentJson != null) {
            TrafficTreatment trafficTreatment = trafficTreatmentCodec.decode((ObjectNode) treatmentJson, context);
            builder.withTreatment(trafficTreatment);
        }

        // decode nextId
        JsonNode nextIdJson = json.get(NEXT_ID);
        if (nextIdJson != null) {
            builder.nextStep(nextIdJson.asInt());
        }

        // decode operation
        String opStr = nullIsIllegal(json.get(OPERATION), OPERATION + MISSING_MEMBER_MESSAGE).asText();
        ForwardingObjective forwardingObjective = null;

        switch (opStr) {
            case "ADD":
                forwardingObjective = builder.add();
                break;
            case "REMOVE":
                forwardingObjective = builder.remove();
                break;
            default:
                throw new IllegalArgumentException("The requested operation " + opStr +
                " is not defined for FilteringObjective.");
        }

        return forwardingObjective;
    }
}
