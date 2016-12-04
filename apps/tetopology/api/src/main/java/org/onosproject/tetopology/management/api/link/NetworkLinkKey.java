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
package org.onosproject.tetopology.management.api.link;

import org.onosproject.tetopology.management.api.KeyId;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Representation of a link key or link reference.
 */
public class NetworkLinkKey {
    private final KeyId networkId;
    private final KeyId linkId;

    /**
     * Creates an instance of NetworkLinkKey.
     *
     * @param networkId network identifier
     * @param linkId link identifier
     */
    public NetworkLinkKey(KeyId networkId, KeyId linkId) {
        this.networkId = networkId;
        this.linkId = linkId;
    }

    /**
     * Returns the network identifier.
     *
     * @return network identifier
     */
    public KeyId networkId() {
        return networkId;
    }

    /**
     * Returns the link identifier.
     *
     * @return link identifier
     */
    public KeyId linkId() {
        return linkId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networkId, linkId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof NetworkLinkKey) {
            NetworkLinkKey that = (NetworkLinkKey) object;
            return Objects.equal(this.networkId, that.networkId) &&
                    Objects.equal(this.linkId, that.linkId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("networkId", networkId)
                .add("linkId", linkId)
                .toString();
    }

}
