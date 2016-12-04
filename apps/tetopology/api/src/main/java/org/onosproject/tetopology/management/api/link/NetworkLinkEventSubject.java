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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.tetopology.management.api.TeTopologyEventSubject;

/**
 * Representation of a network link event.
 */
public class NetworkLinkEventSubject implements TeTopologyEventSubject {
    private final NetworkLinkKey key;
    private final NetworkLink networkLink;

    /**
     * Creates a network link event instance.
     *
     * @param key         the network link global key
     * @param networkLink the network link object
     */
    public NetworkLinkEventSubject(NetworkLinkKey key, NetworkLink networkLink) {
        this.key = key;
        this.networkLink = networkLink;
    }

    /**
     * Returns the network link global key.
     *
     * @return the key
     */
    public NetworkLinkKey key() {
        return key;
    }

    /**
     * Returns the network link.
     *
     * @return the network link
     */
    public NetworkLink networkLink() {
        return networkLink;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key, networkLink);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof NetworkLinkEventSubject) {
            NetworkLinkEventSubject that = (NetworkLinkEventSubject) object;
            return Objects.equal(key, that.key) &&
                    Objects.equal(networkLink, that.networkLink);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("key", key)
                .add("networkLink", networkLink)
                .toString();
    }
}
