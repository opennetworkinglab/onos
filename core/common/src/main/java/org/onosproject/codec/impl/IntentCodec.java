/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import org.onosproject.net.NetworkResource;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Intent JSON codec.
 */
public final class IntentCodec extends JsonCodec<Intent> {

    @Override
    public ObjectNode encode(Intent intent, CodecContext context) {
        checkNotNull(intent, "Intent cannot be null");
        final ObjectNode result = context.mapper().createObjectNode()
                .put("type", intent.getClass().getSimpleName())
                .put("id", intent.id().toString())
                .put("appId", intent.appId().toString())
                .put("details", intent.toString());

        final ArrayNode jsonResources = result.putArray("resources");

        for (final NetworkResource resource : intent.resources()) {
            jsonResources.add(resource.toString());
        }

        IntentService service = context.getService(IntentService.class);
        IntentState state = service.getIntentState(intent.key());
        if (state != null) {
            result.put("state", state.toString());
        }

        return result;
    }
}
