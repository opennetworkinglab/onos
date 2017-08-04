/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.link;

import org.onosproject.event.AbstractEvent;
import org.onosproject.net.Link;

/**
 * Describes infrastructure link event.
 */
public class LinkEvent extends AbstractEvent<LinkEvent.Type, Link> {

    /**
     * Type of link events.
     */
    public enum Type {
        /**
         * Signifies that a new link has been detected.
         */
        LINK_ADDED,

        /**
         * Signifies that a link has been updated or changed state.
         */
        LINK_UPDATED,

        /**
         * Signifies that a link has been removed.
         */
        LINK_REMOVED
    }

    /**
     * Creates an event of a given type and for the specified link and the
     * current time.
     *
     * @param type link event type
     * @param link event link subject
     */
    public LinkEvent(Type type, Link link) {
        super(type, link);
    }

    /**
     * Creates an event of a given type and for the specified link and time.
     *
     * @param type link event type
     * @param link event link subject
     * @param time occurrence time
     */
    public LinkEvent(Type type, Link link, long time) {
        super(type, link, time);
    }

}
