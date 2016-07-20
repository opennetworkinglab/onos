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

package org.onosproject.incubator.net.tunnel;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;

/**
 * Describes tunnel events.
 */
@Beta
public final class TunnelEvent extends AbstractEvent<TunnelEvent.Type, Tunnel> {

    /**
     * Type of tunnel events.
     */
    public enum Type {
        /**
         * Signifies that a new tunnel has been added.
         */
        TUNNEL_ADDED,

        /**
         * Signifies that a tunnel has been updated or changed state.
         */
        TUNNEL_UPDATED,

        /**
         * Signifies that a tunnel has been removed.
         */
        TUNNEL_REMOVED
    }

    /**
     * Creates an event of a given type and for the specified tunnel.
     *
     * @param type tunnel event type
     * @param tunnel event tunnel subject
     */
    public TunnelEvent(Type type, Tunnel tunnel) {
        super(type, tunnel);
    }

    /**
     * Creates an event of a given type and for the specified link and
     * the current time.
     *
     * @param type tunnel event type
     * @param tunnel event tunnel subject
     * @param time occurrence time
     */
    public TunnelEvent(Type type, Tunnel tunnel, long time) {
        super(type, tunnel, time);
    }

}
