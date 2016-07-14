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

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
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
import org.onosproject.vtn.table.SnatService;
import org.onosproject.vtnrsc.SegmentationId;

/**
 * Provides implementation of SnatService.
 */
public class SnatServiceImpl implements SnatService {

    private static final int SNAT_SAME_SEG_PRIORITY = 0xffff;
    private static final int SNAT_SAME_SEG_CON_PRIORITY = 0xfff0;
    private static final int SNAT_DIFF_SEG_PRIORITY = 0xffe0;
    private static final int PREFIC_LENGTH = 32;

    private final FlowObjectiveService flowObjectiveService;
    private final ApplicationId appId;

    /**
     * Construct a SnatServiceImpl object.
     *
     * @param appId the application id of vtn
     */
    public SnatServiceImpl(ApplicationId appId) {
        this.appId = checkNotNull(appId, "ApplicationId can not be null");
        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        this.flowObjectiveService = serviceDirectory.get(FlowObjectiveService.class);
    }

    @Override
    public void programSnatSameSegmentRules(DeviceId deviceId, SegmentationId matchVni,
                             IpAddress srcIP, IpAddress dstIP, MacAddress ethDst,
                             MacAddress ethSrc, IpAddress ipSrc,
                             SegmentationId actionVni, Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(Long.parseLong(matchVni.segmentationId()))
                .matchIPSrc(IpPrefix.valueOf(srcIP, PREFIC_LENGTH))
                .matchIPDst(IpPrefix.valueOf(dstIP, PREFIC_LENGTH)).build();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setEthDst(ethDst).setEthSrc(ethSrc).setIpSrc(ipSrc)
                .setTunnelId(Long.parseLong(actionVni.segmentationId()));
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment.build())
                .withSelector(selector).fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(SNAT_SAME_SEG_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void programSnatDiffSegmentRules(DeviceId deviceId, SegmentationId matchVni,
                             IpAddress srcIP, MacAddress ethDst,
                             MacAddress ethSrc, IpAddress ipSrc,
                             SegmentationId actionVni, Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(Long.parseLong(matchVni.segmentationId()))
                .matchIPSrc(IpPrefix.valueOf(srcIP, PREFIC_LENGTH)).build();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setEthDst(ethDst).setEthSrc(ethSrc).setIpSrc(ipSrc)
                .setTunnelId(Long.parseLong(actionVni.segmentationId()));
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment.build())
                .withSelector(selector).fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(SNAT_DIFF_SEG_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void programSnatSameSegmentUploadControllerRules(DeviceId deviceId,
                                                            SegmentationId matchVni,
                                                            IpAddress srcIP,
                                                            IpAddress dstIP,
                                                            IpPrefix prefix,
                                                            Operation type) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(Long.parseLong(matchVni.segmentationId()))
                .matchIPSrc(IpPrefix.valueOf(srcIP, PREFIC_LENGTH))
                .matchIPDst(IpPrefix.valueOf(dstIP, prefix.prefixLength()))
                .build();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.add(Instructions.createOutput(PortNumber.CONTROLLER));
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment.build())
                .withSelector(selector).fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(SNAT_SAME_SEG_CON_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void removeSnatRules(DeviceId deviceId, TrafficSelector selector,
                                TrafficTreatment treatment, int priority,
                                Objective.Operation type) {
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC).withPriority(priority);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }
}
