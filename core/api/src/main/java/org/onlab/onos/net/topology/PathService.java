package org.onlab.onos.net.topology;

import org.onlab.onos.net.ElementId;
import org.onlab.onos.net.Path;

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
     * @return set of all shortest paths between the two element
     */
    Set<Path> getPaths(ElementId src, ElementId dst, LinkWeight weight);

}
