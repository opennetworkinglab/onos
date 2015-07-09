/*
 * Copyright 2015 Open Networking Laboratory
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
    public Result<V, E> search(Graph<V, E> graph, V src, V dst,
                               EdgeWeight<V, E> weight, int maxPaths) {

        if (weight == null) {
            weight = edge -> 1;
        }

        List<DisjointPathPair<V, E>> dpps = new ArrayList<>();

        final EdgeWeight weightf = weight;
        DefaultResult firstDijkstraS = (DefaultResult) super.search(graph, src, dst, weight, ALL_PATHS);
        DefaultResult firstDijkstra = (DefaultResult) super.search(graph, src, null, weight, ALL_PATHS);

        //choose an arbitrary shortest path to run Suurballe on
        Path<V, E> shortPath = null;
        if (firstDijkstraS.paths().size() == 0) {
            return firstDijkstraS;
        }
        for (Path p: firstDijkstraS.paths()) {
            shortPath = p;
            //transforms the graph so tree edges have 0 weight
            EdgeWeight<V, Edge<V>> modified = edge -> {
                if (classE().isInstance(edge)) {
                    return weightf.weight((E) (edge)) + firstDijkstra.cost(edge.src())
                            - firstDijkstra.cost(edge.dst());
                }
                return 0;
            };
            EdgeWeight<V, E> modified2 = edge ->
                    weightf.weight(edge) + firstDijkstra.cost(edge.src()) - firstDijkstra.cost(edge.dst());

            //create a residual graph g' by removing all src vertices and reversing 0 length path edges
            MutableGraph<V, Edge<V>> gt = mutableCopy(graph);

            Map<Edge<V>, E> revToEdge = new HashMap<>();
            graph.getEdgesTo(src).forEach(gt::removeEdge);
            for (E edge: shortPath.edges()) {
                gt.removeEdge(edge);
                Edge<V> reverse = new Edge<V>() {
                    final Edge<V> orig = edge;
                    public V src() {
                        return orig.dst();
                    }
                    public V dst() {
                        return orig.src();
                    }
                    public String toString() {
                        return "ReversedEdge " + "src=" + src() + " dst=" + dst();
                    }
                };
                revToEdge.put(reverse, edge);
                gt.addEdge(reverse);
            }

            //rerun dijkstra on the temporary graph to get a second path
            Result<V, Edge<V>> secondDijkstra;
            secondDijkstra = new DijkstraGraphSearch<V, Edge<V>>().search(gt, src, dst, modified, ALL_PATHS);

            Path<V, Edge<V>> residualShortPath = null;
            if (secondDijkstra.paths().size() == 0) {
                dpps.add(new DisjointPathPair<V, E>(shortPath, null));
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
                        if (classE().isInstance(edge)) {
                            roundTrip.addEdge((E) edge);
                        } else {
                            roundTrip.removeEdge(revToEdge.get(edge));
                        }
                    }
                }
                //Actually build the final result
                DefaultResult lastSearch = (DefaultResult) super.search(roundTrip, src, dst, weight, ALL_PATHS);
                Path<V, E> path1 = lastSearch.paths().iterator().next();
                path1.edges().forEach(roundTrip::removeEdge);

                Set<Path<V, E>> bckpaths = super.search(roundTrip, src, dst, weight, ALL_PATHS).paths();
                Path<V, E> backup = null;
                if (bckpaths.size() != 0) {
                    backup = bckpaths.iterator().next();
                }

                dpps.add(new DisjointPathPair<>(path1, backup));
            }
        }

        for (int i = dpps.size() - 1; i > 0; i--) {
            if (dpps.get(i).size() <= 1) {
                dpps.remove(i);
            }
        }

        return new Result<V, E>() {
            final DefaultResult search = firstDijkstra;

            public V src() {
                return src;
            }
            public V dst() {
                return dst;
            }
            public Set<Path<V, E>> paths() {
                Set<Path<V, E>> pathsD = new HashSet<>();
                int paths = 0;
                for (DisjointPathPair<V, E> path: dpps) {
                    pathsD.add((Path<V, E>) path);
                    paths++;
                    if (paths == maxPaths) {
                        break;
                    }
                }
                return pathsD;
            }
            public Map<V, Double> costs() {
                return search.costs();
            }
            public Map<V, Set<E>> parents() {
                return search.parents();
            }
        };
    }

    private Class<?> clazzV;

    public Class<?> classV() {
        return clazzV;
    }

    private Class<?> clazzE;

    public Class<?> classE() {
        return clazzE;
    }
    /**
     * Creates a mutable copy of an immutable graph.
     *
     * @param graph   immutable graph
     * @return mutable copy
     */
    public MutableGraph mutableCopy(Graph<V, E> graph) {
        clazzV = graph.getVertexes().iterator().next().getClass();
        clazzE = graph.getEdges().iterator().next().getClass();
        return new MutableAdjacencyListsGraph<V, E>(graph.getVertexes(), graph.getEdges());
    }
}

