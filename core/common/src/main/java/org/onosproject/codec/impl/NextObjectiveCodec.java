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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.slf4j.Logger;

import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Next Objective Codec.
 */
public class NextObjectiveCodec extends JsonCodec<NextObjective> {

    private final Logger log = getLogger(getClass());

    // JSON field names
    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String OPERATION = "operation";
    private static final String TREATMENTS = "treatments";
    private static final String META = "meta";

    // messages to be printed out
    private static final String MISSING_MEMBER_MESSAGE =
            " member is required in NextObjective";
    private static final String NOT_NULL_MESSAGE =
            "NextObjective cannot be null";
    private static final String INVALID_TYPE_MESSAGE =
            "The requested flag {} is not defined in NextObjective.";
    private static final String INVALID_OP_MESSAGE =
            "The requested operation {} is not defined for NextObjective.";

    public static final String REST_APP_ID = "org.onosproject.rest";

    @Override
    public ObjectNode encode(NextObjective nextObjective, CodecContext context) {

        checkNotNull(nextObjective, NOT_NULL_MESSAGE);

        final JsonCodec<TrafficTreatment> trafficTreatmentCodec = context.codec(TrafficTreatment.class);
        final JsonCodec<TrafficSelector> trafficSelectorCodec = context.codec(TrafficSelector.class);

        // encode common properties
        ObjectiveCodecHelper och = new ObjectiveCodecHelper();
        ObjectNode result = och.encode(nextObjective, context);

        // encode id
        result.put(ID, nextObjective.id());

        // encode type
        result.put(TYPE, nextObjective.type().toString());

        // encode operation
        result.put(OPERATION, nextObjective.op().toString());

        // encode treatments
        ArrayNode treatments = context.mapper().createArrayNode();
        nextObjective.next().forEach(t -> {
            ObjectNode treatmentJson = trafficTreatmentCodec.encode(t, context);
            treatments.add(treatmentJson);
        });
        result.set(TREATMENTS, treatments);

        // encode meta
        if (nextObjective.meta() != null) {
            ObjectNode trafficSelectorNode = trafficSelectorCodec.encode(nextObjective.meta(), context);
            result.set(META, trafficSelectorNode);
        }

        return result;
    }

    @Override
    public NextObjective decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        CoreService coreService = context.getService(CoreService.class);

        final JsonCodec<TrafficSelector> trafficSelectorCodec = context.codec(TrafficSelector.class);
        final JsonCodec<TrafficTreatment> trafficTreatmentCodec = context.codec(TrafficTreatment.class);

        ObjectiveCodecHelper och = new ObjectiveCodecHelper();

        DefaultNextObjective.Builder baseBuilder = DefaultNextObjective.builder();
        final DefaultNextObjective.Builder builder =
                (DefaultNextObjective.Builder) och.decode(json, baseBuilder, context);

        // decode id
        JsonNode idJson = json.get(ID);
        checkNotNull(idJson);
        builder.withId(idJson.asInt());

        // decode application id
        ApplicationId appId = coreService.registerApplication(REST_APP_ID);
        builder.fromApp(appId);

        // decode type
        String typeStr = nullIsIllegal(json.get(TYPE), TYPE + MISSING_MEMBER_MESSAGE).asText();

        switch (typeStr) {
            case "HASHED":
                builder.withType(NextObjective.Type.HASHED);
                break;
            case "BROADCAST":
                builder.withType(NextObjective.Type.BROADCAST);
                break;
            case "FAILOVER":
                builder.withType(NextObjective.Type.FAILOVER);
                break;
            case "SIMPLE":
                builder.withType(NextObjective.Type.SIMPLE);
                break;
            default:
                log.warn(INVALID_TYPE_MESSAGE, typeStr);
                return null;
        }

        // decode treatments
        JsonNode treatmentsJson = json.get(TREATMENTS);
        checkNotNull(treatmentsJson);
        if (treatmentsJson != null) {
            IntStream.range(0, treatmentsJson.size()).forEach(i -> {
                ObjectNode treatmentJson = get(treatmentsJson, i);
                builder.addTreatment(trafficTreatmentCodec.decode(treatmentJson, context));
            });
        }

        // decode meta
        JsonNode metaJson = json.get(META);
        if (metaJson != null) {
            TrafficSelector trafficSelector = trafficSelectorCodec.decode((ObjectNode) metaJson, context);
            builder.withMeta(trafficSelector);
        }

        // decode operation
        String opStr = nullIsIllegal(json.get(OPERATION), OPERATION + MISSING_MEMBER_MESSAGE).asText();
        NextObjective nextObjective;

        switch (opStr) {
            case "ADD":
                nextObjective = builder.add();
                break;
            case "REMOVE":
                nextObjective = builder.remove();
                break;
            default:
                log.warn(INVALID_OP_MESSAGE, opStr);
                return null;
        }

        return nextObjective;
    }
}
