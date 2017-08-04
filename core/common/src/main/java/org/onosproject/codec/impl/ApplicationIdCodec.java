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
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * ApplicationId JSON codec.
 */
public final class ApplicationIdCodec extends JsonCodec<ApplicationId> {

    private static final String APP_ID = "id";
    private static final String APP_NAME = "name";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in ApplicationId";

    @Override
    public ObjectNode encode(ApplicationId appId, CodecContext context) {
        checkNotNull(appId, "ApplicationId cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put("id", appId.id())
                .put("name", appId.name());

        return result;
    }

    @Override
    public ApplicationId decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse application identifier
        int id = nullIsIllegal(json.get(APP_ID), APP_ID + MISSING_MEMBER_MESSAGE).asInt();

        // parse application name
        String name = nullIsIllegal(json.get(APP_NAME), APP_NAME + MISSING_MEMBER_MESSAGE).asText();

        return new DefaultApplicationId(id, name);
    }
}
