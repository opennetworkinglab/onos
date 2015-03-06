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
import org.onosproject.store.Timestamp;

/**
 * Describes a single remove event in an EventuallyConsistentMap.
 */
final class RemoveEntry<K, V> extends AbstractEntry<K, V> {
    /**
     * Creates a new remove entry.
     *
     * @param key key of the entry
     * @param timestamp timestamp of the remove event
     */
    public RemoveEntry(K key, Timestamp timestamp) {
        super(key, timestamp);
    }

    // Needed for serialization.
    @SuppressWarnings("unused")
    private RemoveEntry() {
        super();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("key", key())
                .add("timestamp", timestamp())
                .toString();
    }
}
