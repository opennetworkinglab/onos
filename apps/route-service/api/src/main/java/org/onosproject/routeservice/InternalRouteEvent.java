/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routeservice;

import org.onosproject.event.AbstractEvent;

/**
 * Route event for signalling between the store and the manager.
 */
public class InternalRouteEvent extends
        AbstractEvent<InternalRouteEvent.Type, RouteSet> {

    /**
     * Internal route event type.
     */
    public enum Type {
        /**
         * Indicates a route was added to the store.
         */
        ROUTE_ADDED,

        /**
         * Indicates a route was removed from the store.
         */
        ROUTE_REMOVED
    }

    /**
     * Creates a new internal route event.
     *
     * @param type route event type
     * @param subject route set
     */
    public InternalRouteEvent(Type type, RouteSet subject) {
        super(type, subject);
    }

    protected InternalRouteEvent(Type type, RouteSet subject, long time) {
        super(type, subject, time);
    }
}
