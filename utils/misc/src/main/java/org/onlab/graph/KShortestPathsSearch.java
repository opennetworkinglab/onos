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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Runs K shortest paths algorithm on a provided directed graph.  Returns results in the form of an
 * InnerOrderedResult so iteration through the returned paths will return paths in ascending order according to the
 * provided EdgeWeight.
 */
public class KShortestPathsSearch<V extends Vertex, E extends Edge<V>> extends AbstractGraphPathSearch<V, E> {

    private final Logger log = getLogger(getClass());

    @Override
    public Result<V, E> search(Graph<V, E> graph, V src, V dst, EdgeWeight<V, E> weight, int maxPaths) {
        checkNotNull(src);
        checkNotNull(dst);
        //The modified edge weight removes any need to modify the original graph
        InnerEdgeWeighter modifiedWeighter = new InnerEdgeWeighter(checkNotNull(weight));
        checkArgument(maxPaths > 0);
        Graph<V, E> originalGraph = checkNotNull(graph);
        //the result contains the set of eventual results
        InnerOrderedResult result = new InnerOrderedResult(src, dst, maxPaths);
        ArrayList<Path<V, E>> resultPaths = new ArrayList<>(maxPaths);
        ArrayList<Path<V, E>> potentialPaths = Lists.newArrayList();

        DijkstraGraphSearch<V, E> dijkstraSearch = new DijkstraGraphSearch<>();
        Set<Path<V, E>> dijkstraResults = dijkstraSearch.search(originalGraph, src, dst, modifiedWeighter, 1).paths();
        //Checks if the dst was reachable
        if (dijkstraResults.size() == 0) {
            log.warn("No path was found.");
            return result;
        }
        //If it was reachable adds the first shortest path to the set of results
        resultPaths.add(dijkstraResults.iterator().next());

        for (int k = 1; k < maxPaths; k++) {

            for (int i = 0; i < (resultPaths.get(k - 1).edges().size() - 1); i++) {
                V spurNode = resultPaths.get(k - 1).edges().get(i).src();
                List<E> rootPathEdgeList = resultPaths.get(k - 1).edges().subList(0, i);

                for (Path<V, E> path : resultPaths) {
                    if (edgeListsAreEqual(rootPathEdgeList, path.edges().subList(0, i))) {
                        modifiedWeighter.removedEdges.add(path.edges().get(i));
                    }
                }

                //Effectively remove all nodes from the source path
                for (E edge : rootPathEdgeList) {
                    originalGraph.getEdgesFrom(edge.src()).forEach(e -> modifiedWeighter.removedEdges.add(e));
                    originalGraph.getEdgesTo(edge.src()).forEach(e -> modifiedWeighter.removedEdges.add(e));
                }

                dijkstraResults = dijkstraSearch.search(originalGraph, spurNode, dst, modifiedWeighter, 1).paths();
                if (dijkstraResults.size() != 0) {
                    Path<V, E> spurPath = dijkstraResults.iterator().next();
                    List<E> totalPath = new ArrayList<>(rootPathEdgeList);
                    spurPath.edges().forEach(e -> totalPath.add(e));
                    //The following line must use the original weighter not the modified weighter because the modified
                    //weighter will count -1 values used for modifying the graph and return an inaccurate cost.
                    potentialPaths.add(new DefaultPath<V, E>(totalPath,
                                                             calculatePathCost(weight, totalPath)));
                }

                //Restore all removed paths and nodes
                modifiedWeighter.removedEdges.clear();
            }
            if (potentialPaths.isEmpty()) {
                break;
            }
            potentialPaths.sort(new InnerPathComparator());
            resultPaths.add(potentialPaths.get(0));
            potentialPaths.remove(0);
        }
        result.pathSet.addAll(resultPaths);

        return result;
    }
    //Edge list equality is judges by shared endpoints, and shared endpoints should be the same
    private boolean edgeListsAreEqual(List<E> edgeListOne, List<E> edgeListTwo) {
        if (edgeListOne.size() != edgeListTwo.size()) {
            return false;
        }
        E edgeOne;
        E edgeTwo;
        for (int i = 0; i < edgeListOne.size(); i++) {
            edgeOne = edgeListOne.get(i);
            edgeTwo = edgeListTwo.get(i);
            if (!edgeOne.equals(edgeTwo)) {
                return false;
            }
        }
        return true;
    }

    private Double calculatePathCost(EdgeWeight<V, E> weighter, List<E> edges) {
        Double totalCost = 0.0;
        for (E edge : edges) {
            totalCost += weighter.weight(edge);
        }
        return totalCost;
    }

    /**
     * Weights edges to make them inaccessible if set, otherwise returns the result of the original EdgeWeight.
     */
    private class InnerEdgeWeighter implements EdgeWeight<V, E> {

        private Set<E> removedEdges = Sets.newConcurrentHashSet();
        private EdgeWeight<V, E> innerEdgeWeight;

        public InnerEdgeWeighter(EdgeWeight<V, E> weight) {
            this.innerEdgeWeight = weight;
        }

        @Override
        public double weight(E edge) {
            if (removedEdges.contains(edge)) {
                //THIS RELIES ON THE LOCAL DIJKSTRA ALGORITHM AVOIDING NEGATIVES
                return -1;
            } else {
                return innerEdgeWeight.weight(edge);
            }
        }
    }

    /**
     * A result modified to return paths ordered according to the provided comparator.
     */
    protected class InnerOrderedResult extends DefaultResult {

        private TreeSet<Path<V, E>> pathSet = new TreeSet<>(new InnerPathComparator());

        public InnerOrderedResult(V src, V dst) {
            super(src, dst);
        }

        public InnerOrderedResult(V src, V dst, int maxPaths) {
            super(src, dst, maxPaths);
        }

        @Override
        public Set<Path<V, E>> paths() {
            return ImmutableSet.copyOf(pathSet);
        }
    }

    /**
     * Provides a comparator to order the set of paths.
     */
    private class InnerPathComparator implements Comparator<Path<V, E>> {

        @Override
        public int compare(Path<V, E> pathOne, Path<V, E> pathTwo) {
            int comparisonValue = Double.compare(pathOne.cost(), pathTwo.cost());
            if  (comparisonValue != 0) {
                return comparisonValue;
            } else if (edgeListsAreEqual(pathOne.edges(), pathTwo.edges())) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}
