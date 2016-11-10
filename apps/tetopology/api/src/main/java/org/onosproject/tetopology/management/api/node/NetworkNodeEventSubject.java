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
package org.onosproject.tetopology.management.api.node;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.tetopology.management.api.TeTopologyEventSubject;

/**
 * Representation of a network node event.
 */
public class NetworkNodeEventSubject implements TeTopologyEventSubject {
    private final NetworkNodeKey key;
    private final NetworkNode node;

    /**
     * Creates a network node event.
     *
     * @param key  the network node global key
     * @param node the network node
     */
    public NetworkNodeEventSubject(NetworkNodeKey key, NetworkNode node) {
        this.key = key;
        this.node = node;
    }

    /**
     * Returns the network node global key.
     *
     * @return the key
     */
    public NetworkNodeKey key() {
        return key;
    }

    /**
     * Returns the network node.
     *
     * @return the node
     */
    public NetworkNode neworkNode() {
        return node;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key, node);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof NetworkNodeEventSubject) {
            NetworkNodeEventSubject that = (NetworkNodeEventSubject) object;
            return Objects.equal(key, that.key) &&
                    Objects.equal(node, that.node);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("key", key)
                .add("node", node)
                .toString();
    }
}
