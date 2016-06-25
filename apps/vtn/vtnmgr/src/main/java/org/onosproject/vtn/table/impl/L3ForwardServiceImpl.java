/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtn.table.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.Objective.Operation;
import org.onosproject.vtn.table.L3ForwardService;
import org.onosproject.vtnrsc.SegmentationId;
import org.slf4j.Logger;

/**
 * Provides implementation of L3ForwardService.
 */
public class L3ForwardServiceImpl implements L3ForwardService {
    private final Logger log = getLogger(getClass());

    private static final int L3FWD_PRIORITY = 0xffff;
    private static final short IP_TYPE = Ethernet.TYPE_IPV4;
    private static final int PREFIX_LENGTH = 32;

    private final FlowObjectiveService flowObjectiveService;
    private final ApplicationId appId;

    /**
     * Construct a L3ForwardServiceImpl object.
     *
     * @param appId the application id of vtn
     */
    public L3ForwardServiceImpl(ApplicationId appId) {
        this.appId = checkNotNull(appId, "ApplicationId can not be null");
        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        this.flowObjectiveService = serviceDirectory.get(FlowObjectiveService.class);
    }

    @Override
    public void programRouteRules(DeviceId deviceId, SegmentationId l3Vni,
                                  IpAddress dstVmIP, SegmentationId dstVni,
                                  MacAddress dstVmGwMac, MacAddress dstVmMac,
                                  Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(IP_TYPE)
                .matchTunnelId(Long.parseLong(l3Vni.segmentationId()))
                .matchIPDst(IpPrefix.valueOf(dstVmIP, PREFIX_LENGTH)).build();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setEthSrc(dstVmGwMac)
                .setEthDst(dstVmMac)
                .add(Instructions.modTunnelId(Long.parseLong(dstVni
                             .segmentationId())));
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment.build())
                .withSelector(selector).fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(L3FWD_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("RouteRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("RouteRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

}
