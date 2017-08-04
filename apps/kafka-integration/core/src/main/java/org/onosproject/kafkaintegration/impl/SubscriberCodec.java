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
package org.onosproject.kafkaintegration.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kafkaintegration.api.dto.DefaultEventSubscriber;
import org.onosproject.kafkaintegration.api.dto.EventSubscriber;
import org.onosproject.kafkaintegration.api.dto.EventSubscriberGroupId;
import org.onosproject.kafkaintegration.api.dto.OnosEvent.Type;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Codec for encoding/decoding a Subscriber object to/from JSON.
 *
 */
public final class SubscriberCodec extends JsonCodec<EventSubscriber> {

    // JSON field names
    private static final String NAME = "appName";
    private static final String GROUP_ID = "groupId";
    private static final String EVENT_TYPE = "eventType";

    @Override
    public ObjectNode encode(EventSubscriber data, CodecContext context) {
        checkNotNull(data, "Subscriber cannot be null");
        return context.mapper().createObjectNode().put(NAME, data.appName())
                .put(GROUP_ID, data.subscriberGroupId().getId().toString())
                .put(EVENT_TYPE, data.eventType().toString());
    }

    @Override
    public EventSubscriber decode(ObjectNode json, CodecContext context) {

        EventSubscriber.Builder resultBuilder = new DefaultEventSubscriber
                .Builder();
        String appName = json.get(NAME).asText();
        resultBuilder.setAppName(appName);

        String subscriberGroupId = json.get(GROUP_ID).asText();
        resultBuilder.setSubscriberGroupId(new EventSubscriberGroupId(UUID.
                fromString(subscriberGroupId)));

        String eventType = json.get(EVENT_TYPE).asText();
        resultBuilder.setEventType(Type.valueOf(eventType));
        return resultBuilder.build();
    }
}
