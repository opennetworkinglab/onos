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

import java.util.stream.IntStream;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Traffic selector codec.
 */
public final class TrafficSelectorCodec extends JsonCodec<TrafficSelector> {
    private static final String CRITERIA = "criteria";

    @Override
    public ObjectNode encode(TrafficSelector selector, CodecContext context) {
        checkNotNull(selector, "Traffic selector cannot be null");

        final ObjectNode result = context.mapper().createObjectNode();
        final ArrayNode jsonCriteria = result.putArray(CRITERIA);

        if (selector.criteria() != null) {
            final JsonCodec<Criterion> criterionCodec =
                    context.codec(Criterion.class);
            for (final Criterion criterion : selector.criteria()) {
                jsonCriteria.add(criterionCodec.encode(criterion, context));
            }
        }

        return result;
    }

    @Override
    public TrafficSelector decode(ObjectNode json, CodecContext context) {
        final JsonCodec<Criterion> criterionCodec =
                context.codec(Criterion.class);

        JsonNode criteriaJson = json.get(CRITERIA);
        TrafficSelector.Builder builder = DefaultTrafficSelector.builder();
        if (criteriaJson != null) {
            IntStream.range(0, criteriaJson.size())
                    .forEach(i -> builder.add(
                            criterionCodec.decode(get(criteriaJson, i),
                                    context)));
        }
        return builder.build();
    }
}
