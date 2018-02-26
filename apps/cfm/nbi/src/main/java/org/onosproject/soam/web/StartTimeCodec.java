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
package org.onosproject.soam.web;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.soam.StartTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Encode and decode to/from JSON to StartTime object.
 */
public class StartTimeCodec extends JsonCodec<StartTime> {

    @Override
    public StartTime decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        if (json.get("immediate") != null) {
            return StartTime.immediate();
        } else if (json.get("absolute") != null) {
            if (json.get("absolute").get("start-time") != null) {
                return StartTime.absolute(OffsetDateTime
                        .parse(json.get("absolute").get("start-time").asText())
                        .toInstant());
            }
            throw new IllegalArgumentException("StartTime absolute must contain "
                    + "a start-time in date-time format with offset");
        } else if (json.get("relative") != null) {
            if (json.get("relative").get("start-time") != null) {
                return StartTime.relative(Duration.parse(json.get("relative").get("start-time").asText()));
            }
            throw new IllegalArgumentException("StartTime relative must contain a start-time duration");
        } else {
            throw new IllegalArgumentException("StartTime must be either immediate, absolute or relative");
        }
    }
}
