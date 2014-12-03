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
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.Timestamp;

/**
 * Information published by GossipHostStore to notify peers of a host
 * change (create/update) event.
 */
public class InternalHostEvent {

    private final ProviderId providerId;
    private final HostId hostId;
    private final HostDescription hostDescription;
    private final Timestamp timestamp;

    public InternalHostEvent(ProviderId providerId, HostId hostId,
                             HostDescription hostDescription, Timestamp timestamp) {
        this.providerId = providerId;
        this.hostId = hostId;
        this.hostDescription = hostDescription;
        this.timestamp = timestamp;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public HostId hostId() {
        return hostId;
    }

    public HostDescription hostDescription() {
        return hostDescription;
    }

    public Timestamp timestamp() {
        return timestamp;
    }

    // Needed for serialization.
    @SuppressWarnings("unused")
    private InternalHostEvent() {
        providerId = null;
        hostId = null;
        hostDescription = null;
        timestamp = null;
    }
}
