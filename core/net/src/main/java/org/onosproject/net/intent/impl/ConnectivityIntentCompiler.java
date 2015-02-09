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
package org.onosproject.net.intent.impl;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ElementId;
import org.onosproject.net.Path;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.LinkResourceService;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;

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
    protected LinkResourceService resourceService;

    /**
     * Returns an edge-weight capable of evaluating links on the basis of the
     * specified constraints.
     *
     * @param constraints path constraints
     * @return edge-weight function
     */
    protected LinkWeight weight(List<Constraint> constraints) {
        return new ConstraintBasedLinkWeight(constraints);
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
            if (!constraint.validate(path, resourceService)) {
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
    protected Path getPath(ConnectivityIntent intent,
                           ElementId one, ElementId two) {
        Set<Path> paths = pathService.getPaths(one, two, weight(intent.constraints()));
        final List<Constraint> constraints = intent.constraints();
        ImmutableList<Path> filtered = FluentIterable.from(paths)
                .filter(new Predicate<Path>() {
                    @Override
                    public boolean apply(Path path) {
                        return checkPath(path, constraints);
                    }
                }).toList();
        if (filtered.isEmpty()) {
            throw new PathNotFoundException(one, two);
        }
        // TODO: let's be more intelligent about this eventually
        return filtered.iterator().next();
    }

    /**
     * Edge-weight capable of evaluating link cost using a set of constraints.
     */
    protected class ConstraintBasedLinkWeight implements LinkWeight {

        private final List<Constraint> constraints;

        /**
         * Creates a new edge-weight function capable of evaluating links
         * on the basis of the specified constraints.
         *
         * @param constraints path constraints
         */
        ConstraintBasedLinkWeight(List<Constraint> constraints) {
            if (constraints == null) {
                this.constraints = Collections.emptyList();
            } else {
                this.constraints = ImmutableList.copyOf(constraints);
            }
        }

        @Override
        public double weight(TopologyEdge edge) {
            if (!constraints.iterator().hasNext()) {
                return 1.0;
            }

            // iterate over all constraints in order and return the weight of
            // the first one with fast fail over the first failure
            Iterator<Constraint> it = constraints.iterator();

            double cost = it.next().cost(edge.link(), resourceService);
            while (it.hasNext() && cost > 0) {
                if (it.next().cost(edge.link(), resourceService) < 0) {
                    return -1;
                }
            }
            return cost;

        }
    }

}
