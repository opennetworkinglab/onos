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
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.Objective.Operation;
import org.onosproject.vtn.table.ClassifierService;
import org.onosproject.vtnrsc.SegmentationId;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Provides implementation of ClassifierService.
 */
public class ClassifierServiceImpl implements ClassifierService {
    private final Logger log = getLogger(getClass());

    private static final EtherType ETH_TYPE = EtherType.ARP;
    private static final int ARP_CLASSIFIER_PRIORITY = 60000;
    private static final int L3_CLASSIFIER_PRIORITY = 0xffff;
    private static final int L2_CLASSIFIER_PRIORITY = 50000;
    private static final int USERDATA_CLASSIFIER_PRIORITY = 65535;
    private final FlowObjectiveService flowObjectiveService;
    private final ApplicationId appId;

    /**
     * Constructor.
     *
     * @param appId the application id of vtn
     */
    public ClassifierServiceImpl(ApplicationId appId) {
        this.appId = checkNotNull(appId, "ApplicationId can not be null");
        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        this.flowObjectiveService = serviceDirectory.get(FlowObjectiveService.class);
    }

    @Override
    public void programLocalIn(DeviceId deviceId,
                               SegmentationId segmentationId, PortNumber inPort,
                               MacAddress srcMac, ApplicationId appid,
                               Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort).matchEthSrc(srcMac).build();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.add(Instructions
                .modTunnelId(Long.parseLong(segmentationId.toString())));
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment.build())
                .withSelector(selector).fromApp(appId).makePermanent()
                .withFlag(Flag.SPECIFIC).withPriority(L2_CLASSIFIER_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("programLocalIn-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("programLocalIn-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void programTunnelIn(DeviceId deviceId,
                                SegmentationId segmentationId,
                                Iterable<PortNumber> localTunnelPorts,
                                Objective.Operation type) {
        if (localTunnelPorts == null) {
            log.info("No tunnel port in device");
            return;
        }
        Sets.newHashSet(localTunnelPorts).forEach(tp -> {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchInPort(tp).add(Criteria.matchTunnelId(Long
                            .parseLong(segmentationId.toString())))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .build();
            ForwardingObjective.Builder objective = DefaultForwardingObjective
                    .builder().withTreatment(treatment).withSelector(selector)
                    .fromApp(appId).makePermanent().withFlag(Flag.SPECIFIC)
                    .withPriority(L2_CLASSIFIER_PRIORITY);
            if (type.equals(Objective.Operation.ADD)) {
                log.debug("programTunnelIn-->ADD");
                flowObjectiveService.forward(deviceId, objective.add());
            } else {
                log.debug("programTunnelIn-->REMOVE");
                flowObjectiveService.forward(deviceId, objective.remove());
            }
        });
    }

    @Override
    public void programL3ExPortClassifierRules(DeviceId deviceId, PortNumber inPort,
                                               IpAddress dstIp,
                                               Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4).matchInPort(inPort)
                .matchIPDst(IpPrefix.valueOf(dstIp, 32)).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(L3_CLASSIFIER_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("L3ExToInClassifierRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("L3ExToInClassifierRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void programL3InPortClassifierRules(DeviceId deviceId, PortNumber inPort,
                                               MacAddress srcMac, MacAddress dstMac,
                                               SegmentationId actionVni,
                                               Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort).matchEthSrc(srcMac).matchEthDst(dstMac)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(Long.parseLong(actionVni.segmentationId())).build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(L3_CLASSIFIER_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("L3InternalClassifierRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("L3InternalClassifierRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void programArpClassifierRules(DeviceId deviceId, IpAddress dstIp,
                                          SegmentationId actionVni,
                                          Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(ETH_TYPE.ethType().toShort())
                .matchArpTpa(Ip4Address.valueOf(dstIp.toString()))
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(Long.parseLong(actionVni.segmentationId()))
                .build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(ARP_CLASSIFIER_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("ArpClassifierRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("ArpClassifierRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void programArpClassifierRules(DeviceId deviceId, PortNumber inPort,
                                          IpAddress dstIp,
                                          SegmentationId actionVni,
                                          Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort).matchEthType(ETH_TYPE.ethType().toShort())
                .matchArpTpa(Ip4Address.valueOf(dstIp.toString())).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(Long.parseLong(actionVni.segmentationId()))
                .build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(ARP_CLASSIFIER_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("ArpClassifierRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("ArpClassifierRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void programUserdataClassifierRules(DeviceId deviceId,
                                               IpPrefix ipPrefix,
                                               IpAddress dstIp,
                                               MacAddress dstmac,
                                               SegmentationId actionVni,
                                               Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4).matchIPSrc(ipPrefix)
                .matchIPDst(IpPrefix.valueOf(dstIp, 32)).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(Long.parseLong(actionVni.segmentationId()))
                .setEthDst(dstmac).build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(USERDATA_CLASSIFIER_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("UserdataClassifierRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("UserdataClassifierRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void programExportPortArpClassifierRules(Port exportPort,
                                                    DeviceId deviceId,
                                                    Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EtherType.ARP.ethType().toShort())
                .matchInPort(exportPort.number()).build();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.add(Instructions.createOutput(PortNumber.CONTROLLER));
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment.build())
                .withSelector(selector).fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(L3_CLASSIFIER_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }
}
