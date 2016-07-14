/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.topology;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of a network topology cluster.
 */
public class DefaultTopologyCluster implements TopologyCluster {

    private final ClusterId id;
    private final int deviceCount;
    private final int linkCount;
    private final TopologyVertex root;

    /**
     * Creates a new topology cluster descriptor with the specified attributes.
     *
     * @param id          cluster id
     * @param deviceCount number of devices in the cluster
     * @param linkCount   number of links in the cluster
     * @param root        cluster root node
     */
    public DefaultTopologyCluster(ClusterId id, int deviceCount, int linkCount,
                                  TopologyVertex root) {
        this.id = id;
        this.deviceCount = deviceCount;
        this.linkCount = linkCount;
        this.root = root;
    }

    @Override
    public ClusterId id() {
        return id;
    }

    @Override
    public int deviceCount() {
        return deviceCount;
    }

    @Override
    public int linkCount() {
        return linkCount;
    }

    @Override
    public TopologyVertex root() {
        return root;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, deviceCount, linkCount, root);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultTopologyCluster) {
            final DefaultTopologyCluster other = (DefaultTopologyCluster) obj;
            return Objects.equals(this.id, other.id) &&
                    Objects.equals(this.deviceCount, other.deviceCount) &&
                    Objects.equals(this.linkCount, other.linkCount) &&
                    Objects.equals(this.root, other.root);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("deviceCount", deviceCount)
                .add("linkCount", linkCount)
                .add("root", root)
                .toString();
    }
}
