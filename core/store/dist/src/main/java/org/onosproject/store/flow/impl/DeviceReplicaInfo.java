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

import java.util.List;
import java.util.Objects;

import org.onosproject.cluster.NodeId;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Device term context.
 */
public class DeviceReplicaInfo {
    private final long term;
    private final NodeId master;
    private final List<NodeId> backups;

    public DeviceReplicaInfo(long term, NodeId master, List<NodeId> backups) {
        this.term = term;
        this.master = master;
        this.backups = backups;
    }

    /**
     * Returns the mastership term.
     *
     * @return the mastership term
     */
    public long term() {
        return term;
    }

    /**
     * Returns the master for the {@link #term()}.
     *
     * @return the master {@link NodeId} for the current {@link #term()}
     */
    public NodeId master() {
        return master;
    }

    /**
     * Returns a boolean indicating whether the given {@link NodeId} is the current master.
     *
     * @param nodeId the node ID to check
     * @return indicates whether the given node identifier is the identifier of the current master
     */
    public boolean isMaster(NodeId nodeId) {
        return Objects.equals(master, nodeId);
    }

    /**
     * Returns a list of all active backup nodes in priority order.
     * <p>
     * The returned backups are limited by the flow rule store's configured backup count.
     *
     * @return a list of backup nodes in priority order
     */
    public List<NodeId> backups() {
        return backups;
    }

    /**
     * Returns a boolean indicating whether the given node is a backup.
     *
     * @param nodeId the node identifier
     * @return indicates whether the given node is a backup
     */
    public boolean isBackup(NodeId nodeId) {
        return backups.contains(nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, master, backups);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DeviceReplicaInfo) {
            DeviceReplicaInfo that = (DeviceReplicaInfo) object;
            return this.term == that.term
                && Objects.equals(this.master, that.master)
                && Objects.equals(this.backups, that.backups);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
            .add("term", term())
            .add("master", master())
            .add("backups", backups())
            .toString();
    }
}
