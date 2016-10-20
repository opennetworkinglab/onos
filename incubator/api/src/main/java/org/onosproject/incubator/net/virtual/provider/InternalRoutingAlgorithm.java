/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual.provider;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Path;

/**
 * Abstraction of an embedding algorithm used for embedding virtual objects
 * into the underlying physical network.
 */
public interface InternalRoutingAlgorithm {
    /**
     * Find a route between two connect points (i.e. ports)
     * according to the own logic.
     *
     * @param src A start point
     * @param dst A sink point
     * @return The path between src and dst calculated from the algorithm
     */
    Path findPath(ConnectPoint src, ConnectPoint dst);
}
