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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.onosproject.cluster.NodeId;
import org.onosproject.net.HostId;
import org.onosproject.store.Timestamp;

/**
 * Host AE Advertisement message.
 */
public final class HostAntiEntropyAdvertisement {

    private final NodeId sender;
    private final Map<HostFragmentId, Timestamp> timestamps;
    private final Map<HostId, Timestamp> tombstones;


    public HostAntiEntropyAdvertisement(NodeId sender,
                Map<HostFragmentId, Timestamp> timestamps,
                Map<HostId, Timestamp> tombstones) {
        this.sender = checkNotNull(sender);
        this.timestamps = checkNotNull(timestamps);
        this.tombstones = checkNotNull(tombstones);
    }

    public NodeId sender() {
        return sender;
    }

    public Map<HostFragmentId, Timestamp> timestamps() {
        return timestamps;
    }

    public Map<HostId, Timestamp> tombstones() {
        return tombstones;
    }

    // For serializer
    @SuppressWarnings("unused")
    private HostAntiEntropyAdvertisement() {
        this.sender = null;
        this.timestamps = null;
        this.tombstones = null;
    }
}
