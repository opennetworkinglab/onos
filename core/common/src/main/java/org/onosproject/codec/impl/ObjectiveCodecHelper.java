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
import org.onosproject.codec.CodecContext;
import org.onosproject.net.flowobjective.Objective;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Objective Codec Helper.
 */
public class ObjectiveCodecHelper {

    // JSON field names
    private static final String ID = "id";
    private static final String APP_ID = "appId";
    private static final String OPERATION = "operation";
    private static final String PERMANENT = "isPermanent";
    private static final String PRIORITY = "priority";
    private static final String TIMEOUT = "timeout";
    public static final String REST_APP_ID = "org.onosproject.rest";

    public ObjectNode encode(Objective objective, CodecContext context) {
        checkNotNull(objective, "Objective cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, objective.id())
                .put(OPERATION, objective.op().toString())
                .put(PERMANENT, String.valueOf(objective.permanent()))
                .put(PRIORITY, objective.priority())
                .put(TIMEOUT, objective.timeout());

        if (objective.appId() != null) {
            result.put(APP_ID, objective.appId().toString());
        }

        return result;
    }

    public Objective.Builder decode(ObjectNode json, Objective.Builder builder, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // permanent
        boolean permanent = false;
        if (json.get(PERMANENT) != null) {
            permanent = json.get(PERMANENT).asBoolean();
        }

        // timeout
        int timeoutInt = 0;
        if (json.get(TIMEOUT) != null) {
            timeoutInt = json.get(TIMEOUT).asInt();
        }

        // priority
        int priorityInt = 0;
        if (json.get(PRIORITY) != null) {
            priorityInt = json.get(PRIORITY).asInt();
        }

        if (permanent) {
            builder.makePermanent()
                    .withPriority(priorityInt);
        } else {
            builder.makeTemporary(timeoutInt)
                    .withPriority(priorityInt);
        }
        return builder;
    }
}
