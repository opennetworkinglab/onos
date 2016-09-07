/**
 * Copyright 2016-present Open Networking Laboratory
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.kafkaintegration.api.dto;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onosproject.kafkaintegration.api.dto.OnosEvent.Type;

/**
 * Representation of a subscription to an event type.
 *
 */
public final class DefaultEventSubscriber implements EventSubscriber {
    private final String appName;
    private final EventSubscriberGroupId subscriberGroupId;
    private final Type eventType;

    /**
     * Creates a new Event Subscriber.
     *
     * @param name Application Name
     * @param groupId Subscriber group id of the application
     * @param eventType ONOS event type
     */
    public DefaultEventSubscriber(String name, EventSubscriberGroupId groupId,
                                  Type eventType) {
        this.appName = checkNotNull(name);
        this.subscriberGroupId = checkNotNull(groupId);
        this.eventType = checkNotNull(eventType);
    }

    @Override
    public String appName() {
        return appName;
    }

    @Override
    public EventSubscriberGroupId subscriberGroupId() {
        return subscriberGroupId;
    }

    @Override
    public Type eventType() {
        return eventType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, subscriberGroupId, eventType);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DefaultEventSubscriber) {
            DefaultEventSubscriber sub = (DefaultEventSubscriber) o;
            if (sub.appName.equals(appName)
                    && sub.subscriberGroupId.equals(subscriberGroupId)
                    && sub.eventType.equals(eventType)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("appName", appName)
                .addValue(subscriberGroupId.toString())
                .add("eventType", eventType).toString();
    }

    /**
     * To create an instance of the builder.
     *
     * @return instance of builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for Event subscriber.
     */
    public static final class Builder implements EventSubscriber.Builder {
        private String appName;
        private EventSubscriberGroupId subscriberGroupId;
        private Type eventType;

        @Override
        public Builder setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        @Override
        public Builder setSubscriberGroupId(EventSubscriberGroupId subscriberGroupId) {
            this.subscriberGroupId = subscriberGroupId;
            return this;
        }

        @Override
        public Builder setEventType(Type eventType) {
            this.eventType = eventType;
            return this;
        }

        @Override
        public EventSubscriber build() {
            checkNotNull(appName, "App name cannot be null");
            checkNotNull(subscriberGroupId,
                         "Subscriber group ID cannot " + "be " + "null");
            checkNotNull(eventType, "Event type cannot be null");

            return new DefaultEventSubscriber(appName, subscriberGroupId,
                                              eventType);
        }
    }

}
