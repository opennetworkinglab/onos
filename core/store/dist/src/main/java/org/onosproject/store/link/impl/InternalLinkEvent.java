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

import com.google.common.base.MoreObjects;

import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.impl.Timestamped;

/**
 * Information published by GossipDeviceStore to notify peers of a device
 * change event.
 */
public class InternalLinkEvent {

    private final ProviderId providerId;
    private final Timestamped<LinkDescription> linkDescription;

    protected InternalLinkEvent(
            ProviderId providerId,
            Timestamped<LinkDescription> linkDescription) {
        this.providerId = providerId;
        this.linkDescription = linkDescription;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public Timestamped<LinkDescription> linkDescription() {
        return linkDescription;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("linkDescription", linkDescription)
                .toString();
    }

    // for serializer
    protected InternalLinkEvent() {
        this.providerId = null;
        this.linkDescription = null;
    }
}
