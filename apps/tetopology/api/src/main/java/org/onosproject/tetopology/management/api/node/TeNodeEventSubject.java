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

import org.onosproject.tetopology.management.api.TeTopologyEventSubject;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Representation of TE node event.
 */
public class TeNodeEventSubject implements TeTopologyEventSubject {
    private final TeNodeKey key;
    private final TeNode teNode;

    /**
     * Creates a TE node event.
     *
     * @param key    the TE node global key
     * @param teNode the TE node
     */
    public TeNodeEventSubject(TeNodeKey key, TeNode teNode) {
        this.key = key;
        this.teNode = teNode;
    }

    /**
     * Returns the TE node global key.
     *
     * @return the key
     */
    public TeNodeKey key() {
        return key;
    }

    /**
     * Returns the TE node.
     *
     * @return the TE node
     */
    public TeNode teNode() {
        return teNode;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key, teNode);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeNodeEventSubject) {
            TeNodeEventSubject that = (TeNodeEventSubject) object;
            return Objects.equal(key, that.key) &&
                    Objects.equal(teNode, that.teNode);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("key", key)
                .add("teNode", teNode)
                .toString();
    }
}
