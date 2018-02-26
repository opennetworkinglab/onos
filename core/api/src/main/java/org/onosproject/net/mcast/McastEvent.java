/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.mcast;

import org.onosproject.event.AbstractEvent;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * An entity representing a multicast event. Event either add or remove
 * sinks or sources.
 *
 * @deprecated in 1.11 ("Loon") release. To be moved into an app.
 */
@Deprecated
public class McastEvent extends AbstractEvent<McastEvent.Type, McastRouteInfo> {


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
         * A source for a mcast route (ie. the subject) has been added.
         */
        SOURCE_ADDED,

        /**
         * A source for a mcast route has been updated.
         */
        SOURCE_UPDATED,

        /**
         * A sink for a mcast route (ie. the subject) has been added.
         */
        SINK_ADDED,

        /**
         * A source for a mcast route (ie. the subject) has been removed.
         */
        SINK_REMOVED
    }

    // Used when an update event happens
    private McastRouteInfo prevSubject;

    /**
     * Creates a McastEvent of a given type using the subject.
     *
     * @param type the event type
     * @param subject the subject of the event type
     */
    public McastEvent(McastEvent.Type type, McastRouteInfo subject) {
        super(type, subject);
    }

    /**
     * Creates a McastEvent of a given type using the subject and
     * the previous subject.
     *
     * @param type the event type
     * @param subject the subject of the event
     * @param prevSubject the previous subject of the event
     */
    public McastEvent(McastEvent.Type type, McastRouteInfo subject,
                      McastRouteInfo prevSubject) {
        super(type, subject);
        // For now we have just this kind of updates
        if (type == Type.SOURCE_UPDATED) {
            this.prevSubject = prevSubject;
        }
    }

    /**
     * Gets the previous subject in this mcast event.
     *
     * @return the previous subject, or null if previous subject is not
     *         specified.
     */
    public McastRouteInfo prevSubject() {
        return this.prevSubject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), subject(), prevSubject);
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
                Objects.equals(this.prevSubject, that.prevSubject);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", type())
                .add("info", subject())
                .add("prevInfo", prevSubject())
                .toString();
    }
}
