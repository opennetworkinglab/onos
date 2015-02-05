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
import com.google.common.collect.ImmutableList;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.constraint.LinkTypeConstraint;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of point-to-point connectivity.
 */
public class PointToPointIntent extends ConnectivityIntent {

    private final ConnectPoint ingressPoint;
    private final ConnectPoint egressPoint;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and constraints.
     *
     * @param appId        application identifier
     * @param key          key of the intent
     * @param selector     traffic selector
     * @param treatment    treatment
     * @param ingressPoint ingress port
     * @param egressPoint  egress port
     * @param constraints  optional list of constraints
     * @throws NullPointerException if {@code ingressPoint} or {@code egressPoints} is null.
     */
    public PointToPointIntent(ApplicationId appId,
                              Key key,
                              TrafficSelector selector,
                              TrafficTreatment treatment,
                              ConnectPoint ingressPoint,
                              ConnectPoint egressPoint,
                              List<Constraint> constraints) {
        super(appId, key, Collections.emptyList(), selector, treatment, constraints);

        checkNotNull(ingressPoint);
        checkNotNull(egressPoint);
        checkArgument(!ingressPoint.equals(egressPoint),
                "ingress and egress should be different (ingress: %s, egress: %s)", ingressPoint, egressPoint);

        this.ingressPoint = ingressPoint;
        this.egressPoint = egressPoint;
    }

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and with built-in link type constraint to avoid optical links.
     *
     * @param appId        application identifier
     * @param selector     traffic selector
     * @param treatment    treatment
     * @param ingressPoint ingress port
     * @param egressPoint  egress port
     * @throws NullPointerException if {@code ingressPoint} or {@code egressPoints} is null.
     */
    public PointToPointIntent(ApplicationId appId, TrafficSelector selector,
                              TrafficTreatment treatment,
                              ConnectPoint ingressPoint,
                              ConnectPoint egressPoint) {
        this(appId, null, selector, treatment, ingressPoint, egressPoint,
             ImmutableList.of(new LinkTypeConstraint(false, Link.Type.OPTICAL)));
    }

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and constraints.
     *
     * @param appId        application identifier
     * @param selector     traffic selector
     * @param treatment    treatment
     * @param ingressPoint ingress port
     * @param egressPoint  egress port
     * @param constraints  optional list of constraints
     * @throws NullPointerException if {@code ingressPoint} or {@code egressPoints} is null.
     */
    public PointToPointIntent(ApplicationId appId, TrafficSelector selector,
                              TrafficTreatment treatment,
                              ConnectPoint ingressPoint,
                              ConnectPoint egressPoint,
                              List<Constraint> constraints) {
        super(appId, null, Collections.emptyList(), selector, treatment, constraints);

        checkNotNull(ingressPoint);
        checkNotNull(egressPoint);
        checkArgument(!ingressPoint.equals(egressPoint),
                "ingress and egress should be different (ingress: %s, egress: %s)", ingressPoint, egressPoint);

        this.ingressPoint = ingressPoint;
        this.egressPoint = egressPoint;
    }

    /**
     * Constructor for serializer.
     */
    protected PointToPointIntent() {
        super();
        this.ingressPoint = null;
        this.egressPoint = null;
    }

    /**
     * Returns the port on which the ingress traffic should be connected to
     * the egress.
     *
     * @return ingress port
     */
    public ConnectPoint ingressPoint() {
        return ingressPoint;
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
                .add("ingress", ingressPoint)
                .add("egress", egressPoint)
                .add("constraints", constraints())
                .toString();
    }

}
