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

import org.onlab.packet.IpAddress;

import java.util.Set;

/**
 * Representation of host and kubernetes nodes mapping info.
 */
public interface HostNodesInfo {

    /**
     * Returns the host's IP address.
     *
     * @return host IP address
     */
    IpAddress hostIp();

    /**
     * Returns the list of nodes associated with the host.
     *
     * @return a set of node's names
     */
    Set<String> nodes();

    /**
     * Builder of new HostNodesInfo entity.
     */
    interface Builder {

        /**
         * Builds an immutable host IP to nodes mapping instance.
         *
         * @return HostNodesInfo instance
         */
        HostNodesInfo build();

        /**
         * Returns HostNodesInfo builder with host IP address.
         *
         * @param hostIp host IP address
         * @return HostNodesInfo builder
         */
        Builder hostIp(IpAddress hostIp);

        /**
         * Returns HostNodesInfo builder with nodes.
         *
         * @param nodes a set of node's names
         * @return HostNodesInfo builder
         */
        Builder nodes(Set<String> nodes);
    }
}
