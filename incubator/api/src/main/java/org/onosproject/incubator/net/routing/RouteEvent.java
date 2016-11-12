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

package org.onosproject.incubator.net.routing;

import org.joda.time.LocalDateTime;
import org.onosproject.event.AbstractEvent;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes an event about a route.
 */
public class RouteEvent extends AbstractEvent<RouteEvent.Type, ResolvedRoute> {

    private final ResolvedRoute prevSubject;

    /**
     * Route event type.
     */
    public enum Type {

        /**
         * Route is new and the next hop is resolved.
         * <p>
         * The subject of this event should be the route being added.
         * The prevSubject of this event should be null.
         */
        ROUTE_ADDED,

        /**
         * Route has updated information.
         * <p>
         * The subject of this event should be the new route.
         * The prevSubject of this event should be the old route.
         */
        ROUTE_UPDATED,

        /**
         * Route was removed or the next hop becomes unresolved.
         * <p>
         * The subject of this event should be the route being removed.
         * The prevSubject of this event should be null.
         */
        ROUTE_REMOVED
    }

    /**
     * Creates a new route event without specifying previous subject.
     *
     * @param type event type
     * @param subject event subject
     */
    public RouteEvent(Type type, ResolvedRoute subject) {
        super(type, subject);
        this.prevSubject = null;
    }

    /**
     * Creates a new route event.
     *
     * @param type event type
     * @param subject event subject
     * @param time event time
     */
    protected RouteEvent(Type type, ResolvedRoute subject, long time) {
        super(type, subject, time);
        this.prevSubject = null;
    }

    /**
     * Creates a new route event with previous subject.
     *
     * @param type event type
     * @param subject event subject
     * @param prevSubject previous subject
     */
    public RouteEvent(Type type, ResolvedRoute subject, ResolvedRoute prevSubject) {
        super(type, subject);
        this.prevSubject = prevSubject;
    }

    /**
     * Returns the previous subject of the event.
     *
     * @return previous subject to which this event pertains
     */
    public ResolvedRoute prevSubject() {
        return prevSubject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject(), type(), prevSubject());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof RouteEvent)) {
            return false;
        }

        RouteEvent that = (RouteEvent) other;

        return Objects.equals(this.subject(), that.subject()) &&
                Objects.equals(this.type(), that.type()) &&
                Objects.equals(this.prevSubject(), that.prevSubject());
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", new LocalDateTime(time()))
                .add("type", type())
                .add("subject", subject())
                .add("prevSubject", prevSubject())
                .toString();
    }
}
