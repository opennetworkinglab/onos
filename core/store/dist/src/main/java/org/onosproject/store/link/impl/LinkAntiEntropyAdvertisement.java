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
package org.onosproject.store.link.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.onosproject.cluster.NodeId;
import org.onosproject.net.LinkKey;
import org.onosproject.store.Timestamp;

/**
 * Link AE Advertisement message.
 */
public class LinkAntiEntropyAdvertisement {

    private final NodeId sender;
    private final Map<LinkFragmentId, Timestamp> linkTimestamps;
    private final Map<LinkKey, Timestamp> linkTombstones;


    public LinkAntiEntropyAdvertisement(NodeId sender,
                Map<LinkFragmentId, Timestamp> linkTimestamps,
                Map<LinkKey, Timestamp> linkTombstones) {
        this.sender = checkNotNull(sender);
        this.linkTimestamps = checkNotNull(linkTimestamps);
        this.linkTombstones = checkNotNull(linkTombstones);
    }

    public NodeId sender() {
        return sender;
    }

    public Map<LinkFragmentId, Timestamp> linkTimestamps() {
        return linkTimestamps;
    }

    public Map<LinkKey, Timestamp> linkTombstones() {
        return linkTombstones;
    }

    // For serializer
    @SuppressWarnings("unused")
    private LinkAntiEntropyAdvertisement() {
        this.sender = null;
        this.linkTimestamps = null;
        this.linkTombstones = null;
    }
}
