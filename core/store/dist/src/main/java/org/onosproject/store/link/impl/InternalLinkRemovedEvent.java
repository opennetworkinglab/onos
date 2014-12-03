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

import org.onosproject.net.LinkKey;
import org.onosproject.store.Timestamp;

import com.google.common.base.MoreObjects;

/**
 * Information published by GossipLinkStore to notify peers of a link
 * being removed.
 */
public class InternalLinkRemovedEvent {

    private final LinkKey linkKey;
    private final Timestamp timestamp;

    /**
     * Creates a InternalLinkRemovedEvent.
     * @param linkKey identifier of the removed link.
     * @param timestamp timestamp of when the link was removed.
     */
    public InternalLinkRemovedEvent(LinkKey linkKey, Timestamp timestamp) {
        this.linkKey = linkKey;
        this.timestamp = timestamp;
    }

    public LinkKey linkKey() {
        return linkKey;
    }

    public Timestamp timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("linkKey", linkKey)
                .add("timestamp", timestamp)
                .toString();
    }

    // for serializer
    @SuppressWarnings("unused")
    private InternalLinkRemovedEvent() {
        linkKey = null;
        timestamp = null;
    }
}
