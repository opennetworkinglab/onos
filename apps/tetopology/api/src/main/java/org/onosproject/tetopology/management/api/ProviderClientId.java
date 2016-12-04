/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;

/**
 * TE topology provider and client identifiers.
 */
public abstract class ProviderClientId {
    private final long providerId;
    private final long clientId;

    /**
     * Creates an instance of TE topology provider client identifier.
     *
     * @param providerId provider identifier
     * @param clientId   client identifier
     */
    public ProviderClientId(long providerId, long clientId) {
        this.providerId = providerId;
        this.clientId = clientId;
    }

    /**
     * Returns the provider identifier.
     *
     * @return provider identifier
     */
    public long providerId() {
        return providerId;
    }

    /**
     * Returns the client identifier.
     *
     * @return client identifier
     */
    public long clientId() {
        return clientId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(providerId, clientId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof ProviderClientId) {
            ProviderClientId that = (ProviderClientId) object;
            return Objects.equal(providerId, that.providerId) &&
                    Objects.equal(clientId, that.clientId);
        }
        return false;
    }

    /**
     * Returns ToStringHelper with providerId and clientId.
     *
     * @return toStringHelper
     */
    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("providerId", providerId)
                .add("clientId", clientId);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }
}
