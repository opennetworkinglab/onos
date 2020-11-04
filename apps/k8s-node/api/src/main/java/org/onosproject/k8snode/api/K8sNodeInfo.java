/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.k8snode.api;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import java.util.Objects;

/**
 * Kubernetes node info class.
 */
public class K8sNodeInfo {
    private final IpAddress nodeIp;
    private final MacAddress nodeMac;

    /**
     * Default constructor.
     *
     * @param nodeIp        node IP address
     * @param nodeMac       node MAC address
     */
    public K8sNodeInfo(IpAddress nodeIp, MacAddress nodeMac) {
        this.nodeIp = nodeIp;
        this.nodeMac = nodeMac;
    }

    /**
     * Obtains Kubernetes node IP address.
     *
     * @return node IP address
     */
    public IpAddress nodeIp() {
        return nodeIp;
    }

    /**
     * Obtains Kubernetes node MAC address.
     *
     * @return node MAC address
     */
    public MacAddress nodeMac() {
        return nodeMac;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        K8sNodeInfo that = (K8sNodeInfo) o;
        return Objects.equals(nodeIp, that.nodeIp) &&
                Objects.equals(nodeMac, that.nodeMac);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeIp, nodeMac);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("nodeIp", nodeIp)
                .add("nodeMac", nodeMac)
                .toString();
    }
}