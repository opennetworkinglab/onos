/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.sfc.installer.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SI;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SPI;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.apache.felix.scr.annotations.Component;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.NshServicePathId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.Objective.Operation;
import org.onosproject.net.host.HostService;
import org.onosproject.sfc.installer.FlowClassifierInstallerService;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierService;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.onosproject.vtnrsc.service.VtnRscService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;

import org.slf4j.Logger;

/**
 * Provides flow classifier installer implementation.
 */
@Component(immediate = true)
@Service
public class FlowClassifierInstallerImpl implements FlowClassifierInstallerService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualPortService virtualPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VtnRscService vtnRscService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PortPairService portPairService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PortPairGroupService portPairGroupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowClassifierService flowClassifierService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    protected FlowObjectiveService flowObjectiveService;
    protected ApplicationId appId;

    private static final String DRIVER_NAME = "onosfw";
    private static final String FLOW_CLASSIFIER_NOT_NULL = "Flow-Classifier cannot be null";
    private static final String FLOW_CLASSIFIER_ID_NOT_NULL = "Flow-Classifier-Id cannot be null";
    private static final String PORT_CHAIN_NOT_NULL = "Port-Chain cannot be null";
    private static final int NULL = 0;
    private static final int L3FWD_PRIORITY = 0xffff;
    private static final int NSH_SI_ID = 0xff;

    /**
     * Default constructor.
     */
    public FlowClassifierInstallerImpl() {
    }

    /**
     * Explicit constructor.
     *
     * @param appId Application ID.
     */
    public FlowClassifierInstallerImpl(ApplicationId appId) {
        this.appId = checkNotNull(appId, "ApplicationId can not be null");
        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        this.flowObjectiveService = serviceDirectory.get(FlowObjectiveService.class);
    }

    @Override
    public void installFlowClassifier(PortChain portChain, NshServicePathId nshSpiId) {
        checkNotNull(portChain, PORT_CHAIN_NOT_NULL);
        processFlowClassifier(portChain, nshSpiId, Objective.Operation.ADD);
    }

    @Override
    public void unInstallFlowClassifier(PortChain portChain, NshServicePathId nshSpiId) {
        checkNotNull(portChain, PORT_CHAIN_NOT_NULL);
        processFlowClassifier(portChain, nshSpiId, Objective.Operation.REMOVE);
    }

    public void processFlowClassifier(PortChain portChain, NshServicePathId nshSpiId, Objective.Operation type) {

        // get the portPairGroup
        List<PortPairGroupId> llPortPairGroupIdList = portChain.portPairGroups();
        ListIterator<PortPairGroupId> portPairGroupIdListIterator = llPortPairGroupIdList.listIterator();
        PortPairGroupId portPairGroupId = portPairGroupIdListIterator.next();
        PortPairGroup portPairGroup = portPairGroupService.getPortPairGroup(portPairGroupId);
        List<PortPairId> llPortPairIdList = portPairGroup.portPairs();

        // get port pair
        ListIterator<PortPairId> portPairListIterator = llPortPairIdList.listIterator();
        PortPairId portPairId = portPairListIterator.next();
        PortPair portPair = portPairService.getPortPair(portPairId);

        FlowClassifierInstallerService flowclassifierinstallerService;
        // get flow classifiers
        List<FlowClassifierId> llFlowClassifierList = portChain.flowClassifiers();
        ListIterator<FlowClassifierId> flowClassifierListIterator = llFlowClassifierList.listIterator();

        while (flowClassifierListIterator.hasNext()) {
            FlowClassifierId flowclassifierId = flowClassifierListIterator.next();
            FlowClassifier flowClassifier = flowClassifierService.getFlowClassifier(flowclassifierId);
            prepareFlowClassification(flowClassifier, portPair, nshSpiId, type);
        }
    }

    @Override
    public void prepareFlowClassification(FlowClassifier flowClassifier, PortPair portPair, NshServicePathId nshSPI,
                                          Operation type) {
        DeviceId deviceId = null;
        // device id if virtual ports are set in flow classifier.
        DeviceId deviceIdfromFc = null;
        // device id if port pair is used to fetch device id.
        DeviceId deviceIdfromPp = null;
        MacAddress srcMacAddress = null;
        // Vxlan tunnel port for NSH header(Vxlan + NSH).
        TpPort nshDstPort = TpPort.tpPort(6633);

        if ((flowClassifier.srcPort() != null) && (!flowClassifier.srcPort().portId().isEmpty())) {
            deviceIdfromFc = vtnRscService.getSFToSFFMaping(flowClassifier.srcPort());
            deviceId = deviceIdfromFc;
        } else {
            deviceIdfromPp = vtnRscService.getSFToSFFMaping(VirtualPortId.portId(portPair.ingress()));
            srcMacAddress = virtualPortService.getPort(VirtualPortId.portId(portPair.egress())).macAddress();
            deviceId = deviceIdfromPp;
        }

        // Build Traffic selector.
        TrafficSelector.Builder selector = packTrafficSelector(flowClassifier);

        // Build traffic treatment.
        TrafficTreatment.Builder treatment = packTrafficTreatment(deviceId, srcMacAddress, nshDstPort, deviceIdfromFc,
                                                                  deviceIdfromPp, nshSPI, flowClassifier);

        // Build forwarding objective and send to OVS.
        sendServiceFunctionForwarder(selector, treatment, deviceId, type);
    }

    /**
     * Pack Traffic selector.
     *
     * @param flowClassifier flow-classifier
     * @return traffic selector
     */
    public TrafficSelector.Builder packTrafficSelector(FlowClassifier flowClassifier) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        if ((flowClassifier.srcIpPrefix() != null) && (flowClassifier.srcIpPrefix().prefixLength() != 0)) {
            selector.matchIPSrc(flowClassifier.srcIpPrefix());
        }

        if ((flowClassifier.dstIpPrefix() != null) && (flowClassifier.dstIpPrefix().prefixLength() != 0)) {
            selector.matchIPDst(flowClassifier.dstIpPrefix());
        }

        if ((flowClassifier.protocol() != null) && (!flowClassifier.protocol().isEmpty())) {
            selector.add(Criteria.matchIPProtocol(Short.parseShort(flowClassifier.protocol())));
        }

        if (((flowClassifier.etherType() != null) && (!flowClassifier.etherType().isEmpty()))
                && (flowClassifier.etherType().equals("IPv4") || flowClassifier.etherType().equals("IPv6"))) {
            if (flowClassifier.etherType().equals("IPv4")) {
                selector.matchEthType(Ethernet.TYPE_IPV4);
            } else {
                selector.matchEthType(Ethernet.TYPE_IPV6);
            }
        }

        List<TpPort> srcPortRange = new LinkedList<>();
        List<TpPort> dstPortRange = new LinkedList<>();
        if ((flowClassifier.minSrcPortRange() != 0) && flowClassifier.maxSrcPortRange() != 0
                && flowClassifier.minDstPortRange() != 0 && flowClassifier.maxDstPortRange() != 0) {

            for (int port = flowClassifier.minSrcPortRange(); port <= flowClassifier.maxSrcPortRange(); port++) {
                srcPortRange.add(TpPort.tpPort(port));
            }
            for (int port = flowClassifier.minDstPortRange(); port <= flowClassifier.maxDstPortRange(); port++) {
                dstPortRange.add(TpPort.tpPort(port));
            }
        }

        for (TpPort inPort : srcPortRange) {
            selector.matchUdpSrc(inPort);
        }
        for (TpPort outPort : dstPortRange) {
            selector.matchUdpDst(outPort);
        }
        return selector;
    }

    /**
     * Pack traffic treatment.
     *
     * @param deviceId device id
     * @param srcMacAddress source mac-address
     * @param nshDstPort vxlan tunnel port for nsh header
     * @param deviceIdfromFc device id if virtual ports are set in flow classifier.
     * @param deviceIdfromPp device id if port pair is used to fetch device id.
     * @param nshSPI nsh spi
     * @param flowClassifier flow-classifier
     * @return traffic treatment
     */
    public TrafficTreatment.Builder packTrafficTreatment(DeviceId deviceId, MacAddress srcMacAddress,
                                               TpPort nshDstPort, DeviceId deviceIdfromFc, DeviceId deviceIdfromPp,
                                               NshServicePathId nshSPI, FlowClassifier flowClassifier) {
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        Host host = hostService.getHost(HostId.hostId(srcMacAddress));
        PortNumber port = host.location().port();
        if (deviceIdfromPp != null) {
            treatmentBuilder.setOutput(port);
        } else if (deviceIdfromFc != null) {
            treatmentBuilder.setVlanId((VlanId.vlanId(Short.parseShort((vtnRscService.getL3vni(flowClassifier
                    .tenantId()).toString())))));
        }

        // Set NSH
        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionTreatmentResolver resolver = handler.behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment nspIdTreatment = resolver.getExtensionInstruction(NICIRA_SET_NSH_SPI.type());
        ExtensionTreatment nsiIdTreatment = resolver.getExtensionInstruction(NICIRA_SET_NSH_SI.type());

        treatmentBuilder.extension(nspIdTreatment, deviceId);
        treatmentBuilder.extension(nsiIdTreatment, deviceId);
        treatmentBuilder.setUdpDst(nshDstPort);

        try {
            nspIdTreatment.setPropertyValue("nshSpi", nshSPI);
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set Nsh Spi Id {}", deviceId);
        }
        try {
            nsiIdTreatment.setPropertyValue("nshSi", NSH_SI_ID);
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set Nsh Si Id {}", deviceId);
        }
        return treatmentBuilder;
    }

    /**
     * Send service-function-forwarder to OVS.
     *
     * @param selector traffic selector
     * @param treatment traffic treatment
     * @param deviceId device id
     * @param type operation type
     */
    public void sendServiceFunctionForwarder(TrafficSelector.Builder selector, TrafficTreatment.Builder treatment,
            DeviceId deviceId, Objective.Operation type) {
        ForwardingObjective.Builder objective = DefaultForwardingObjective.builder().withTreatment(treatment.build())
                .withSelector(selector.build()).fromApp(appId).makePermanent().withFlag(Flag.VERSATILE)
                .withPriority(L3FWD_PRIORITY);

        if (type.equals(Objective.Operation.ADD)) {
            log.debug("flowClassifierRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("flowClassifierRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }
}
