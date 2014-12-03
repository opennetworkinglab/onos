/*
 * Copyright 2014 Open Networking Laboratory
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

import org.onosproject.net.ElementId;
import org.onosproject.net.Path;

import java.util.Set;

/**
 * Service for obtaining pre-computed paths or for requesting computation of
 * paths using the current topology snapshot.
 */
public interface PathService {

    /**
     * Returns the set of all shortest paths, precomputed in terms of hop-count,
     * between the specified source and destination elements.
     *
     * @param src source element
     * @param dst destination element
     * @return set of all shortest paths between the two elements
     */
    Set<Path> getPaths(ElementId src, ElementId dst);

    /**
     * Returns the set of all shortest paths, computed using the supplied
     * edge-weight entity, between the specified source and destination
     * network elements.
     *
     * @param src source element
     * @param dst destination element
     * @param weight edge-weight entity
     * @return set of all shortest paths between the two element
     */
    Set<Path> getPaths(ElementId src, ElementId dst, LinkWeight weight);

}
