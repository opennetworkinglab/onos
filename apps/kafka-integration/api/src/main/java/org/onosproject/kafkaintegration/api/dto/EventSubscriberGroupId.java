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
package org.onosproject.kafkaintegration.api.dto;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Wrapper Object for storing the consumer group id. Group ids are used by
 * external applications when consuming events from Kafka server.
 *
 */
public final class EventSubscriberGroupId {
    private final UUID id;

    /**
     * Creates a new Subscriber Group Id.
     *
     * @param uuid representing the group id.
     */
    public EventSubscriberGroupId(UUID uuid) {
        id = checkNotNull(uuid);
    }

    /**
     * Returns the Group Id of the subscriber.
     *
     * @return uuid representing the group id.
     */
    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof EventSubscriberGroupId) {
            EventSubscriberGroupId sub = (EventSubscriberGroupId) o;
            if (sub.id.equals(id)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("subscriberGroupId", id).toString();
    }
}
