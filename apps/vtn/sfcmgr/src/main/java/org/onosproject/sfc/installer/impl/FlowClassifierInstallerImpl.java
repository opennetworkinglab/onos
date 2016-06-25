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
package org.onosproject.sfc.installer.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SI;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SPI;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.NshServiceIndex;
import org.onosproject.net.NshServicePathId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
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
import org.onosproject.net.host.HostService;
import org.onosproject.sfc.installer.FlowClassifierInstallerService;
import org.onosproject.vtnrsc.FiveTuple;
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
public class FlowClassifierInstallerImpl implements FlowClassifierInstallerService {
    private final Logger log = getLogger(getClass());

    protected VirtualPortService virtualPortService;
    protected VtnRscService vtnRscService;
    protected PortPairService portPairService;
    protected PortPairGroupService portPairGroupService;
    protected FlowClassifierService flowClassifierService;
    protected DriverService driverService;
    protected DeviceService deviceService;
    protected HostService hostService;
    protected FlowObjectiveService flowObjectiveService;
    protected ApplicationId appId;

    private static final String DRIVER_NAME = "onosfw";
    private static final String FLOW_CLASSIFIER_NOT_NULL = "Flow-Classifier cannot be null";
    private static final String FLOW_CLASSIFIER_ID_NOT_NULL = "Flow-Classifier-Id cannot be null";
    private static final String PORT_CHAIN_NOT_NULL = "Port-Chain cannot be null";
    private static final int NULL = 0;
    private static final int FLOW_CLASSIFIER_PRIORITY = 0x7fff;
    private static final short NSH_SI_ID = 0xff;

    /**
     * Default constructor.
     */
    public FlowClassifierInstallerImpl() {
    }

    /**
     * Explicit constructor.
     *
     * @param appId application id.
     */
    public FlowClassifierInstallerImpl(ApplicationId appId) {
        this.appId = checkNotNull(appId, "ApplicationId can not be null");
        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        this.flowObjectiveService = serviceDirectory.get(FlowObjectiveService.class);
        this.driverService = serviceDirectory.get(DriverService.class);
        this.deviceService = serviceDirectory.get(DeviceService.class);
        this.hostService = serviceDirectory.get(HostService.class);
        this.virtualPortService = serviceDirectory.get(VirtualPortService.class);
        this.vtnRscService = serviceDirectory.get(VtnRscService.class);
        this.portPairService = serviceDirectory.get(PortPairService.class);
        this.portPairGroupService = serviceDirectory.get(PortPairGroupService.class);
        this.flowClassifierService = serviceDirectory.get(FlowClassifierService.class);
    }

    @Override
    public ConnectPoint installFlowClassifier(PortChain portChain, NshServicePathId nshSpiId) {
        checkNotNull(portChain, PORT_CHAIN_NOT_NULL);
        // Get the portPairGroup
        List<PortPairGroupId> llPortPairGroupIdList = portChain.portPairGroups();
        ListIterator<PortPairGroupId> portPairGroupIdListIterator = llPortPairGroupIdList.listIterator();
        PortPairGroupId portPairGroupId = portPairGroupIdListIterator.next();
        PortPairGroup portPairGroup = portPairGroupService.getPortPairGroup(portPairGroupId);
        List<PortPairId> llPortPairIdList = portPairGroup.portPairs();

        // Get port pair
        ListIterator<PortPairId> portPairListIterator = llPortPairIdList.listIterator();
        PortPairId portPairId = portPairListIterator.next();
        PortPair portPair = portPairService.getPortPair(portPairId);

        return processFlowClassifier(portChain, portPair, nshSpiId, null, Objective.Operation.ADD);
    }

    @Override
    public ConnectPoint unInstallFlowClassifier(PortChain portChain, NshServicePathId nshSpiId) {
        checkNotNull(portChain, PORT_CHAIN_NOT_NULL);
        // Get the portPairGroup
        List<PortPairGroupId> llPortPairGroupIdList = portChain.portPairGroups();
        ListIterator<PortPairGroupId> portPairGroupIdListIterator = llPortPairGroupIdList.listIterator();
        PortPairGroupId portPairGroupId = portPairGroupIdListIterator.next();
        PortPairGroup portPairGroup = portPairGroupService.getPortPairGroup(portPairGroupId);
        List<PortPairId> llPortPairIdList = portPairGroup.portPairs();

        // Get port pair
        ListIterator<PortPairId> portPairListIterator = llPortPairIdList.listIterator();
        PortPairId portPairId = portPairListIterator.next();
        PortPair portPair = portPairService.getPortPair(portPairId);

        return processFlowClassifier(portChain, portPair, nshSpiId, null, Objective.Operation.REMOVE);
    }

    @Override
    public ConnectPoint installLoadBalancedFlowClassifier(PortChain portChain, FiveTuple fiveTuple,
                                                          NshServicePathId nshSpiId) {
        checkNotNull(portChain, PORT_CHAIN_NOT_NULL);

        // Get the load balanced path
        List<PortPairId> portPairs = portChain.getLoadBalancePath(fiveTuple);

        // Get the first port pair
        ListIterator<PortPairId> portPairListIterator = portPairs.listIterator();
        PortPairId portPairId = portPairListIterator.next();
        PortPair portPair = portPairService.getPortPair(portPairId);

        return processFlowClassifier(portChain, portPair, nshSpiId, fiveTuple, Objective.Operation.ADD);
    }

    @Override
    public ConnectPoint unInstallLoadBalancedFlowClassifier(PortChain portChain, FiveTuple fiveTuple,
                                                            NshServicePathId nshSpiId) {
        checkNotNull(portChain, PORT_CHAIN_NOT_NULL);
        // Get the load balanced path
        List<PortPairId> portPairs = portChain.getLoadBalancePath(fiveTuple);

        // Get the first port pair
        ListIterator<PortPairId> portPairListIterator = portPairs.listIterator();
        PortPairId portPairId = portPairListIterator.next();
        PortPair portPair = portPairService.getPortPair(portPairId);

        return processFlowClassifier(portChain, portPair, nshSpiId, fiveTuple, Objective.Operation.REMOVE);
    }

    public ConnectPoint processFlowClassifier(PortChain portChain, PortPair portPair, NshServicePathId nshSpiId,
                                              FiveTuple fiveTuple, Objective.Operation type) {

        DeviceId deviceIdfromPortPair = vtnRscService.getSfToSffMaping(VirtualPortId.portId(portPair.ingress()));
        MacAddress srcMacAddress = virtualPortService.getPort(VirtualPortId.portId(portPair.ingress())).macAddress();
        Host host = hostService.getHost(HostId.hostId(srcMacAddress));
        PortNumber port = host.location().port();

        DeviceId deviceId = deviceIdfromPortPair;

        // Vxlan tunnel port for NSH header(Vxlan + NSH).
        TpPort nshDstPort = TpPort.tpPort(6633);

        FlowClassifierInstallerService flowclassifierinstallerService;
        // get flow classifiers
        List<FlowClassifierId> llFlowClassifierList = portChain.flowClassifiers();
        ListIterator<FlowClassifierId> flowClassifierListIterator = llFlowClassifierList.listIterator();

        while (flowClassifierListIterator.hasNext()) {
            FlowClassifierId flowclassifierId = flowClassifierListIterator.next();
            FlowClassifier flowClassifier = flowClassifierService.getFlowClassifier(flowclassifierId);

            if ((flowClassifier.srcPort() != null) && (!flowClassifier.srcPort().portId().isEmpty())) {
                deviceId = vtnRscService.getSfToSffMaping(flowClassifier.srcPort());
            }

            // Build Traffic selector.
            TrafficSelector.Builder selector = packTrafficSelector(flowClassifier, fiveTuple);

            if (fiveTuple == null) {
                // Send the packet to controller
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                treatment.setOutput(PortNumber.CONTROLLER);
                sendServiceFunctionClassifier(selector, treatment, deviceId, type, flowClassifier.priority());
            } else if (deviceId.equals(deviceIdfromPortPair)) {
                // classifier and source device are in the same OVS. So directly send packet to first port pair
                TrafficTreatment.Builder treatment = packTrafficTreatment(deviceId, port, nshDstPort,
                                                                          nshSpiId, flowClassifier, true);
                // Build forwarding objective and send to OVS.
                sendServiceFunctionClassifier(selector, treatment, deviceId, type, flowClassifier.priority());
            } else {
                // classifier and source device are not in the same OVS. Send packet on vlan Tunnel
                TrafficTreatment.Builder treatment = packTrafficTreatment(deviceId, port, nshDstPort,
                                                                          nshSpiId, flowClassifier, false);
                // Build forwarding objective and send to OVS.
                sendServiceFunctionClassifier(selector, treatment, deviceId, type, flowClassifier.priority());

                // At the other device get the packet from vlan and send to first port pair
                TrafficSelector.Builder selectorDst = DefaultTrafficSelector.builder();
                selectorDst.matchVlanId((VlanId.vlanId(Short.parseShort((vtnRscService
                        .getL3vni(flowClassifier.tenantId()).toString())))));
                TrafficTreatment.Builder treatmentDst = DefaultTrafficTreatment.builder();
                Host hostDst = hostService.getHost(HostId.hostId(srcMacAddress));
                treatmentDst.setOutput(hostDst.location().port());
                sendServiceFunctionClassifier(selectorDst, treatmentDst, deviceIdfromPortPair, type,
                                              flowClassifier.priority());
            }
        }
        return host.location();
    }

    /**
     * Pack Traffic selector.
     *
     * @param flowClassifier flow-classifier
     * @param fiveTuple five tuple info for the packet
     * @return traffic selector
     */
    public TrafficSelector.Builder packTrafficSelector(FlowClassifier flowClassifier, FiveTuple fiveTuple) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        if ((flowClassifier.srcIpPrefix() != null) && (flowClassifier.srcIpPrefix().prefixLength() != 0)) {
            selector.matchIPSrc(flowClassifier.srcIpPrefix());
        } else if (fiveTuple != null && fiveTuple.ipSrc() != null) {
            selector.matchIPSrc(IpPrefix.valueOf(fiveTuple.ipSrc(), 24));
        }

        if ((flowClassifier.dstIpPrefix() != null) && (flowClassifier.dstIpPrefix().prefixLength() != 0)) {
            selector.matchIPDst(flowClassifier.dstIpPrefix());
        } else if (fiveTuple != null && fiveTuple.ipDst() != null) {
            selector.matchIPDst(IpPrefix.valueOf(fiveTuple.ipDst(), 24));
        }

        if ((flowClassifier.protocol() != null) && (!flowClassifier.protocol().isEmpty())) {
            if (flowClassifier.protocol().equalsIgnoreCase("TCP")) {
                selector.add(Criteria.matchIPProtocol(IPv4.PROTOCOL_TCP));
            } else if (flowClassifier.protocol().equalsIgnoreCase("UDP")) {
                selector.add(Criteria.matchIPProtocol(IPv4.PROTOCOL_UDP));
            }
        } else if (fiveTuple != null && fiveTuple.protocol() != 0) {
            selector.add(Criteria.matchIPProtocol(fiveTuple.protocol()));
        }

        if (((flowClassifier.etherType() != null) && (!flowClassifier.etherType().isEmpty()))
                && (flowClassifier.etherType().equals("IPv4") || flowClassifier.etherType().equals("IPv6"))) {
            if (flowClassifier.etherType().equals("IPv4")) {
                selector.matchEthType(Ethernet.TYPE_IPV4);
            } else {
                selector.matchEthType(Ethernet.TYPE_IPV6);
            }
        }

        if ((flowClassifier.srcPort() != null) && (!flowClassifier.srcPort().portId().isEmpty())) {
            VirtualPortId vPortId = VirtualPortId.portId(flowClassifier.srcPort().portId());
            MacAddress macAddress = virtualPortService.getPort(vPortId).macAddress();
            Host host = hostService.getHost(HostId.hostId(macAddress));
            selector.matchInPort(host.location().port());
        }

        // Take the port information from five tuple only when the protocol is TCP.
        if (fiveTuple != null && fiveTuple.protocol() == IPv4.PROTOCOL_TCP) {
            selector.matchTcpSrc(TpPort.tpPort((int) fiveTuple.portSrc().toLong()));
            selector.matchTcpDst(TpPort.tpPort((int) fiveTuple.portDst().toLong()));
        } else {
            // For udp packets take the port information from flow classifier
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
        }
        return selector;
    }

    /**
     * Pack traffic treatment.
     *
     * @param deviceId device id
     * @param port port number
     * @param nshDstPort vxlan tunnel port for nsh header
     * @param nshSpi nsh spi
     * @param flowClassifier flow-classifier
     * @param isSameOvs whether the next service function is in same ovs
     * @return traffic treatment
     */
    public TrafficTreatment.Builder packTrafficTreatment(DeviceId deviceId, PortNumber port,
                                                         TpPort nshDstPort, NshServicePathId nshSpi,
                                                         FlowClassifier flowClassifier, boolean isSameOvs) {

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        if (isSameOvs) {
            treatmentBuilder.setOutput(port);
        } else {
            treatmentBuilder.setVlanId((VlanId.vlanId(Short.parseShort((vtnRscService
                    .getL3vni(flowClassifier.tenantId()).toString())))));
            treatmentBuilder.setUdpDst(nshDstPort);
        }

        // Set NSH
        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionTreatmentResolver resolver = handler.behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment nspIdTreatment = resolver.getExtensionInstruction(NICIRA_SET_NSH_SPI.type());
        ExtensionTreatment nsiIdTreatment = resolver.getExtensionInstruction(NICIRA_SET_NSH_SI.type());

        treatmentBuilder.extension(nspIdTreatment, deviceId);
        treatmentBuilder.extension(nsiIdTreatment, deviceId);

        try {
            nspIdTreatment.setPropertyValue("nshSpi", nshSpi);
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set Nsh Spi Id {}", deviceId);
        }
        try {
            nsiIdTreatment.setPropertyValue("nshSi", NshServiceIndex.of(NSH_SI_ID));
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
     * @param priority priority of classifier
     */
    public void sendServiceFunctionClassifier(TrafficSelector.Builder selector, TrafficTreatment.Builder treatment,
                                              DeviceId deviceId, Objective.Operation type, int priority) {
        log.info("Sending flow to service function classifier. Selector {}, Treatment {}",
                 selector.toString(), treatment.toString());
        ForwardingObjective.Builder objective = DefaultForwardingObjective.builder().withTreatment(treatment.build())
                .withSelector(selector.build()).fromApp(appId).makePermanent().withFlag(Flag.VERSATILE)
                .withPriority(priority);

        if (type.equals(Objective.Operation.ADD)) {
            log.debug("flowClassifierRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("flowClassifierRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }
}
