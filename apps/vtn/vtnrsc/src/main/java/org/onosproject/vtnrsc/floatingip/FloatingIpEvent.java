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
package org.onosproject.vtnrsc.floatingip;

import org.onosproject.event.AbstractEvent;
import org.onosproject.vtnrsc.FloatingIp;

/**
 * Describes network Floating IP event.
 */
public class FloatingIpEvent
        extends AbstractEvent<FloatingIpEvent.Type, FloatingIp> {
    /**
     * Type of Floating IP events.
     */
    public enum Type {
        /**
         * Signifies that Floating IP has been created.
         */
        FLOATINGIP_PUT,
        /**
         * Signifies that Floating IP has been deleted.
         */
        FLOATINGIP_DELETE,
        /**
         * Signifies that Floating IP has been bound.
         */
        FLOATINGIP_BIND,
        /**
         * Signifies that Floating IP has been unbound.
         */
        FLOATINGIP_UNBIND
    }

    /**
     * Creates an event of a given type and for the specified Floating IP.
     *
     * @param type Floating IP event type
     * @param floagingIp Floating IP subject
     */
    public FloatingIpEvent(Type type, FloatingIp floagingIp) {
        super(type, floagingIp);
    }

    /**
     * Creates an event of a given type and for the specified Floating IP.
     *
     * @param type Floating IP event type
     * @param floagingIp Floating IP subject
     * @param time occurrence time
     */
    public FloatingIpEvent(Type type, FloatingIp floagingIp, long time) {
        super(type, floagingIp, time);
    }
}
