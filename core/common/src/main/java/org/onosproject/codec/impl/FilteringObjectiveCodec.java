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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.slf4j.Logger;

import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Filtering Objective Codec.
 */
public final class FilteringObjectiveCodec extends JsonCodec<FilteringObjective> {
    private final Logger log = getLogger(getClass());

    // JSON field names
    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String KEY = "key";
    private static final String META = "meta";
    private static final String APP_ID = "appId";
    private static final String OPERATION = "operation";
    private static final String CONDITIONS = "conditions";

    // messages to be printed out
    private static final String MISSING_MEMBER_MESSAGE =
            " member is required in FilteringObjective";
    private static final String NOT_NULL_MESSAGE =
            "FilteringObjective cannot be null";

    public static final String REST_APP_ID = "org.onosproject.rest";

    @Override
    public ObjectNode encode(FilteringObjective filteringObjective, CodecContext context) {

        checkNotNull(filteringObjective, NOT_NULL_MESSAGE);

        final JsonCodec<Criterion> criterionCodec = context.codec(Criterion.class);
        final JsonCodec<TrafficTreatment> trafficTreatmentCodec = context.codec(TrafficTreatment.class);

        // encode common properties
        ObjectiveCodecHelper och = new ObjectiveCodecHelper();
        ObjectNode result = och.encode(filteringObjective, context);

        // encode id
        result.put(ID, filteringObjective.id());

        // encode type
        result.put(TYPE, filteringObjective.type().toString());

        // encode key
        if (filteringObjective.key() != null) {
            ObjectNode criterionNode = criterionCodec.encode(filteringObjective.key(), context);
            result.set(KEY, criterionNode);
        }

        // encode meta
        if (filteringObjective.meta() != null) {
            ObjectNode trafficTreatmentNode = trafficTreatmentCodec.encode(filteringObjective.meta(), context);
            result.set(META, trafficTreatmentNode);
        }

        // encode conditions
        ArrayNode conditions = context.mapper().createArrayNode();
        filteringObjective.conditions().forEach(c -> {
            ObjectNode criterionJson = criterionCodec.encode(c, context);
            conditions.add(criterionJson);
        });
        result.set(CONDITIONS, conditions);

        return result;
    }

    @Override
    public FilteringObjective decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        CoreService coreService = context.getService(CoreService.class);

        final JsonCodec<Criterion> criterionCodec = context.codec(Criterion.class);
        final JsonCodec<TrafficTreatment> trafficTreatmentCodec = context.codec(TrafficTreatment.class);

        ObjectiveCodecHelper och = new ObjectiveCodecHelper();

        DefaultFilteringObjective.Builder baseBuilder = DefaultFilteringObjective.builder();
        final DefaultFilteringObjective.Builder builder =
                (DefaultFilteringObjective.Builder) och.decode(json, baseBuilder, context);



        // application id
        JsonNode appIdJson = json.get(APP_ID);
        String appId = appIdJson != null ? appIdJson.asText() : REST_APP_ID;
        builder.fromApp(coreService.registerApplication(appId));

        // decode type
        String typeStr = nullIsIllegal(json.get(TYPE), TYPE + MISSING_MEMBER_MESSAGE).asText();

        switch (typeStr) {
            case "PERMIT":
                builder.permit();
                break;
            case "DENY":
                builder.deny();
                break;
            default:
                throw new IllegalArgumentException("The requested type " + typeStr +
                " is not defined for FilteringObjective.");
        }

        // decode key
        JsonNode keyJson = json.get(KEY);
        if (keyJson != null) {
            Criterion key = criterionCodec.decode((ObjectNode) keyJson, context);
            builder.withKey(key);
        }

        // decode conditions
        JsonNode conditionsJson = json.get(CONDITIONS);
        checkNotNull(conditionsJson);
        if (conditionsJson != null) {
            IntStream.range(0, conditionsJson.size()).forEach(i -> {
                ObjectNode conditionJson = get(conditionsJson, i);
                builder.addCondition(criterionCodec.decode(conditionJson, context));
            });
        }

        // decode meta
        JsonNode metaJson = json.get(META);
        if (metaJson != null) {
            TrafficTreatment trafficTreatment = trafficTreatmentCodec.decode((ObjectNode) metaJson, context);
            builder.withMeta(trafficTreatment);
        }

        // decode operation
        String opStr = nullIsIllegal(json.get(OPERATION), OPERATION + MISSING_MEMBER_MESSAGE).asText();
        FilteringObjective filteringObjective;

        switch (opStr) {
            case "ADD":
                filteringObjective = builder.add();
                break;
            case "REMOVE":
                filteringObjective = builder.remove();
                break;
            default:
                throw new IllegalArgumentException("The requested operation " + opStr +
                " is not defined for FilteringObjective.");
        }

        return filteringObjective;
    }
}
