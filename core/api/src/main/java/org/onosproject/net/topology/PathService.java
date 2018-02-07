/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onosproject.net.DisjointPath;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;

import static org.onosproject.net.topology.HopCountLinkWeigher.DEFAULT_HOP_COUNT_WEIGHER;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Service for obtaining pre-computed paths or for requesting computation of
 * paths using the current topology snapshot.
 */
public interface PathService {

    /**
     * Returns the set of all shortest paths between the specified source and
     * destination elements. The path is computed using the default edge-weight
     * function, which by default is hop-count.
     *
     * @param src source element
     * @param dst destination element
     * @return set of all shortest paths between the two elements
     */
    Set<Path> getPaths(ElementId src, ElementId dst);

    /**
     * Returns the set of all shortest paths between the specified source and
     * destination network elements.  The path is computed using the supplied
     * edge-weight function.
     *
     * @param src     source element
     * @param dst     destination element
     * @param weigher edge-weight entity
     * @return set of all shortest paths between the two element
     */
    Set<Path> getPaths(ElementId src, ElementId dst, LinkWeigher weigher);

    /**
     * Returns the k-shortest paths between source and
     * destination devices.
     *
     * @param src    source device
     * @param dst    destination device
     * @return stream of k-shortest paths
     */
    default Stream<Path> getKShortestPaths(ElementId src, ElementId dst) {
        return getKShortestPaths(src, dst, DEFAULT_HOP_COUNT_WEIGHER);
    }

    /**
     * Returns the k-shortest paths between source and
     * destination devices.
     *
     * @param src    source device
     * @param dst    destination device
     * @param weigher edge-weight entity
     * @return stream of k-shortest paths
     */
    default Stream<Path> getKShortestPaths(ElementId src, ElementId dst,
                                           LinkWeigher weigher) {
        return getPaths(src, dst, weigher).stream();
    }


    /**
     * Returns the set of all disjoint shortest path pairs between the
     * specified source and destination elements. The path is computed using
     * the default edge-weight function, which by default is hop-count.
     *
     * @param src source device
     * @param dst destination device
     * @return set of all shortest paths between the two devices
     */
    Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst);

    /**
     * Returns the set of all disjoint shortest path pairs between the
     * specified source and destination elements. The path is computed using
     * the supplied edge-weight function.
     *
     * @param src     source device
     * @param dst     destination device
     * @param weigher edge-weight entity
     * @return set of all shortest paths between the two devices
     */
    Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst,
                                       LinkWeigher weigher);

    /**
     * Returns the set of all disjoint shortest path pairs between the
     * specified source and destination elements and taking into consideration
     * the provided risk profile. The path is computed using the default
     * edge-weight function, which by default is hop-count.
     *
     * @param src         source device
     * @param dst         destination device
     * @param riskProfile map of edges to risk profiles
     * @return set of all shortest paths between the two devices
     */
    Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst,
                                       Map<Link, Object> riskProfile);

    /**
     * Returns the set of all disjoint shortest path pairs between the
     * specified source and destination elements and taking into consideration
     * the provided risk profile. The path is computed using the supplied
     * edge-weight function.
     *
     * @param src         source device
     * @param dst         destination device
     * @param weigher     edge-weight entity
     * @param riskProfile map of edges to risk profiles
     * @return set of all shortest paths between the two devices
     */
    Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst,
                                       LinkWeigher weigher,
                                       Map<Link, Object> riskProfile);

}
