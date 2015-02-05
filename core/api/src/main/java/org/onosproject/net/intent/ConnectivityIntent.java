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

import com.google.common.collect.ImmutableSet;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Link;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of connectivity intent for traffic matching some criteria.
 */
public abstract class ConnectivityIntent extends Intent {

    // TODO: other forms of intents should be considered for this family:
    //   point-to-point with constraints (waypoints/obstacles)
    //   multi-to-single point with constraints (waypoints/obstacles)
    //   single-to-multi point with constraints (waypoints/obstacles)
    //   concrete path (with alternate)
    //   ...

    private final TrafficSelector selector;
    private final TrafficTreatment treatment;
    private final List<Constraint> constraints;

    /**
     * Creates a connectivity intent that matches on the specified selector
     * and applies the specified treatment.
     * <p>
     * Path will be chosen without any constraints.
     * </p>
     *
     * @param appId     application identifier
     * @param resources required network resources (optional)
     * @param selector  traffic selector
     * @param treatment treatment
     * @throws NullPointerException if the selector or treatment is null
     */
    protected ConnectivityIntent(ApplicationId appId,
                                 Collection<NetworkResource> resources,
                                 TrafficSelector selector,
                                 TrafficTreatment treatment) {
        this(appId, null, resources, selector, treatment, Collections.emptyList());
    }

    /**
     * Creates a connectivity intent that matches on the specified selector
     * and applies the specified treatment.
     * <p>
     * Path will be chosen without any constraints.
     * </p>
     *
     * @param appId     application identifier
     * @param key       intent key
     * @param resources required network resources (optional)
     * @param selector  traffic selector
     * @param treatment treatment
     * @throws NullPointerException if the selector or treatment is null
     */
    protected ConnectivityIntent(ApplicationId appId,
                                 Key key,
                                 Collection<NetworkResource> resources,
                                 TrafficSelector selector,
                                 TrafficTreatment treatment) {
        this(appId, key, resources, selector, treatment, Collections.emptyList());
    }

    /**
     * Creates a connectivity intent that matches on the specified selector
     * and applies the specified treatment.
     * <p>
     * Path will be optimized based on the first constraint if one is given.
     * </p>
     *
     * @param appId       application identifier
     * @param key         explicit key to use for intent
     * @param resources   required network resources (optional)
     * @param selector    traffic selector
     * @param treatment   treatment
     * @param constraints optional prioritized list of constraints
     * @throws NullPointerException if the selector or treatment is null
     */

    protected ConnectivityIntent(ApplicationId appId,
                                 Key key,
                                 Collection<NetworkResource> resources,
                                 TrafficSelector selector,
                                 TrafficTreatment treatment,
                                 List<Constraint> constraints) {
        super(appId, key, resources);
        this.selector = checkNotNull(selector);
        this.treatment = checkNotNull(treatment);
        this.constraints = checkNotNull(constraints);
    }

    /**
     * Creates a connectivity intent that matches on the specified selector
     * and applies the specified treatment.
     * <p>
     * Path will be optimized based on the first constraint if one is given.
     * </p>
     *
     * @param appId       application identifier
     * @param resources   required network resources (optional)
     * @param selector    traffic selector
     * @param treatment   treatment
     * @param constraints optional prioritized list of constraints
     * @throws NullPointerException if the selector or treatment is null
     */

    protected ConnectivityIntent(ApplicationId appId,
                                 Collection<NetworkResource> resources,
                                 TrafficSelector selector,
                                 TrafficTreatment treatment,
                                 List<Constraint> constraints) {
        super(appId, null, resources);
        this.selector = checkNotNull(selector);
        this.treatment = checkNotNull(treatment);
        this.constraints = checkNotNull(constraints);
    }

    /**
     * Constructor for serializer.
     */
    protected ConnectivityIntent() {
        super();
        this.selector = null;
        this.treatment = null;
        this.constraints = Collections.emptyList();
    }

    /**
     * Returns the match specifying the type of traffic.
     *
     * @return traffic match
     */
    public TrafficSelector selector() {
        return selector;
    }

    /**
     * Returns the action applied to the traffic.
     *
     * @return applied action
     */
    public TrafficTreatment treatment() {
        return treatment;
    }

    /**
     * Returns the set of connectivity constraints.
     *
     * @return list of intent constraints
     */
    public List<Constraint> constraints() {
        return constraints;
    }

    /**
     * Produces a collection of network resources from the given links.
     *
     * @param links collection of links
     * @return collection of link resources
     */
    protected static Collection<NetworkResource> resources(Collection<Link> links) {
        return ImmutableSet.copyOf(links);
    }

}
