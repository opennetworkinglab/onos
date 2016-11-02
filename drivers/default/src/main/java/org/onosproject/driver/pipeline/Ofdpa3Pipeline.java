/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.driver.pipeline;

import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flowobjective.ForwardingObjective;

import java.util.Collection;
import java.util.List;

/**
 * Pipeliner for Broadcom OF-DPA 3.0 TTP.
 */
public class Ofdpa3Pipeline extends Ofdpa2Pipeline {
    @Override
    protected void initDriverId() {
        driverId = coreService.registerApplication(
                "org.onosproject.driver.Ofdpa3Pipeline");
    }

    @Override
    protected void initGroupHander(PipelinerContext context) {
        groupHandler = new Ofdpa3GroupHandler();
        groupHandler.init(deviceId, context);
    }

    @Override
    protected List<FlowRule> processVlanIdFilter(PortCriterion portCriterion,
                                                 VlanIdCriterion vidCriterion,
                                                 VlanId assignedVlan,
                                                 ApplicationId applicationId) {
        return processVlanIdFilterInternal(portCriterion, vidCriterion, assignedVlan,
                applicationId, false);
    }

    @Override
    protected Collection<FlowRule> processEthTypeSpecific(ForwardingObjective fwd) {
        if (isNotMplsBos(fwd.selector())) {
            return processEthTypeSpecificInternal(fwd, true, MPLS_TYPE_TABLE);
        }
        return processEthTypeSpecificInternal(fwd, true, MPLS_L3_TYPE_TABLE);
    }
}
