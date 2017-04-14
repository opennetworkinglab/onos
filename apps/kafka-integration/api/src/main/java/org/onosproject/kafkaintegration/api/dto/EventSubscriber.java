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
package org.onosproject.kafkaintegration.api.dto;
import org.onosproject.kafkaintegration.api.dto.OnosEvent.Type;

/**
 * Abstraction of subscription to an event type.
 */
public interface EventSubscriber {
    /**
     * Returns the application name.
     *
     * @return application name.
     */
    String appName();

    /**
     * Returns the subscriber group ID.
     * @return subscriber group ID.
     */
    EventSubscriberGroupId subscriberGroupId();

    /**
     * Returns the Event type.
     *
     * @return ONOS Event Type
     */
    Type eventType();

    /**
     * An event subscriber builder.
     */
    interface Builder {
        Builder setAppName(String appName);

        Builder setSubscriberGroupId(EventSubscriberGroupId subscriberGroupId);

        Builder setEventType(Type eventType);

        EventSubscriber build();
    }
}
