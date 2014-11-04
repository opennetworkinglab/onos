/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.net.intent;

import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.resource.BandwidthResourceRequest;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of point-to-point connectivity.
 */
public class PointToPointIntentWithBandwidthConstraint extends ConnectivityIntent {

    private final ConnectPoint ingressPoint;
    private final ConnectPoint egressPoint;
    private final BandwidthResourceRequest bandwidthResourceRequest;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports.
     *
     * @param appId        application identifier
     * @param selector     traffic selector
     * @param treatment    treatment
     * @param ingressPoint ingress port
     * @param egressPoint  egress port
     * @param bandwidthResourceRequest bandwidth resource request
     * @throws NullPointerException if {@code ingressPoint} or {@code egressPoints} is null.
     */
    public PointToPointIntentWithBandwidthConstraint(ApplicationId appId, TrafficSelector selector,
                                                     TrafficTreatment treatment,
                                                     ConnectPoint ingressPoint,
                                                     ConnectPoint egressPoint,
                                                     BandwidthResourceRequest bandwidthResourceRequest) {
        super(id(PointToPointIntentWithBandwidthConstraint.class, selector,
                 treatment, ingressPoint, egressPoint, bandwidthResourceRequest.bandwidth()),
              appId, null, selector, treatment);
        this.ingressPoint = checkNotNull(ingressPoint);
        this.egressPoint = checkNotNull(egressPoint);
        this.bandwidthResourceRequest = bandwidthResourceRequest;
    }

    /**
     * Constructor for serializer.
     */
    protected PointToPointIntentWithBandwidthConstraint() {
        super();
        this.ingressPoint = null;
        this.egressPoint = null;
        bandwidthResourceRequest = new BandwidthResourceRequest(0.0);
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

    public BandwidthResourceRequest bandwidthRequest() {
        return this.bandwidthResourceRequest;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("ingress", ingressPoint)
                .add("egress", egressPoint)
                .add("bandwidth", bandwidthResourceRequest.bandwidth().toString())
                .toString();
    }

}
