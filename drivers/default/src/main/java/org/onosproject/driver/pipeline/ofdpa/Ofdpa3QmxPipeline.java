/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.driver.pipeline.ofdpa;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import org.onlab.packet.Ethernet;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;

/**
 * Pipeliner for Broadcom OF-DPA 3.0 TTP, specifically for Qumran based switches.
 */
public class Ofdpa3QmxPipeline extends Ofdpa3Pipeline {
    private final Logger log = getLogger(getClass());

    @Override
    protected void initDriverId() {
        driverId = coreService.registerApplication(
                "org.onosproject.driver.Ofdpa3QmxPipeline");
    }

    @Override
    protected boolean matchInPortTmacTable() {
        return false;
    }

    @Override
    protected Collection<FlowRule> processEthTypeSpecific(ForwardingObjective fwd) {
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        //XXX remove when support is added to Qumran based OF-DPA
        if (ethType.ethType().toShort() == Ethernet.TYPE_IPV4 ||
                ethType.ethType().toShort() == Ethernet.TYPE_IPV6) {
            log.warn("Routing table is currently unsupported in dev:{}", deviceId);
            return ImmutableSet.of();
        }

        return super.processEthTypeSpecific(fwd);
    }

    @Override
    protected Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
         EthTypeCriterion ethType =
                 (EthTypeCriterion) fwd.selector().getCriterion(Criterion.Type.ETH_TYPE);
         if (ethType == null) {
             log.error("Versatile forwarding objective:{} must include ethType",
                       fwd.id());
             fail(fwd, ObjectiveError.BADPARAMS);
             return ImmutableSet.of();
         }
         //XXX remove when support is added to Qumran based OF-DPA
         if (ethType.ethType().toShort() == Ethernet.TYPE_IPV6) {
             log.warn("ACL table for IPv6 is currently unsupported in dev:{}", deviceId);
             return ImmutableSet.of();
         }

         if (ethType.ethType().toShort() == Ethernet.TYPE_IPV4) {
             for (Criterion c : fwd.selector().criteria()) {
                 if (c instanceof IPCriterion) {
                     if (((IPCriterion) c).type() == Criterion.Type.IPV4_DST) {
                         log.warn("ACL table for Dst IPv4 is currently "
                                 + "unsupported in dev:{}", deviceId);
                         return ImmutableSet.of();
                     }
                 }
             }
         }

         return super.processVersatile(fwd);
    }
}
