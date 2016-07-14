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

import org.onosproject.net.Provided;

/**
 * Represents a network topology computation snapshot.
 */
public interface Topology extends Provided {

    /**
     * Returns the time, specified in system nanos of when the topology became
     * available.
     *
     * @return time in system nanos
     */
    long time();

    /**
     * Returns the time, specified in system millis of when the topology became
     * available.
     *
     * @return time in system nanos
     */
    long creationTime();

    /**
     * Returns the time, specified in system nanos of how long the topology took
     * to compute.
     *
     * @return elapsed time in system nanos
     */
    long computeCost();

    /**
     * Returns the number of SCCs (strongly connected components) in the
     * topology.
     *
     * @return number of clusters
     */
    int clusterCount();

    /**
     * Returns the number of infrastructure devices in the topology.
     *
     * @return number of devices
     */
    int deviceCount();

    /**
     * Returns the number of infrastructure links in the topology.
     *
     * @return number of links
     */
    int linkCount();

}
