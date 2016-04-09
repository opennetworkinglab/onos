/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.intent.Constraint;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Constraint JSON codec.
 */
public final class ConstraintCodec extends JsonCodec<Constraint> {

    protected static final String MISSING_MEMBER_MESSAGE =
            " member is required in Constraint";
    protected static final String TYPE = "type";
    protected static final String TYPES = "types";
    protected static final String INCLUSIVE = "inclusive";
    protected static final String KEY = "key";
    protected static final String THRESHOLD = "threshold";
    protected static final String BANDWIDTH = "bandwidth";
    protected static final String LAMBDA = "lambda";
    protected static final String LATENCY_MILLIS = "latencyMillis";
    protected static final String OBSTACLES = "obstacles";
    protected static final String WAYPOINTS = "waypoints";

    @Override
    public ObjectNode encode(Constraint constraint, CodecContext context) {
        checkNotNull(constraint, "Constraint cannot be null");

        final EncodeConstraintCodecHelper encodeCodec =
                new EncodeConstraintCodecHelper(constraint, context);

        return encodeCodec.encode();
    }

    @Override
    public Constraint decode(ObjectNode json, CodecContext context) {
        checkNotNull(json, "JSON cannot be null");

        final DecodeConstraintCodecHelper decodeCodec =
                new DecodeConstraintCodecHelper(json);

        return decodeCodec.decode();
    }
}
