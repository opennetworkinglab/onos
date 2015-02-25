/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.ecmap;

import com.google.common.base.MoreObjects;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.Timestamp;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Anti-entropy advertisement message for eventually consistent map.
 */
public class AntiEntropyAdvertisement<K> {

    private final NodeId sender;
    private final Map<K, Timestamp> timestamps;
    private final Map<K, Timestamp> tombstones;

    /**
     * Creates a new anti entropy advertisement message.
     *
     * @param sender the sender's node ID
     * @param timestamps map of item key to timestamp for current items
     * @param tombstones map of item key to timestamp for removed items
     */
    public AntiEntropyAdvertisement(NodeId sender,
                                    Map<K, Timestamp> timestamps,
                                    Map<K, Timestamp> tombstones) {
        this.sender = checkNotNull(sender);
        this.timestamps = checkNotNull(timestamps);
        this.tombstones = checkNotNull(tombstones);
    }

    /**
     * Returns the sender's node ID.
     *
     * @return the sender's node ID
     */
    public NodeId sender() {
        return sender;
    }

    /**
     * Returns the map of current item timestamps.
     *
     * @return current item timestamps
     */
    public Map<K, Timestamp> timestamps() {
        return timestamps;
    }

    /**
     * Returns the map of removed item timestamps.
     *
     * @return removed item timestamps
     */
    public Map<K, Timestamp> tombstones() {
        return tombstones;
    }

    // For serializer
    @SuppressWarnings("unused")
    private AntiEntropyAdvertisement() {
        this.sender = null;
        this.timestamps = null;
        this.tombstones = null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("timestampsSize", timestamps.size())
                .add("tombstonesSize", tombstones.size())
                .toString();
    }
}
