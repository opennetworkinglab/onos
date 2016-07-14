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

package org.onosproject.incubator.net.intf;

import org.onosproject.event.AbstractEvent;
import org.joda.time.LocalDateTime;
import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes an interface event.
 */
public class InterfaceEvent extends AbstractEvent<InterfaceEvent.Type, Interface> {

    private final Interface prevSubject;

    public enum Type {
        /**
         * Indicates a new interface has been added.
         */
        INTERFACE_ADDED,

        /**
         * Indicates an interface has been updated.
         */
        INTERFACE_UPDATED,

        /**
         * Indicates an interface has been removed.
         */
        INTERFACE_REMOVED
    }

    /**
     * Creates an interface event with type and subject.
     *
     * @param type event type
     * @param subject subject interface
     */
    public InterfaceEvent(Type type, Interface subject) {
        this(type, subject, null);
    }

    /**
     * Creates an interface event with type, subject and time of event.
     *
     * @param type event type
     * @param subject subject interface
     * @param time time of event
     */
    public InterfaceEvent(Type type, Interface subject, long time) {
        this(type, subject, null, time);
    }

    /**
     * Creates an interface event with type, subject and previous subject.
     *
     * @param type event type
     * @param subject subject interface
     * @param prevSubject previous interface subject
     */
    public InterfaceEvent(Type type, Interface subject, Interface prevSubject) {
        super(type, subject);
        this.prevSubject = prevSubject;
    }

    /**
     * Creates an interface event with type, subject, previous subject and time.
     *
     * @param type event type
     * @param subject subject interface
     * @param prevSubject previous interface subject
     * @param time time of event
     */
    public InterfaceEvent(Type type, Interface subject, Interface prevSubject, long time) {
        super(type, subject, time);
        this.prevSubject = prevSubject;
    }

    /**
     * Returns the previous interface subject.
     *
     * @return previous subject of interface or null if the event is not interface specific.
     */
    public Interface prevSubject() {
        return prevSubject;
    }

    @Override
    public String toString() {
        if (prevSubject == null) {
            return super.toString();
        }
        return toStringHelper(this)
                .add("time", new LocalDateTime(time()))
                .add("type", type())
                .add("subject", subject())
                .add("prevSubject", prevSubject)
                .toString();
     }
}
