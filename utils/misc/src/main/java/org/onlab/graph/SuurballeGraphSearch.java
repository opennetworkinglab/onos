/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onlab.graph;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Suurballe shortest-path graph search algorithm capable of finding both
 * a shortest path, as well as a backup shortest path, between a source and a destination
 * such that the sum of the path lengths is minimized.
 */
public class SuurballeGraphSearch<V extends Vertex, E extends Edge<V>> extends DijkstraGraphSearch<V, E> {

    @Override
    protected Result<V, E> internalSearch(Graph<V, E> graph, V src, V dst,
                               EdgeWeigher<V, E> weigher, int maxPaths) {
        // FIXME: This method needs to be refactored as it is difficult to follow and debug.

        // FIXME: There is a defect here triggered by 3+ edges between the same vertices,
        // which makes an attempt to produce looping paths. Protection against
        // this was added to AbstractGraphPathSearch, but the root issue remains here.

        // FIXME: There is a defect here where not all paths are truly disjoint.
        // This class needs to filter its own results to make sure that the
        // paths are indeed disjoint. Temporary fix for this is provided, but
        // the issue needs to be addressed through refactoring.

        EdgeWeigher weightf = weigher;
        DefaultResult firstDijkstraS = (DefaultResult) super.internalSearch(
                graph, src, dst, weigher, ALL_PATHS);
        DefaultResult firstDijkstra = (DefaultResult) super.internalSearch(
                graph, src, null, weigher, ALL_PATHS);

        //choose an arbitrary shortest path to run Suurballe on
        Path<V, E> shortPath = null;
        if (firstDijkstraS.paths().isEmpty()) {
            return firstDijkstraS;
        }

        DisjointPathResult result = new DisjointPathResult(firstDijkstra, src, dst, maxPaths);

        for (Path p: firstDijkstraS.paths()) {
            shortPath = p;
            //transforms the graph so tree edges have 0 weight
            EdgeWeigher<V, E> modified = new EdgeWeigher<V, E>() {
                @Override
                public Weight weight(E edge) {
                    return edge instanceof ReverseEdge ?
                            weightf.getInitialWeight() :
                            weightf.weight(edge).merge(firstDijkstra.cost(edge.src()))
                                    .subtract(firstDijkstra.cost(edge.dst()));
                }

                @Override
                public Weight getInitialWeight() {
                    return weightf.getInitialWeight();
                }

                @Override
                public Weight getNonViableWeight() {
                    return weightf.getNonViableWeight();
                }
            };

            EdgeWeigher<V, E> modified2 = new EdgeWeigher<V, E>() {
                @Override
                public Weight weight(E edge) {
                    return weightf.weight(edge).merge(firstDijkstra.cost(edge.src()))
                            .subtract(firstDijkstra.cost(edge.dst()));
                }

                @Override
                public Weight getInitialWeight() {
                    return weightf.getInitialWeight();
                }

                @Override
                public Weight getNonViableWeight() {
                    return weightf.getNonViableWeight();
                }
            };

            //create a residual graph g' by removing all src vertices and reversing 0 length path edges
            MutableGraph<V, E> gt = mutableCopy(graph);

            Map<E, E> revToEdge = new HashMap<>();
            graph.getEdgesTo(src).forEach(gt::removeEdge);
            for (E edge: shortPath.edges()) {
                gt.removeEdge(edge);
                Edge<V> reverse = new ReverseEdge<V>(edge);
                revToEdge.put((E) reverse, edge);
                gt.addEdge((E) reverse);
            }

            //rerun dijkstra on the temporary graph to get a second path
            Result<V, E> secondDijkstra = new DijkstraGraphSearch<V, E>()
                    .search(gt, src, dst, modified, ALL_PATHS);

            Path<V, E> residualShortPath = null;
            if (secondDijkstra.paths().isEmpty()) {
                result.dpps.add(new DisjointPathPair<>(shortPath, null));
                continue;
            }

            for (Path p2: secondDijkstra.paths()) {
                residualShortPath = p2;

                MutableGraph<V, E> roundTrip = mutableCopy(graph);

                List<E> tmp = roundTrip.getEdges().stream().collect(Collectors.toList());

                tmp.forEach(roundTrip::removeEdge);

                shortPath.edges().forEach(roundTrip::addEdge);

                if (residualShortPath != null) {
                    for (Edge<V> edge: residualShortPath.edges()) {
                        if (edge instanceof ReverseEdge) {
                            roundTrip.removeEdge(revToEdge.get(edge));
                        } else {
                            roundTrip.addEdge((E) edge);
                        }
                    }
                }
                //Actually build the final result
                DefaultResult lastSearch = (DefaultResult)
                        super.internalSearch(roundTrip, src, dst, weigher, ALL_PATHS);
                Path<V, E> primary = lastSearch.paths().iterator().next();
                primary.edges().forEach(roundTrip::removeEdge);

                Set<Path<V, E>> backups = super.internalSearch(roundTrip, src, dst,
                        weigher, ALL_PATHS).paths();

                // Find first backup path that does not share any nodes with the primary
                for (Path<V, E> backup : backups) {
                    if (isDisjoint(primary, backup)) {
                        result.dpps.add(new DisjointPathPair<>(primary, backup));
                        break;
                    }
                }
            }
        }

        for (int i = result.dpps.size() - 1; i > 0; i--) {
            if (result.dpps.get(i).size() <= 1) {
                result.dpps.remove(i);
            }
        }

        result.buildPaths();
        return result;
    }

    private boolean isDisjoint(Path<V, E> a, Path<V, E> b) {
        return Sets.intersection(vertices(a), vertices(b)).isEmpty();
    }

    private Set<V> vertices(Path<V, E> p) {
        Set<V> set = new HashSet<>();
        p.edges().forEach(e -> set.add(e.src()));
        set.remove(p.src());
        return set;
    }

    /**
     * Creates a mutable copy of an immutable graph.
     *
     * @param graph   immutable graph
     * @return mutable copy
     */
    private MutableGraph<V, E> mutableCopy(Graph<V, E> graph) {
        return new MutableAdjacencyListsGraph<>(graph.getVertexes(), graph.getEdges());
    }

    private static final class ReverseEdge<V extends Vertex> extends AbstractEdge<V> {
        private ReverseEdge(Edge<V> edge) {
            super(edge.dst(), edge.src());
        }

        @Override
        public String toString() {
            return "ReversedEdge " + "src=" + src() + " dst=" + dst();
        }
    }

    // Auxiliary result for disjoint path search
    private final class DisjointPathResult implements AbstractGraphPathSearch.Result<V, E> {

        private final Result<V, E> searchResult;
        private final V src, dst;
        private final int maxPaths;
        private final List<DisjointPathPair<V, E>> dpps = new ArrayList<>();
        private final Set<Path<V, E>> disjointPaths = new HashSet<>();

        private DisjointPathResult(Result<V, E> searchResult, V src, V dst, int maxPaths) {
            this.searchResult = searchResult;
            this.src = src;
            this.dst = dst;
            this.maxPaths = maxPaths;
        }

        @Override
        public V src() {
            return src;
        }

        @Override
        public V dst() {
            return dst;
        }

        @Override
        public Set<Path<V, E>> paths() {
            return disjointPaths;
        }

        private void buildPaths() {
            int paths = 0;
            for (DisjointPathPair<V, E> path: dpps) {
                disjointPaths.add(path);
                paths++;
                if (paths == maxPaths) {
                    break;
                }
            }
        }

        @Override
        public Map<V, Set<E>> parents() {
            return searchResult.parents();
        }

        @Override
        public Map<V, Weight> costs() {
            return searchResult.costs();
        }
    }
}

