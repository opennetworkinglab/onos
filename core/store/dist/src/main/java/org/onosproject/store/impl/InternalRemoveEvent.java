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
package org.onosproject.store.impl;

import com.google.common.collect.ImmutableList;
import org.onosproject.store.Timestamp;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Internal inter-instance event used by EventuallyConsistentMap for REMOVE
 * events.
 */
final class InternalRemoveEvent<K> {
    private final List<RemoveEntry<K>> entries;

    /**
     * Creates a remove event for a single key.
     *
     * @param key key the event concerns
     * @param timestamp timestamp of the event
     */
    public InternalRemoveEvent(K key, Timestamp timestamp) {
        entries = ImmutableList.of(new RemoveEntry<>(key, timestamp));
    }

    /**
     * Creates a remove event for multiple keys.
     *
     * @param entries list of remove entries to send an event for
     */
    public InternalRemoveEvent(List<RemoveEntry<K>> entries) {
        this.entries = checkNotNull(entries);
    }

    // Needed for serialization.
    @SuppressWarnings("unused")
    private InternalRemoveEvent() {
        entries = null;
    }

    /**
     * Returns the list of remove entries this event concerns.
     *
     * @return list of remove entries
     */
    public List<RemoveEntry<K>> entries() {
        return entries;
    }
}
