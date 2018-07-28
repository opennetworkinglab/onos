/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.atomix.primitives.impl;

import java.util.Map;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import org.onosproject.cluster.NodeId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Anti-entropy advertisement message for eventually consistent map.
 */
public class AntiEntropyAdvertisement<K> {

    private final NodeId sender;
    private final Map<K, MapValue.Digest> digest;

    /**
     * Creates a new anti entropy advertisement message.
     *
     * @param sender the sender's node ID
     * @param digest for map entries
     */
    public AntiEntropyAdvertisement(NodeId sender,
                                    Map<K, MapValue.Digest> digest) {
        this.sender = checkNotNull(sender);
        this.digest = ImmutableMap.copyOf(checkNotNull(digest));
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
     * Returns the digest for map entries.
     *
     * @return mapping from key to associated digest
     */
    public Map<K, MapValue.Digest> digest() {
        return digest;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("sender", sender)
                .add("totalEntries", digest.size())
                .toString();
    }
}
