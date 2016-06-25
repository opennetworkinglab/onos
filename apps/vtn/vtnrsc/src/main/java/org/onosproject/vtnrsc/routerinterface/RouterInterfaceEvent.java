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
package org.onosproject.vtnrsc.routerinterface;

import org.onosproject.event.AbstractEvent;
import org.onosproject.vtnrsc.RouterInterface;

/**
 * Describes network Router Interface event.
 */
public class RouterInterfaceEvent
        extends AbstractEvent<RouterInterfaceEvent.Type, RouterInterface> {

    /**
     * Type of Router Interface events.
     */
    public enum Type {
        /**
         * Signifies that router interface has been added.
         */
        ROUTER_INTERFACE_PUT,
        /**
         * Signifies that router interface has been removed.
         */
        ROUTER_INTERFACE_DELETE
    }

    /**
     * Creates an event of a given type and for the specified Router Interface.
     *
     * @param type Router Interface event type
     * @param routerInterface Router Interface subject
     */
    public RouterInterfaceEvent(Type type, RouterInterface routerInterface) {
        super(type, routerInterface);
    }

    /**
     * Creates an event of a given type and for the specified Router Interface.
     *
     * @param type Router Interface event type.
     * @param routerInterface Router Interface subject
     * @param time occurrence time
     */
    public RouterInterfaceEvent(Type type, RouterInterface routerInterface,
                                long time) {
        super(type, routerInterface, time);
    }
}
