/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.host;

import org.joda.time.LocalDateTime;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.Host;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes end-station host event.
 */
public class HostEvent extends AbstractEvent<HostEvent.Type, Host> {

    /**
     * Type of host events.
     */
    public enum Type {
        /**
         * Signifies that a new host has been detected.
         */
        HOST_ADDED,

        /**
         * Signifies that a host has been removed.
         */
        HOST_REMOVED,

        /**
         * Signifies that host data changed, e.g. IP address
         */
        HOST_UPDATED,

        /**
         * Signifies that a host location has changed.
         */
        HOST_MOVED
    }

    private Host prevSubject;

    /**
     * Creates an event of a given type and for the specified host and the
     * current time.
     *
     * @param type host event type
     * @param host event host subject
     */
    public HostEvent(Type type, Host host) {
        super(type, host);
    }

    /**
     * Creates an event of a given type and for the specified host and time.
     *
     * @param type host event type
     * @param host event host subject
     * @param time occurrence time
     */
    public HostEvent(Type type, Host host, long time) {
        super(type, host, time);
    }

    /**
     * Creates an event with previous subject.
     *
     * The previous subject is ignored if the type is not moved or updated
     *
     * @param type host event type
     * @param host event host subject
     * @param prevSubject previous host subject
     */
    public HostEvent(Type type, Host host, Host prevSubject) {
        super(type, host);
        if (type == Type.HOST_MOVED || type == Type.HOST_UPDATED) {
            this.prevSubject = prevSubject;
        }
    }

    /**
     * Gets the previous subject in this host event.
     *
     * @return the previous subject, or null if previous subject is not
     *         specified.
     */
    public Host prevSubject() {
        return this.prevSubject;
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
