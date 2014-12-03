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

import java.util.Objects;

import org.onosproject.net.LinkKey;
import org.onosproject.net.provider.ProviderId;

import com.google.common.base.MoreObjects;

/**
 * Identifier for LinkDescription from a Provider.
 */
public final class LinkFragmentId {
    public final ProviderId providerId;
    public final LinkKey linkKey;

    public LinkFragmentId(LinkKey linkKey, ProviderId providerId) {
        this.providerId = providerId;
        this.linkKey = linkKey;
    }

    public LinkKey linkKey() {
        return linkKey;
    }

    public ProviderId providerId() {
        return providerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, linkKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LinkFragmentId)) {
            return false;
        }
        LinkFragmentId that = (LinkFragmentId) obj;
        return Objects.equals(this.linkKey, that.linkKey) &&
               Objects.equals(this.providerId, that.providerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("linkKey", linkKey)
                .toString();
    }

    // for serializer
    @SuppressWarnings("unused")
    private LinkFragmentId() {
        this.providerId = null;
        this.linkKey = null;
    }
}
