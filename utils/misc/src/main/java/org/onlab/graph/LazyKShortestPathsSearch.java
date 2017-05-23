/*
 * Copyright 2017-present Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.base.Suppliers;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 * Lazily runs K shortest paths algorithm on a provided directed graph.
 */
public class LazyKShortestPathsSearch<V extends Vertex, E extends Edge<V>> {

    private final Comparator<Path<V, E>> pathComparator = new PathComparator();

    private final GraphPathSearch<V, E> shortest = new DijkstraGraphSearch<>();

    /**
     * Searches the specified graph for paths between vertices.
     *
     * @param graph    graph to be searched
     * @param src      source vertex
     * @param dst      destination vertex
     * @param weigher  edge-weigher
     * @return Stream of shortest paths
     */
    public Stream<Path<V, E>> lazyPathSearch(Graph<V, E> graph,
                                              V src, V dst,
                                              EdgeWeigher<V, E> weigher) {

        Iterator<Path<V, E>> it = new ShortestPathIterator(graph, src, dst, weigher);

        return StreamSupport.stream(spliteratorUnknownSize(it,
                                                           Spliterator.ORDERED |
                                                           Spliterator.DISTINCT |
                                                           Spliterator.NONNULL |
                                                           Spliterator.IMMUTABLE),
                                    false);
    }

    /**
     * Iterator returning shortest paths, searched incrementally on each next() call.
     */
    private final class ShortestPathIterator implements Iterator<Path<V, E>> {

        final Graph<V, E> graph;
        final V src;
        final V dst;
        final EdgeWeigher<V, E> weigher;

        final InnerEdgeWeigher maskingWeigher;

        final List<Path<V, E>> resultPaths = new ArrayList<>(); // A
        final Queue<Path<V, E>> potentialPaths = new PriorityQueue<>(pathComparator); // B

        Supplier<Path<V, E>> next;

        ShortestPathIterator(Graph<V, E> graph,
                             V src, V dst,
                             EdgeWeigher<V, E> weigher) {
            this.graph = checkNotNull(graph);
            this.src = checkNotNull(src);
            this.dst = checkNotNull(dst);
            this.weigher = checkNotNull(weigher);

            maskingWeigher = new InnerEdgeWeigher(weigher);
            next = Suppliers.ofInstance(
                        shortest.search(graph, src, dst, weigher, 1)
                            .paths().stream().findFirst().orElse(null));
        }

        @Override
        public boolean hasNext() {
            return next.get() != null;
        }

        @Override
        public Path<V, E> next() {
            if (next.get() == null) {
                throw new NoSuchElementException("No more path between " + src + "-" + dst);
            }

            // lastPath: the path to return at the end of this call
            Path<V, E> lastPath = next.get();
            resultPaths.add(lastPath);

            next = Suppliers.memoize(() -> computeNext(lastPath));

            return lastPath;
        }

        private Path<V, E> computeNext(Path<V, E> lastPath) {
            /// following is basically Yen's k-shortest path algorithm

            // start searching for next path
            for (int i = 0; i < lastPath.edges().size(); i++) {
                V spurNode = lastPath.edges().get(i).src();
                List<E> rootPathEdgeList = lastPath.edges().subList(0, i);

                for (Path<V, E> path : resultPaths) {
                    if (path.edges().size() >= i &&
                        rootPathEdgeList.equals(path.edges().subList(0, i))) {
                        maskingWeigher.excluded.add(path.edges().get(i));
                    }
                }

                // Effectively remove all root path nodes other than spurNode
                rootPathEdgeList.forEach(edge -> {
                    maskingWeigher.excluded.addAll(graph.getEdgesFrom(edge.src()));
                    maskingWeigher.excluded.addAll(graph.getEdgesTo(edge.src()));
                });

                shortest.search(graph, spurNode, dst, maskingWeigher, 1)
                        .paths().stream().findAny().ifPresent(spurPath -> {

                            List<E> totalPath = ImmutableList.<E>builder()
                                    .addAll(rootPathEdgeList)
                                    .addAll(spurPath.edges())
                                    .build();
                            potentialPaths.add(path(totalPath));
                });

                // Restore all removed paths and nodes
                maskingWeigher.excluded.clear();
            }

            if (potentialPaths.isEmpty()) {
                return null;
            } else {
                return potentialPaths.poll();
            }
        }

        private Path<V, E> path(List<E> edges) {
            //The following line must use the original weigher not the modified weigher because the modified
            //weigher will count -1 values used for modifying the graph and return an inaccurate cost.
            return new DefaultPath<>(edges, calculatePathCost(weigher, edges));
        }

        private Weight calculatePathCost(EdgeWeigher<V, E> weighter, List<E> edges) {
            Weight totalCost = weighter.getInitialWeight();
            for (E edge : edges) {
                totalCost = totalCost.merge(weighter.weight(edge));
            }
            return totalCost;
        }
    }

    /**
     * EdgeWeigher which excludes specified edges from path computation.
     */
    private final class InnerEdgeWeigher implements EdgeWeigher<V, E> {

        private final Set<E> excluded = Sets.newConcurrentHashSet();
        private final EdgeWeigher<V, E> weigher;

        private InnerEdgeWeigher(EdgeWeigher<V, E> weigher) {
            this.weigher = weigher;
        }

        @Override
        public Weight weight(E edge) {
            if (excluded.contains(edge)) {
                return weigher.getNonViableWeight();
            }
            return weigher.weight(edge);
        }

        @Override
        public Weight getInitialWeight() {
            return weigher.getInitialWeight();
        }

        @Override
        public Weight getNonViableWeight() {
            return weigher.getNonViableWeight();
        }
    }

    /**
     * Provides a comparator to order the set of paths.
     * Compare by cost, then by hop count.
     */
    private final class PathComparator implements Comparator<Path<V, E>> {

        @Override
        public int compare(Path<V, E> pathOne, Path<V, E> pathTwo) {
            return ComparisonChain.start()
                    .compare(pathOne.cost(), pathTwo.cost())
                    .compare(pathOne.edges().size(), pathTwo.edges().size())
                    .result();
        }
    }

}
