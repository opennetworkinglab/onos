/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour;


import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.pipelines.basic.PortStatisticsDiscoveryImpl;
import org.onosproject.pipelines.fabric.FabricConstants;

/**
 * Implementation of the PortStatisticsBehaviour for fabric.p4.
 */
public class FabricPortStatisticsDiscovery extends PortStatisticsDiscoveryImpl {

    /**
     * Returns the ID of the ingress port counter.
     *
     * @return counter ID
     */
    @Override
    public PiCounterId ingressCounterId() {
        return FabricConstants.FABRIC_INGRESS_PORT_COUNTERS_CONTROL_INGRESS_PORT_COUNTER;
    }

    /**
     * Returns the ID of the egress port counter.
     *
     * @return counter ID
     */
    @Override
    public PiCounterId egressCounterId() {
        return FabricConstants.FABRIC_INGRESS_PORT_COUNTERS_CONTROL_EGRESS_PORT_COUNTER;
    }
}
