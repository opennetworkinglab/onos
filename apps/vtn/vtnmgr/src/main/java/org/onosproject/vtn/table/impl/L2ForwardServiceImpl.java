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
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.slf4j.LoggerFactory.getLogger;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment.Builder;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.vtn.table.L2ForwardService;
import org.onosproject.vtnrsc.SegmentationId;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Provides implementation of L2ForwardService.
 */
public final class L2ForwardServiceImpl implements L2ForwardService {
    private final Logger log = getLogger(getClass());

    private static final int MAC_PRIORITY = 0xffff;
    public static final Integer GROUP_ID = 1;
    private final FlowObjectiveService flowObjectiveService;
    private final ApplicationId appId;
    private final DriverService driverService;
    /**
     * Constructor.
     *
     * @param appId the application id of vtn
     */
    public L2ForwardServiceImpl(ApplicationId appId) {
        this.appId = checkNotNull(appId, "ApplicationId can not be null");
        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        this.flowObjectiveService = serviceDirectory.get(FlowObjectiveService.class);
        this.driverService = serviceDirectory.get(DriverService.class);
    }

    @Override
    public void programLocalBcastRules(DeviceId deviceId,
                                       SegmentationId segmentationId,
                                       PortNumber inPort,
                                       Iterable<PortNumber> localVmPorts,
                                       Iterable<PortNumber> localTunnelPorts,
                                       Objective.Operation type) {
        if (localVmPorts == null || localTunnelPorts == null) {
            log.info("No other host port and tunnel in the device");
            return;
        }
        Sets.newHashSet(localVmPorts).forEach(lp -> {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchInPort(lp).matchEthDst(MacAddress.BROADCAST)
                    .add(Criteria.matchTunnelId(Long
                            .parseLong(segmentationId.toString())))
                    .build();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                    .builder();
            boolean flag = false;
            for (PortNumber outPort : localVmPorts) {
                flag = true;
                if (outPort != lp) {
                    treatment.setOutput(outPort);
                }
            }
            if (type == Objective.Operation.REMOVE && inPort.equals(lp)) {
                flag = false;
            }
            treatment.group(new DefaultGroupId(GROUP_ID));
            ForwardingObjective.Builder objective = DefaultForwardingObjective
                    .builder().withTreatment(treatment.build())
                    .withSelector(selector).fromApp(appId).makePermanent()
                    .withFlag(Flag.SPECIFIC).withPriority(MAC_PRIORITY);
            if (flag) {
                flowObjectiveService.forward(deviceId, objective.add());
            } else {
                flowObjectiveService.forward(deviceId, objective.remove());
            }
        });
    }

    @Override
    public void programTunnelBcastRules(DeviceId deviceId,
                                        SegmentationId segmentationId,
                                        Iterable<PortNumber> localVmPorts,
                                        Iterable<PortNumber> localTunnelPorts,
                                        Objective.Operation type) {
        if (localVmPorts == null || localTunnelPorts == null) {
            log.info("No other host port or tunnel ports in the device");
            return;
        }
        Sets.newHashSet(localTunnelPorts).forEach(tp -> {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchInPort(tp)
                    .add(Criteria.matchTunnelId(Long
                            .parseLong(segmentationId.toString())))
                    .matchEthDst(MacAddress.BROADCAST).build();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                    .builder();

            for (PortNumber outPort : localVmPorts) {
                treatment.setOutput(outPort);
            }

            ForwardingObjective.Builder objective = DefaultForwardingObjective
                    .builder().withTreatment(treatment.build())
                    .withSelector(selector).fromApp(appId).makePermanent()
                    .withFlag(Flag.SPECIFIC).withPriority(MAC_PRIORITY);
            if (type.equals(Objective.Operation.ADD)) {
                if (Sets.newHashSet(localVmPorts).size() == 0) {
                    flowObjectiveService.forward(deviceId, objective.remove());
                } else {
                    flowObjectiveService.forward(deviceId, objective.add());
                }
            } else {
                flowObjectiveService.forward(deviceId, objective.remove());
            }
        });
    }

    @Override
    public void programLocalOut(DeviceId deviceId,
                                SegmentationId segmentationId,
                                PortNumber outPort, MacAddress sourceMac,
                                Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchTunnelId(Long.parseLong(segmentationId.toString()))
                .matchEthDst(sourceMac).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(outPort).build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(MAC_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }

    }

    @Override
    public void programExternalOut(DeviceId deviceId,
                                SegmentationId segmentationId,
                                PortNumber outPort, MacAddress sourceMac,
                                Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchTunnelId(Long.parseLong(segmentationId.toString()))
                .matchEthSrc(sourceMac).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(outPort).build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(MAC_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }

    }

    @Override
    public void programTunnelOut(DeviceId deviceId,
                                 SegmentationId segmentationId,
                                 PortNumber tunnelOutPort, MacAddress dstMac,
                                 Objective.Operation type, IpAddress ipAddress) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(dstMac).add(Criteria.matchTunnelId(Long
                        .parseLong(segmentationId.toString())))
                .build();

        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionTreatmentResolver resolver =  handler.behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment = resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
        try {
            treatment.setPropertyValue("tunnelDst", Ip4Address.valueOf(ipAddress.toString()));
        } catch (Exception e) {
           log.error("Failed to get extension instruction to set tunnel dst {}", deviceId);
        }

        Builder builder = DefaultTrafficTreatment.builder();
        builder.extension(treatment, deviceId)
                .setOutput(tunnelOutPort).build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(builder.build()).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(MAC_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }

    }
}
