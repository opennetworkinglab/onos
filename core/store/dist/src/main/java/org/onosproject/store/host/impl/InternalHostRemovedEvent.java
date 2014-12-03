/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.host.impl;

import org.onosproject.net.HostId;
import org.onosproject.store.Timestamp;

/**
 * Information published by GossipHostStore to notify peers of a host
 * removed event.
 */
public class InternalHostRemovedEvent {

    private final HostId hostId;
    private final Timestamp timestamp;

    public InternalHostRemovedEvent(HostId hostId, Timestamp timestamp) {
        this.hostId = hostId;
        this.timestamp = timestamp;
    }

    public HostId hostId() {
        return hostId;
    }

    public Timestamp timestamp() {
        return timestamp;
    }

    // for serialization.
    @SuppressWarnings("unused")
    private InternalHostRemovedEvent() {
        hostId = null;
        timestamp = null;
    }
}
