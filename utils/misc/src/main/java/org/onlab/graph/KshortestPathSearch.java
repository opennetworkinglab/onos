/*
 * Copyright 2014-2015 Open Networking Laboratory
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
//import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
//import java.util.Map;
//import java.util.PriorityQueue;
import java.util.Set;

import static org.onlab.graph.GraphPathSearch.ALL_PATHS;

/**
 * K-shortest-path graph search algorithm capable of finding not just one,
 * but K shortest paths with ascending order between the source and destinations.
 */

public class KshortestPathSearch<V extends Vertex, E extends Edge<V>> {

    // Define class variables.
    private Graph<V, E> immutableGraph;
    private MutableGraph<V, E> mutableGraph;
    private List<List<E>> pathResults = new ArrayList<List<E>>();
    private List<List<E>> pathCandidates = new ArrayList<List<E>>();
    private V source;
    private V sink;
    private int numK = 0;
    private EdgeWeight<V, E> weight =  null;
    // private PriorityQueue<List<E>> pathCandidates = new PriorityQueue<List<E>>();

    // Initialize the graph.
    public KshortestPathSearch(Graph<V, E> graph) {
        immutableGraph = graph;
        mutableGraph = new MutableAdjacencyListsGraph<>(graph.getVertexes(),
                graph.getEdges());
    }

    public List<List<E>> search(V src,
            V dst,
            EdgeWeight<V, E> wei,
            int k) {

        weight = wei;
        source = src;
        sink = dst;
        numK = k;
        // pathCandidates = new PriorityQueue<List<E>>();

        pathResults.clear();
        pathCandidates.clear();

        // Double check the parameters
        checkArguments(immutableGraph, src, dst, numK);

        // DefaultResult result = new DefaultResult(src, dst);

        searchKShortestPaths();

        return pathResults;
    }

    private void checkArguments(Graph<V, E> graph, V src, V dst, int k) {
            if (graph == null) {
                throw new NullPointerException("graph is null");
            }
            if (!graph.getVertexes().contains(src)) {
                throw new NullPointerException("source node does not exist");
            }
            if (!graph.getVertexes().contains(dst)) {
                throw new NullPointerException("target node does not exist");
            }
            if (k <= 0) {
                throw new NullPointerException("K is negative or 0");
            }
            if (weight == null) {
                throw new NullPointerException("the cost matrix is null");
            }
    }

    private void searchKShortestPaths() {
            // Step 1: find the shortest path.
            List<E> shortestPath = searchShortestPath(immutableGraph, source, sink);
            // no path exists, exit.
            if (shortestPath == null) {
                return;
            }

            // Step 2: update the results.
            pathResults.add(shortestPath);
            // pathCandidates.add(shortestPath);

            // Step 3: find the other K-1 paths.
            while (/*pathCandidates.size() > 0 &&*/pathResults.size() < numK) {
                // 3.1 the spur node ranges from the first node to the last node in the previous k-shortest path.
                List<E> lastPath = pathResults.get(pathResults.size() - 1);
                for (int i = 0; i < lastPath.size(); i++) {
                    // 3.1.1 convert the graph into mutable.
                    convertGraph();
                    // 3.1.2 transform the graph.
                    List<E> rootPath = createSpurNode(lastPath, i);
                    transformGraph(rootPath);
                    // 3.1.3 find the deviation node.
                    V devNode;
                    devNode = getDevNode(rootPath);
                    List<E> spurPath;
                    // 3.1.4 find the shortest path in the transformed graph.
                    spurPath = searchShortestPath(mutableGraph, devNode, sink);
                    // 3.1.5 update the path candidates.
                    if (spurPath != null) {
                        // totalPath = rootPath + spurPath;
                        rootPath.addAll(spurPath);
                        pathCandidates.add(rootPath);
                    }
                }
                // 3.2 if there is no spur path, exit.
                if (pathCandidates.size() == 0) {
                    break;
                }
                 // 3.3 add the path into the results.
                addPathResult();
            }
        }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<E> searchShortestPath(Graph<V, E> graph, V src, V dst) {
        // Determine the shortest path from the source to the destination by using the Dijkstra algorithm.
        DijkstraGraphSearch dijkstraAlg = new DijkstraGraphSearch();
        Set<Path> paths =  dijkstraAlg.search(graph, src, dst, weight, ALL_PATHS).paths();
        Iterator<Path> itr = paths.iterator();
        if (!itr.hasNext()) {
            return null;
        }
        // return the first shortest path only.
        return (List<E>) itr.next().edges();
    }

    private void convertGraph() {
        // clear the mutableGraph first
        if (mutableGraph != null) {
            ((MutableAdjacencyListsGraph) mutableGraph).clear();
        }

        // create a immutableGraph
        Set<E> copyEa = immutableGraph.getEdges();
        Set<V> copyVa = immutableGraph.getVertexes();
        for (V vertex : copyVa) {
            mutableGraph.addVertex(vertex);
            }
        for (E edge : copyEa) {
            mutableGraph.addEdge(edge);
            }
    }

    private V getDevNode(List<E> path) {
            V srcA;
            V dstB;

            if (path.size() == 0) {
                return source;
            }

            E temp1 = path.get(path.size() - 1);
            srcA = temp1.src();
            dstB = temp1.dst();

            if (path.size() == 1) {
                if (srcA.equals(source)) {
                    return dstB;
                } else {
                    return srcA;
                }
            } else {
                E temp2 = path.get(path.size() - 2);
                if (srcA.equals(temp2.src()) || srcA.equals(temp2.dst())) {
                    return dstB;
                } else {
                    return srcA;
                }
            }
          }

     private List<E> createSpurNode(List<E> path, int n) {
            List<E> root = new ArrayList<E>();

            for (int i = 0; i < n; i++) {
                root.add(path.get(i));
            }
            return root;
        }

        private void transformGraph(List<E> rootPath) {
            List<E> prePath;
            //remove edges
            for (int i = 0; i < pathResults.size(); i++) {
                prePath = pathResults.get(i);
                if (prePath.size() == 1) {
                    mutableGraph.removeEdge(prePath.get(0));
                } else if (comparePath(rootPath, prePath)) {
                    for (int j = 0; j <= rootPath.size(); j++) {
                        mutableGraph.removeEdge(prePath.get(j));
                    }
                }
            }
            for (int i = 0; i < pathCandidates.size(); i++) {
                prePath = pathCandidates.get(i);
                if (prePath.size() == 1) {
                    mutableGraph.removeEdge(prePath.get(0));
                } else if (comparePath(rootPath, prePath)) {
                    for (int j = 0; j <= rootPath.size(); j++) {
                        mutableGraph.removeEdge(prePath.get(j));
                    }
                }
            }

            if (rootPath.size() == 0) {
                return;
            }

            //remove nodes
            List<V> nodes = new ArrayList<V>();
            nodes.add(source);
            V pre = source;
            V srcA;
            V dstB;
            for (int i = 0; i < rootPath.size() - 1; i++) {
                E temp = rootPath.get(i);
                srcA = temp.src();
                dstB = temp.dst();

                if (srcA.equals(pre)) {
                    nodes.add(dstB);
                    pre = dstB;
                } else {
                    nodes.add(srcA);
                    pre = srcA;
                }
            }
            for (int i = 0; i < nodes.size(); i++) {
                mutableGraph.removeVertex(nodes.get(i));
            }
        }

        private boolean comparePath(List<E> path1, List<E> path2) {
            if (path1.size() > path2.size()) {
                return false;
            }
            if (path1.size() == 0) {
                return true;
            }
            for (int i = 0; i < path1.size(); i++) {
                if (path1.get(i) != path2.get(i)) {
                    return false;
                }
            }
            return true;
        }

        private void addPathResult() {
            List<E> sp;
            sp = pathCandidates.get(0);
            for (int i = 1; i < pathCandidates.size(); i++) {
                if (sp.size() > pathCandidates.get(i).size()) {
                    sp = pathCandidates.get(i);
                }
            }
            pathResults.add(sp);
            // Log.info(sp.toString());
            pathCandidates.remove(sp);
        }

}
