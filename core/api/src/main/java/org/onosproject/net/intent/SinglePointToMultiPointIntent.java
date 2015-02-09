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
import java.util.Set;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of single source, multiple destination connectivity intent.
 */
public class SinglePointToMultiPointIntent extends ConnectivityIntent {

    private final ConnectPoint ingressPoint;
    private final Set<ConnectPoint> egressPoints;

    /**
     * Creates a new single-to-multi point connectivity intent.
     *
     * @param appId application identifier
     * @param selector traffic selector
     * @param treatment treatment
     * @param ingressPoint port on which traffic will ingress
     * @param egressPoints set of ports on which traffic will egress
     * @throws NullPointerException if {@code ingressPoint} or
     *             {@code egressPoints} is null
     * @throws IllegalArgumentException if the size of {@code egressPoints} is
     *             not more than 1
     */
    public SinglePointToMultiPointIntent(ApplicationId appId,
            TrafficSelector selector, TrafficTreatment treatment,
            ConnectPoint ingressPoint, Set<ConnectPoint> egressPoints) {
        this(appId, null, selector, treatment, ingressPoint, egressPoints, Collections.emptyList());
    }

    /**
     * Creates a new single-to-multi point connectivity intent.
     *
     * @param appId application identifier
     * @param key intent key
     * @param selector traffic selector
     * @param treatment treatment
     * @param ingressPoint port on which traffic will ingress
     * @param egressPoints set of ports on which traffic will egress
     * @param constraints constraints to apply to the intent
     * @throws NullPointerException if {@code ingressPoint} or
     *             {@code egressPoints} is null
     * @throws IllegalArgumentException if the size of {@code egressPoints} is
     *             not more than 1
     */
    public SinglePointToMultiPointIntent(ApplicationId appId,
            Key key,
            TrafficSelector selector, TrafficTreatment treatment,
            ConnectPoint ingressPoint, Set<ConnectPoint> egressPoints,
            List<Constraint> constraints) {
        super(appId, key, Collections.emptyList(), selector, treatment, constraints);
        checkNotNull(egressPoints);
        checkNotNull(ingressPoint);
        checkArgument(!egressPoints.isEmpty(), "Egress point set cannot be empty");
        checkArgument(!egressPoints.contains(ingressPoint),
                "Set of egresses should not contain ingress (ingress: %s)", ingressPoint);

        this.ingressPoint = checkNotNull(ingressPoint);
        this.egressPoints = Sets.newHashSet(egressPoints);
    }

    /**
     * Constructor for serializer.
     */
    protected SinglePointToMultiPointIntent() {
        super();
        this.ingressPoint = null;
        this.egressPoints = null;
    }

    /**
     * Returns the port on which the ingress traffic should be connected to the
     * egress.
     *
     * @return ingress port
     */
    public ConnectPoint ingressPoint() {
        return ingressPoint;
    }

    /**
     * Returns the set of ports on which the traffic should egress.
     *
     * @return set of egress ports
     */
    public Set<ConnectPoint> egressPoints() {
        return egressPoints;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("key", key())
                .add("appId", appId())
                .add("resources", resources())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("ingress", ingressPoint)
                .add("egress", egressPoints)
                .add("constraints", constraints())
                .toString();
    }

}
