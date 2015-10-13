/*
 * Copyright 2015 Open Networking Laboratory
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

/**
 * Describes an interface event.
 */
public class InterfaceEvent extends AbstractEvent<InterfaceEvent.Type, Interface> {

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
        super(type, subject);
    }

    /**
     * Creates an interface event with type, subject and time.
     *
     * @param type event type
     * @param subject subject interface
     * @param time time of event
     */
    public InterfaceEvent(Type type, Interface subject, long time) {
        super(type, subject, time);
    }

}
