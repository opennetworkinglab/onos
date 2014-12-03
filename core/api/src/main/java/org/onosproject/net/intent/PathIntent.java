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
package org.onosproject.net.intent;

import java.util.Collections;
import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Abstraction of explicitly path specified connectivity intent.
 */
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
     * @throws NullPointerException {@code path} is null
     */
    public PathIntent(ApplicationId appId, TrafficSelector selector,
                      TrafficTreatment treatment, Path path) {
        this(appId, selector, treatment, path, Collections.emptyList());
    }

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and using the specified explicit path.
     *
     * @param appId     application identifier
     * @param selector  traffic selector
     * @param treatment treatment
     * @param path      traversed links
     * @param constraints  optional list of constraints
     * @throws NullPointerException {@code path} is null
     */
    public PathIntent(ApplicationId appId, TrafficSelector selector,
                      TrafficTreatment treatment, Path path, List<Constraint> constraints) {
        super(appId, resources(path.links()), selector, treatment, constraints);
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

    // NOTE: This methods takes linear time with the number of links.
    /**
     * Validates that source element ID and destination element ID of a link are
     * different for the specified all links and that destination element ID of a link and source
     * element ID of the next adjacent source element ID are same for the specified all links.
     *
     * @param links links to be validated
     */
    public static void validate(List<Link> links) {
        checkArgument(Iterables.all(links, new Predicate<Link>() {
            @Override
            public boolean apply(Link link) {
                return !link.src().elementId().equals(link.dst().elementId());
            }
        }), "element of src and dst in a link must be different: {}", links);

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
    public boolean isInstallable() {
        return true;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("constraints", constraints())
                .add("path", path)
                .toString();
    }

}
