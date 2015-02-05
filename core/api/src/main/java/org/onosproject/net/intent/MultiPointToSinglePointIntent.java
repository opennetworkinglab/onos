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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of multiple source to single destination connectivity intent.
 */
public final class MultiPointToSinglePointIntent extends ConnectivityIntent {

    private final Set<ConnectPoint> ingressPoints;
    private final ConnectPoint egressPoint;

    /**
     * Creates a new multi-to-single point connectivity intent for the specified
     * traffic selector and treatment.
     *
     * @param appId         application identifier
     * @param selector      traffic selector
     * @param treatment     treatment
     * @param ingressPoints set of ports from which ingress traffic originates
     * @param egressPoint   port to which traffic will egress
     * @throws NullPointerException     if {@code ingressPoints} or
     *                                  {@code egressPoint} is null.
     * @throws IllegalArgumentException if the size of {@code ingressPoints} is
     *                                  not more than 1
     */
    public MultiPointToSinglePointIntent(ApplicationId appId,
                                         TrafficSelector selector,
                                         TrafficTreatment treatment,
                                         Set<ConnectPoint> ingressPoints,
                                         ConnectPoint egressPoint) {
        this(appId, selector, treatment, ingressPoints, egressPoint, Collections.emptyList());
    }

    /**
     * Creates a new multi-to-single point connectivity intent for the specified
     * traffic selector and treatment.
     *
     * @param appId         application identifier
     * @param selector      traffic selector
     * @param treatment     treatment
     * @param ingressPoints set of ports from which ingress traffic originates
     * @param egressPoint   port to which traffic will egress
     * @param constraints   constraints to apply to the intent
     * @throws NullPointerException     if {@code ingressPoints} or
     *                                  {@code egressPoint} is null.
     * @throws IllegalArgumentException if the size of {@code ingressPoints} is
     *                                  not more than 1
     */
    public MultiPointToSinglePointIntent(ApplicationId appId,
                                         Key key,
                                         TrafficSelector selector,
                                         TrafficTreatment treatment,
                                         Set<ConnectPoint> ingressPoints,
                                         ConnectPoint egressPoint,
                                         List<Constraint> constraints) {
        super(appId, key, Collections.emptyList(), selector, treatment, constraints);

        checkNotNull(ingressPoints);
        checkArgument(!ingressPoints.isEmpty(), "Ingress point set cannot be empty");
        checkNotNull(egressPoint);
        checkArgument(!ingressPoints.contains(egressPoint),
                "Set of ingresses should not contain egress (egress: %s)", egressPoint);

        this.ingressPoints = Sets.newHashSet(ingressPoints);
        this.egressPoint = egressPoint;
    }

    /**
     * Creates a new multi-to-single point connectivity intent for the specified
     * traffic selector and treatment.
     *
     * @param appId         application identifier
     * @param selector      traffic selector
     * @param treatment     treatment
     * @param ingressPoints set of ports from which ingress traffic originates
     * @param egressPoint   port to which traffic will egress
     * @param constraints   constraints to apply to the intent
     * @throws NullPointerException     if {@code ingressPoints} or
     *                                  {@code egressPoint} is null.
     * @throws IllegalArgumentException if the size of {@code ingressPoints} is
     *                                  not more than 1
     */
    public MultiPointToSinglePointIntent(ApplicationId appId,
                                         TrafficSelector selector,
                                         TrafficTreatment treatment,
                                         Set<ConnectPoint> ingressPoints,
                                         ConnectPoint egressPoint,
                                         List<Constraint> constraints) {
        this(appId, null, selector, treatment, ingressPoints, egressPoint, constraints);
    }

    /**
     * Constructor for serializer.
     */
    protected MultiPointToSinglePointIntent() {
        super();
        this.ingressPoints = null;
        this.egressPoint = null;
    }

    /**
     * Returns the set of ports on which ingress traffic should be connected to
     * the egress port.
     *
     * @return set of ingress ports
     */
    public Set<ConnectPoint> ingressPoints() {
        return ingressPoints;
    }

    /**
     * Returns the port on which the traffic should egress.
     *
     * @return egress port
     */
    public ConnectPoint egressPoint() {
        return egressPoint;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("resources", resources())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("ingress", ingressPoints())
                .add("egress", egressPoint())
                .add("constraints", constraints())
                .toString();
    }
}
