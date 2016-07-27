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
import org.onosproject.core.CoreService;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.UrlEscapers;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Intent JSON codec.
 */
public final class IntentCodec extends JsonCodec<Intent> {

    protected static final String TYPE = "type";
    protected static final String ID = "id";
    protected static final String APP_ID = "appId";
    protected static final String STATE = "state";
    protected static final String PRIORITY = "priority";
    protected static final String RESOURCES = "resources";
    protected static final String MISSING_MEMBER_MESSAGE =
            " member is required in Intent";

    @Override
    public ObjectNode encode(Intent intent, CodecContext context) {
        checkNotNull(intent, "Intent cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put(TYPE, intent.getClass().getSimpleName())
                .put(ID, intent.id().toString())
                .put(APP_ID, UrlEscapers.urlPathSegmentEscaper()
                        .escape(intent.appId().name()));

        final ArrayNode jsonResources = result.putArray(RESOURCES);

        for (final NetworkResource resource : intent.resources()) {
            jsonResources.add(resource.toString());
        }

        IntentService service = context.getService(IntentService.class);
        IntentState state = service.getIntentState(intent.key());
        if (state != null) {
            result.put(STATE, state.toString());
        }

        return result;
    }

    @Override
    public Intent decode(ObjectNode json, CodecContext context) {
        checkNotNull(json, "JSON cannot be null");

        String type = nullIsIllegal(json.get(TYPE),
                TYPE + MISSING_MEMBER_MESSAGE).asText();

        if (type.equals(PointToPointIntent.class.getSimpleName())) {
            return context.codec(PointToPointIntent.class).decode(json, context);
        } else if (type.equals(HostToHostIntent.class.getSimpleName())) {
            return context.codec(HostToHostIntent.class).decode(json, context);
        } else if (type.equals(SinglePointToMultiPointIntent.class.getSimpleName())) {
            return context.codec(SinglePointToMultiPointIntent.class).decode(json, context);
        }

        throw new IllegalArgumentException("Intent type "
                + type + " is not supported");
    }

    /**
     * Extracts base intent specific attributes from a JSON object
     * and adds them to a builder.
     *
     * @param json root JSON object
     * @param context code context
     * @param builder builder to use for storing the attributes
     */
    public static void intentAttributes(ObjectNode json, CodecContext context,
                                    Intent.Builder builder) {
        String appId = nullIsIllegal(json.get(IntentCodec.APP_ID),
                IntentCodec.APP_ID + IntentCodec.MISSING_MEMBER_MESSAGE).asText();
        CoreService service = context.getService(CoreService.class);
        builder.appId(service.getAppId(appId));

        JsonNode priorityJson = json.get(IntentCodec.PRIORITY);
        if (priorityJson != null) {
            builder.priority(priorityJson.asInt());
        }
    }
}
