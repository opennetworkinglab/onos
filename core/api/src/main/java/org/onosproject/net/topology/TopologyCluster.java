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

/**
 * Representation of an SCC (strongly-connected component) in a network topology.
 */
public interface TopologyCluster {

    /**
     * Returns the cluster id.
     *
     * @return cluster identifier
     */
    ClusterId id();

    /**
     * Returns the number of devices in the cluster.
     *
     * @return number of cluster devices
     */
    int deviceCount();

    /**
     * Returns the number of infrastructure links in the cluster.
     *
     * @return number of cluster links
     */
    int linkCount();

    /**
     * Returns the cluster root vertex.
     *
     * @return cluster root vertex
     */
    TopologyVertex root();

}
