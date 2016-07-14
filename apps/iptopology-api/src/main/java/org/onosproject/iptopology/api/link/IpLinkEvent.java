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
package org.onosproject.iptopology.api.link;

import org.onosproject.event.AbstractEvent;
import org.onosproject.iptopology.api.IpLink;

/**
 * Describes ip link event.
 */
public class IpLinkEvent extends AbstractEvent<IpLinkEvent.Type, IpLink> {

    /**
     * Type of link events.
     */
    public enum Type {
        /**
         * Signifies that a new ip link has been detected.
         */
        LINK_ADDED,

        /**
         * Signifies that an ip link has been updated or changed state.
         */
        LINK_UPDATED,

        /**
         * Signifies that an ip link has been removed.
         */
        LINK_REMOVED
    }

    /**
     * Creates an event of a given type and for the specified ip link and the
     * current time.
     *
     * @param type link event type
     * @param link event link subject
     */
    public IpLinkEvent(Type type, IpLink link) {
        super(type, link);
    }

    /**
     * Creates an event of a given type and for the specified ip link and time.
     *
     * @param type link event type
     * @param link event link subject
     * @param time occurrence time
     */
    public IpLinkEvent(Type type, IpLink link, long time) {
        super(type, link, time);
    }

}
