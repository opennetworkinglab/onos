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
package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.graph.DefaultEdgeWeigher;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.ElementId;
import org.onosproject.net.Path;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.constraint.HashedPathSelectionConstraint;
import org.onosproject.net.intent.impl.PathNotFoundException;
import org.onosproject.net.resource.ResourceQueryService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Base class for compilers of various
 * {@link org.onosproject.net.intent.ConnectivityIntent connectivity intents}.
 */
@Component(immediate = true)
public abstract class ConnectivityIntentCompiler<T extends ConnectivityIntent>
        implements IntentCompiler<T> {

    private static final ProviderId PID = new ProviderId("core", "org.onosproject.core", true);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceQueryService resourceService;

    /**
     * Returns an edge-weight capable of evaluating links on the basis of the
     * specified constraints.
     *
     * @param constraints path constraints
     * @return edge-weight function
     */
    protected LinkWeigher weigher(List<Constraint> constraints) {
        return new ConstraintBasedLinkWeigher(constraints);
    }

    /**
     * Validates the specified path against the given constraints.
     *
     * @param path        path to be checked
     * @param constraints path constraints
     * @return true if the path passes all constraints
     */
    protected boolean checkPath(Path path, List<Constraint> constraints) {
        for (Constraint constraint : constraints) {
            if (!constraint.validate(path, resourceService::isAvailable)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes a path between two ConnectPoints.
     *
     * @param intent intent on which behalf path is being computed
     * @param one    start of the path
     * @param two    end of the path
     * @return Path between the two
     * @throws PathNotFoundException if a path cannot be found
     */
    @Deprecated
    protected Path getPathOrException(ConnectivityIntent intent,
                                      ElementId one, ElementId two) {
        Path path = getPath(intent, one, two);
        if (path == null) {
            throw new PathNotFoundException(one, two);
        }
        // TODO: let's be more intelligent about this eventually
        return path;
    }

    /**
     * Computes a path between two ConnectPoints.
     *
     * @param intent intent on which behalf path is being computed
     * @param one    start of the path
     * @param two    end of the path
     * @return Path between the two, or null if no path can be found
     */
    protected Path getPath(ConnectivityIntent intent,
                           ElementId one, ElementId two) {
        Set<Path> paths = pathService.getPaths(one, two, weigher(intent.constraints()));
        final List<Constraint> constraints = intent.constraints();
        ImmutableList<Path> filtered = FluentIterable.from(paths)
                .filter(path -> checkPath(path, constraints))
                .toList();
        if (filtered.isEmpty()) {
            return null;
        }

        if (constraints.stream().anyMatch(c -> c instanceof HashedPathSelectionConstraint)) {
            return filtered.get(intent.hashCode() % filtered.size());
        }

        return filtered.iterator().next();
    }

    /**
     * Computes a disjoint path between two ConnectPoints.
     *
     * @param intent intent on which behalf path is being computed
     * @param one    start of the path
     * @param two    end of the path
     * @return DisjointPath         between the two
     * @throws PathNotFoundException if two paths cannot be found
     */
    protected DisjointPath getDisjointPath(ConnectivityIntent intent,
                           ElementId one, ElementId two) {
        Set<DisjointPath> paths = pathService.getDisjointPaths(one, two, weigher(intent.constraints()));
        final List<Constraint> constraints = intent.constraints();
        ImmutableList<DisjointPath> filtered = FluentIterable.from(paths)
                .filter(path -> checkPath(path, constraints))
                .toList();
        if (filtered.isEmpty()) {
            throw new PathNotFoundException(one, two);
        }

        if (constraints.stream().anyMatch(c -> c instanceof HashedPathSelectionConstraint)) {
            return filtered.get(intent.hashCode() % filtered.size());
        }

        return filtered.iterator().next();
    }

    /**
     * Edge-weight capable of evaluating link cost using a set of constraints.
     */
    protected class ConstraintBasedLinkWeigher extends DefaultEdgeWeigher<TopologyVertex, TopologyEdge>
            implements LinkWeigher {

        private final List<Constraint> constraints;

        /**
         * Creates a new edge-weight function capable of evaluating links
         * on the basis of the specified constraints.
         *
         * @param constraints path constraints
         */
        ConstraintBasedLinkWeigher(List<Constraint> constraints) {
            if (constraints == null) {
                this.constraints = Collections.emptyList();
            } else {
                this.constraints = ImmutableList.copyOf(constraints);
            }
        }

        @Override
        public Weight weight(TopologyEdge edge) {

            // iterate over all constraints in order and return the weight of
            // the first one with fast fail over the first failure
            Iterator<Constraint> it = constraints.iterator();

            if (!it.hasNext()) {
                return new ScalarWeight(HOP_WEIGHT_VALUE);
            }

            double cost = it.next().cost(edge.link(), resourceService::isAvailable);
            while (it.hasNext() && cost > 0) {
                if (it.next().cost(edge.link(), resourceService::isAvailable) < 0) {
                    cost = -1;
                }
            }
            return new ScalarWeight(cost);

        }
    }

}
