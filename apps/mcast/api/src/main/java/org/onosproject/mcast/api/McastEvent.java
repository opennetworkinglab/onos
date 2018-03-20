/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.mcast.api;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * An entity representing a multicast event. Event either add or remove
 * sinks or sources.
 */
@Beta
public class McastEvent extends AbstractEvent<McastEvent.Type, McastRouteUpdate> {

    /**
     * Mcast Event type enum.
     */
    public enum Type {
        /**
         * A new mcast route has been added.
         */
        ROUTE_ADDED,

        /**
         * A mcast route has been removed.
         */
        ROUTE_REMOVED,

        /**
         * A set of sources for a mcast route (ie. the subject) has been added.
         */
        SOURCES_ADDED,

        /**
         * A set of sources for a mcast route has been removed.
         */
        SOURCES_REMOVED,

        /**
         * A set of sinks for a mcast route (ie. the subject) has been added.
         */
        SINKS_ADDED,

        /**
         * A set of sinks for a mcast route (ie. the subject) has been removed.
         */
        SINKS_REMOVED
    }

    private McastRouteUpdate prevSubject;

    /**
     * Creates a McastEvent of a given type using the subject.
     *
     * @param type        the event type
     * @param prevSubject the previous mcast information
     * @param subject     the current mcast information
     */
    public McastEvent(McastEvent.Type type, McastRouteUpdate prevSubject, McastRouteUpdate subject) {
        super(type, subject);
        this.prevSubject = prevSubject;
    }

    /**
     * Gets the previous subject in this Mcast event.
     *
     * @return the previous subject, or null if previous subject is not
     * specified.
     */
    public McastRouteUpdate prevSubject() {
        return this.prevSubject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), subject(), prevSubject());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof McastEvent)) {
            return false;
        }

        McastEvent that = (McastEvent) other;

        return Objects.equals(this.subject(), that.subject()) &&
                Objects.equals(this.type(), that.type()) &&
                Objects.equals(this.prevSubject(), that.prevSubject());
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", type())
                .add("prevSubject", prevSubject())
                .add("subject", subject())
                .toString();
    }
}
