/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api;

import org.onosproject.event.AbstractEvent;

/**
 * TE network topology event.
 */
public class TeTopologyEvent
         extends AbstractEvent<TeTopologyEvent.Type, TeTopologyEventSubject> {

    /**
     * Type of TE topology events.
     */
    public enum Type {

        /** Designates addition of a network. */
        NETWORK_ADDED,

        /** Designates update of a network. */
        NETWORK_UPDATED,

        /** Designates removal of a network. */
        NETWORK_REMOVED,

        /** Designates addition of a node. */
        NODE_ADDED,

        /** Designates update of a node. */
        NODE_UPDATED,

        /** Designates removal of a node. */
        NODE_REMOVED,

        /** Designates addition of a termination point. */
        TP_ADDED,

        /** Designates update of a termination point. */
        TP_UPDATED,

        /** Designates removal of a termination point. */
        TP_REMOVED,

        /** Designates addition of a link. */
        LINK_ADDED,

        /** Designates update of a link. */
        LINK_UPDATED,

        /** Designates removal of a link. */
        LINK_REMOVED;
    }

    /**
     * Constructor for TeTopologyEvent.
     *
     * @param type type of topology event
     * @param subject event subject interface
     */
    public TeTopologyEvent(Type type, TeTopologyEventSubject subject) {
        super(type, subject);
    }

    /**
     * Constructor for TeTopologyEvent.
     *
     * @param type type of topology event
     * @param subject event subject interface
     * @param time event time
     */
    public TeTopologyEvent(Type type, TeTopologyEventSubject subject, long time) {
        super(type, subject, time);
    }

}
