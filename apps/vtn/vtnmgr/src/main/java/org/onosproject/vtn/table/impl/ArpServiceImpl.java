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
import org.onlab.packet.EthType.EtherType;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.Objective.Operation;
import org.onosproject.vtn.table.ArpService;
import org.onosproject.vtnrsc.SegmentationId;
import org.slf4j.Logger;

/**
 * ArpTable class providing the rules in ARP table.
 */
public class ArpServiceImpl implements ArpService {
    private final Logger log = getLogger(getClass());

    private static final int ARP_PRIORITY = 0xffff;
    private static final short ARP_RESPONSE = 0x2;
    private static final EtherType ARP_TYPE = EtherType.ARP;

    private final FlowObjectiveService flowObjectiveService;
    private final ApplicationId appId;

    /**
     * Construct a ArpServiceImpl object.
     *
     * @param appId the application id of vtn
     */
    public ArpServiceImpl(ApplicationId appId) {
        this.appId = checkNotNull(appId, "ApplicationId can not be null");
        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        this.flowObjectiveService = serviceDirectory.get(FlowObjectiveService.class);
    }

    @Override
    public void programArpRules(DriverHandler hander, DeviceId deviceId,
                                IpAddress dstIP, SegmentationId srcVni,
                                     MacAddress dstMac, Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(ARP_TYPE.ethType().toShort())
                .matchArpTpa(Ip4Address.valueOf(dstIP.toString()))
                .matchTunnelId(Long.parseLong(srcVni.segmentationId())).build();

        ExtensionTreatmentResolver resolver = hander
                .behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment ethSrcToDst = resolver
                .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes
                                         .NICIRA_MOV_ETH_SRC_TO_DST.type());
        ExtensionTreatment arpShaToTha = resolver
                .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes
                                         .NICIRA_MOV_ARP_SHA_TO_THA.type());
        ExtensionTreatment arpSpaToTpa = resolver
                .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes
                                         .NICIRA_MOV_ARP_SPA_TO_TPA.type());
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .extension(ethSrcToDst, deviceId)
                .setEthSrc(dstMac).setArpOp(ARP_RESPONSE)
                .extension(arpShaToTha, deviceId)
                .extension(arpSpaToTpa, deviceId)
                .setArpSha(dstMac).setArpSpa(dstIP)
                .setOutput(PortNumber.IN_PORT).build();

        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(ARP_PRIORITY);

        if (type.equals(Objective.Operation.ADD)) {
            log.debug("PrivateArpRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("PrivateArpRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }
}
