/*
 * Copyright 2015-present Open Networking Foundation
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


import java.security.SecureRandom;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * SRLG Graph Search finds a pair of paths with disjoint risk groups; i.e
 * if one path goes through an edge in risk group 1, the other path will go
 * through no edges in risk group 1.
 */
public class SrlgGraphSearch<V extends Vertex, E extends Edge<V>>
        extends AbstractGraphPathSearch<V, E> {

    static final int ITERATIONS = 100;
    static final int POPSIZE = 50;

    boolean useSuurballe = false;

    static final double INF = 100000000.0;

    int numGroups;
    Map<E, Integer> riskGrouping;

    Graph<V, E> orig;
    V src, dst;
    EdgeWeigher<V, E> weigher;

    /**
     * Creates an SRLG graph search object with the given number
     * of groups and given risk mapping.
     *
     * @param   groups      the number of disjoint risk groups
     * @param   grouping    map linking edges to integral group assignments
     */
    public SrlgGraphSearch(int groups, Map<E, Integer> grouping) {
        numGroups = groups;
        riskGrouping = grouping;
    }

    /**
     * Creates an SRLG graph search object from a map, inferring
     * the number of groups and creating an integral mapping.
     *
     * @param   grouping    map linking edges to object group assignments,
     *                      with same-group status linked to equality
     */
    public SrlgGraphSearch(Map<E, Object> grouping) {
        if (grouping == null) {
            useSuurballe = true;
            return;
        }
        numGroups = 0;
        HashMap<Object, Integer> tmpMap = new HashMap<>();
        riskGrouping = new HashMap<>();
        for (E key: grouping.keySet()) {
            Object value = grouping.get(key);
            if (!tmpMap.containsKey(value)) {
                tmpMap.put(value, numGroups);
                numGroups++;
            }
            riskGrouping.put(key, tmpMap.get(value));
        }
    }

    @Override
    protected Result<V, E> internalSearch(Graph<V, E> graph, V src, V dst,
                               EdgeWeigher<V, E> weigher, int maxPaths) {
        if (maxPaths == ALL_PATHS) {
            maxPaths = POPSIZE;
        }
        if (useSuurballe) {
            return new SuurballeGraphSearch<V, E>().search(graph, src, dst, weigher, ALL_PATHS);
        }
        orig = graph;
        this.src = src;
        this.dst = dst;
        this.weigher = weigher;
        List<Subset> best = new GAPopulation<Subset>()
                .runGA(ITERATIONS, POPSIZE, maxPaths, new Subset(new boolean[numGroups]));
        Set<DisjointPathPair> dpps = new HashSet<DisjointPathPair>();
        for (Subset s: best) {
            dpps.addAll(s.buildPaths());
        }
        Result<V, E> firstDijkstra = new DijkstraGraphSearch<V, E>()
                .search(orig, src, dst, weigher, 1);
        return new Result<V, E>() {
            final DefaultResult search = (DefaultResult) firstDijkstra;

            public V src() {
                return src;
            }
            public V dst() {
                return dst;

            }
            public Set<Path<V, E>> paths() {
                Set<Path<V, E>> pathsD = new HashSet<>();
                for (DisjointPathPair<V, E> path: dpps) {
                    pathsD.add(path);
                }
                return pathsD;
            }
            public Map<V, Weight> costs() {
                return search.costs();

            }
            public Map<V, Set<E>> parents() {
                return search.parents();

            }
        };
    }

    //finds the shortest path in the graph given a subset of edge types to use
    private Result<V, E> findShortestPathFromSubset(boolean[] subset) {
        Graph<V, E> graph = orig;
        EdgeWeigher<V, E> modified = new EdgeWeigher<V, E>() {
            final boolean[] subsetF = subset;

            @Override
            public Weight weight(E edge) {
                if (subsetF[riskGrouping.get(edge)]) {
                    return weigher.weight(edge);
                }
                return weigher.getNonViableWeight();
            }

            @Override
            public Weight getInitialWeight() {
                return weigher.getInitialWeight();
            }

            @Override
            public Weight getNonViableWeight() {
                return weigher.getNonViableWeight();
            }
        };

        Result<V, E> res = new DijkstraGraphSearch<V, E>().search(graph, src, dst, modified, 1);
        return res;
    }
    /**
     * A subset is a type of GA organism that represents a subset of allowed shortest
     * paths (and its complement). Its fitness is determined by the sum of the weights
     * of the first two shortest paths.
     */
    class Subset implements GAOrganism {

        boolean[] subset;
        boolean[] not;
        Random r = new SecureRandom();

        /**
         * Creates a Subset from the given subset array.
         *
         * @param sub   subset array
         */
        public Subset(boolean[] sub) {
            subset = sub.clone();
            not = new boolean[subset.length];
            for (int i = 0; i < subset.length; i++) {
                not[i] = !subset[i];
            }
        }

        @Override
        public Comparable fitness() {
            Set<Path<V, E>> paths1 = findShortestPathFromSubset(subset).paths();
            Set<Path<V, E>> paths2 = findShortestPathFromSubset(not).paths();
            if (paths1.isEmpty() || paths2.isEmpty()) {
                return weigher.getNonViableWeight();
            }
            return paths1.iterator().next().cost().merge(paths2.iterator().next().cost());
        }

        @Override
        public void mutate() {
            int turns = r.nextInt((int) Math.sqrt(subset.length));
            while (turns > 0) {
                int choose = r.nextInt(subset.length);
                subset[choose] = !subset[choose];
                not[choose] = !not[choose];
                turns--;
            }
        }

        @Override
        public GAOrganism crossWith(GAOrganism org) {
            if (!(org.getClass().equals(getClass()))) {
                return this;
            }
            Subset other = (Subset) (org);
            boolean[] sub = new boolean[subset.length];
            for (int i = 0; i < subset.length; i++) {
                sub[i] = subset[i];
                if (r.nextBoolean()) {
                    sub[i] = other.subset[i];
                }
            }
            return new Subset(sub);
        }

        @Override
        public GAOrganism random() {
            boolean[] sub = new boolean[subset.length];
            for (int i = 0; i < sub.length; i++) {
                sub[i] = r.nextBoolean();
            }
            return new Subset(sub);
        }

        /**
         * Builds the set of disjoint path pairs for a given subset
         * using Dijkstra's algorithm on both the subset and complement
         * and returning all pairs with one from each set.
         *
         * @return all shortest disjoint paths given this subset
         */
        public Set<DisjointPathPair> buildPaths() {
            Set<DisjointPathPair> dpps = new HashSet<>();
            for (Path<V, E> path1: findShortestPathFromSubset(subset).paths()) {
                for (Path<V, E> path2: findShortestPathFromSubset(not).paths()) {
                    DisjointPathPair<V, E> dpp = new DisjointPathPair<>(path1, path2);
                    dpps.add(dpp);
                }
            }
            return dpps;
        }
    }
}
