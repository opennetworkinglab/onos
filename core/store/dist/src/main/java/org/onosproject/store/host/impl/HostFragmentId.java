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

import java.util.Objects;

import org.onosproject.net.HostId;
import org.onosproject.net.provider.ProviderId;

import com.google.common.base.MoreObjects;

/**
 * Identifier for HostDescription from a Provider.
 */
public final class HostFragmentId {
    public final ProviderId providerId;
    public final HostId hostId;

    public HostFragmentId(HostId hostId, ProviderId providerId) {
        this.providerId = providerId;
        this.hostId = hostId;
    }

    public HostId hostId() {
        return hostId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, hostId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HostFragmentId)) {
            return false;
        }
        HostFragmentId that = (HostFragmentId) obj;
        return Objects.equals(this.hostId, that.hostId) &&
               Objects.equals(this.providerId, that.providerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("hostId", hostId)
                .toString();
    }

    // for serializer
    @SuppressWarnings("unused")
    private HostFragmentId() {
        this.providerId = null;
        this.hostId = null;
    }
}
