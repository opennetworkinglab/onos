/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual.event;

import org.onosproject.event.AbstractEvent;
import org.onosproject.event.Event;
import org.onosproject.incubator.net.virtual.NetworkId;

/**
 * Base class for virtual network event that encapsulates a normal event.
 */
public class VirtualEvent<E extends Event>
        extends AbstractEvent<VirtualEvent.Type, E> {

    private NetworkId networkId;

    /**
     * Type of virtual network events.
     */
    public enum Type {
        /**
         * A new virtual event has been posted.
         */
        POSTED
    }

    protected VirtualEvent(NetworkId networkId, Type type, E subject) {
        super(type, subject);
        this.networkId = networkId;
    }

    protected VirtualEvent(NetworkId networkId, Type type, E subject, long time) {
        super(type, subject, time);
        this.networkId = networkId;
    }

    public NetworkId networkId() {
        return networkId;
    }
}
