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
package org.onosproject.vtnrsc.router;

import org.onosproject.event.AbstractEvent;
import org.onosproject.vtnrsc.Router;

/**
 * Describes network Router event.
 */
public class RouterEvent extends AbstractEvent<RouterEvent.Type, Router> {
    /**
     * Type of Router events.
     */
    public enum Type {
        /**
         * Signifies that router has been created.
         */
        ROUTER_PUT,
        /**
         * Signifies that router has been deleted.
         */
        ROUTER_DELETE
    }

    /**
     * Creates an event of a given type and for the specified Router.
     *
     * @param type Router event type
     * @param router Router subject
     */
    public RouterEvent(Type type, Router router) {
        super(type, router);
    }

    /**
     * Creates an event of a given type and for the specified Router.
     *
     * @param type Router event type
     * @param router Router subject
     * @param time occurrence time
     */
    public RouterEvent(Type type, Router router, long time) {
        super(type, router, time);
    }
}
