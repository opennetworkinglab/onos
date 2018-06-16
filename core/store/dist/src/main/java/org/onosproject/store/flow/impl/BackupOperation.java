/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.flow.impl;

import java.util.Objects;

import org.onosproject.cluster.NodeId;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Identifier representing a backup of a distinct bucket to a specific node.
 */
public class BackupOperation {
    private final NodeId nodeId;
    private final int bucketId;

    BackupOperation(NodeId nodeId, int bucketId) {
        this.nodeId = nodeId;
        this.bucketId = bucketId;
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
     * Returns the bucket identifier.
     *
     * @return the bucket identifier
     */
    public int bucket() {
        return bucketId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, bucketId);
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other instanceof BackupOperation) {
            BackupOperation that = (BackupOperation) other;
            return this.nodeId.equals(that.nodeId)
                && this.bucketId == that.bucketId;
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
            .add("nodeId", nodeId())
            .add("bucket", bucket())
            .toString();
    }
}