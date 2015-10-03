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
package org.onosproject.net.intent;

import java.util.List;

import com.google.common.annotations.Beta;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Abstraction of explicitly path specified connectivity intent.
 */
@Beta
public class PathIntent extends ConnectivityIntent {

    private final Path path;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and using the specified explicit path.
     *
     * @param appId     application identifier
     * @param selector  traffic selector
     * @param treatment treatment
     * @param path      traversed links
     * @param constraints  optional list of constraints
     * @param priority  priority to use for the generated flows
     * @throws NullPointerException {@code path} is null
     */
    protected PathIntent(ApplicationId appId,
                         TrafficSelector selector,
                         TrafficTreatment treatment,
                         Path path,
                         List<Constraint> constraints,
                         int priority) {
        super(appId, null, resources(path.links()), selector, treatment, constraints,
                priority);
        PathIntent.validate(path.links());
        this.path = path;
    }

    /**
     * Constructor for serializer.
     */
    protected PathIntent() {
        super();
        this.path = null;
    }

    /**
     * Returns a new host to host intent builder.
     *
     * @return host to host intent builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of a host to host intent.
     */
    public static class Builder extends ConnectivityIntent.Builder {
        Path path;

        protected Builder() {
            // Hide default constructor
        }

        @Override
        public Builder appId(ApplicationId appId) {
            return (Builder) super.appId(appId);
        }

        @Override
        public Builder key(Key key) {
            return (Builder) super.key(key);
        }

        @Override
        public Builder selector(TrafficSelector selector) {
            return (Builder) super.selector(selector);
        }

        @Override
        public Builder treatment(TrafficTreatment treatment) {
            return (Builder) super.treatment(treatment);
        }

        @Override
        public Builder constraints(List<Constraint> constraints) {
            return (Builder) super.constraints(constraints);
        }

        @Override
        public Builder priority(int priority) {
            return (Builder) super.priority(priority);
        }

        /**
         * Sets the path of the intent that will be built.
         *
         * @param path path for the intent
         * @return this builder
         */
        public Builder path(Path path) {
            this.path = path;
            return this;
        }

        /**
         * Builds a path intent from the accumulated parameters.
         *
         * @return point to point intent
         */
        public PathIntent build() {

            return new PathIntent(
                    appId,
                    selector,
                    treatment,
                    path,
                    constraints,
                    priority
            );
        }
    }



    // NOTE: This methods takes linear time with the number of links.
    /**
     * Validates that source element ID and destination element ID of a link are
     * different for the specified all links and that destination element ID of a link and source
     * element ID of the next adjacent source element ID are same for the specified all links.
     *
     * @param links links to be validated
     */
    public static void validate(List<Link> links) {
        checkArgument(Iterables.all(links, link -> !link.src().elementId().equals(link.dst().elementId())),
                "element of src and dst in a link must be different: {}", links);

        boolean adjacentSame = true;
        for (int i = 0; i < links.size() - 1; i++) {
            if (!links.get(i).dst().elementId().equals(links.get(i + 1).src().elementId())) {
                adjacentSame = false;
                break;
            }
        }
        checkArgument(adjacentSame, "adjacent links must share the same element: {}", links);
    }

    /**
     * Returns the links which the traffic goes along.
     *
     * @return traversed links
     */
    public Path path() {
        return path;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("priority", priority())
                .add("resources", resources())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("constraints", constraints())
                .add("path", path)
                .toString();
    }

}
