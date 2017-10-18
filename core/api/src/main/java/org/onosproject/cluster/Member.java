/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.cluster;

import java.util.Objects;

import org.onosproject.core.Version;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Controller member identity.
 */
public final class Member {

    private final NodeId nodeId;
    private final Version version;

    /**
     * Creates a new cluster member identifier from the specified string.
     *
     * @param nodeId node identifier
     * @param version node version
     */
    public Member(NodeId nodeId, Version version) {
        this.nodeId = checkNotNull(nodeId);
        this.version = version;
    }

    /**
     * Returns the node identifier.
     *
     * @return the node identifier
     */
    public NodeId nodeId() {
        return nodeId;
    }

    /**
     * Returns the node version.
     *
     * @return the node version
     */
    public Version version() {
        return version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, version);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Member) {
            Member member = (Member) object;
            return member.nodeId.equals(nodeId) && Objects.equals(member.version, version);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("nodeId", nodeId)
                .add("version", version)
                .toString();
    }
}
