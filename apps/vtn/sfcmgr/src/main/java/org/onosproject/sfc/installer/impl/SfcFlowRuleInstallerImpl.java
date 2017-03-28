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
package org.onosproject.sfc.installer.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.flow.criteria.ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_ENCAP_ETH_TYPE;
import static org.onosproject.net.flow.criteria.ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SI;
import static org.onosproject.net.flow.criteria.ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SPI;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_DST;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_SRC;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NSH_MDTYPE;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NSH_NP;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_POP_NSH;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_PUSH_NSH;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT_TABLE;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH1;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH2;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH3;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH4;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SI;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SPI;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_TUN_GPE_NP;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.NshContextHeader;
import org.onosproject.net.NshServiceIndex;
import org.onosproject.net.NshServicePathId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.host.HostService;
import org.onosproject.sfc.installer.SfcFlowRuleInstallerService;
import org.onosproject.vtnrsc.FiveTuple;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.SegmentationId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierService;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.onosproject.vtnrsc.service.VtnRscService;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Provides flow classifier installer implementation.
 */
public class SfcFlowRuleInstallerImpl implements SfcFlowRuleInstallerService {
    private final Logger log = getLogger(getClass());

    protected VirtualPortService virtualPortService;
    protected VtnRscService vtnRscService;
    protected PortPairService portPairService;
    protected PortPairGroupService portPairGroupService;
    protected FlowClassifierService flowClassifierService;
    protected DriverService driverService;
    protected DeviceService deviceService;
    protected HostService hostService;
    protected TenantNetworkService tenantNetworkService;
    protected FlowObjectiveService flowObjectiveService;
    protected ApplicationId appId;

    private static final String PORT_CHAIN_NOT_NULL = "Port-Chain cannot be null";
    private static final int FLOW_CLASSIFIER_PRIORITY = 0xC738;
    private static final int DEFAULT_FORWARDER_PRIORITY = 0xD6D8;
    private static final int ENCAP_OUTPUT_PRIORITY = 0x64;
    private static final int TUNNEL_SEND_PRIORITY = 0xC8;
    private static final String SWITCH_CHANNEL_ID = "channelId";
    private static final int ENCAP_OUTPUT_TABLE = 4;
    private static final int TUNNEL_SEND_TABLE = 7;
    private static final short ENCAP_ETH_TYPE = (short) 0x894f;
    private static final String DEFAULT_IP = "0.0.0.0";
    private static final String VXLANPORT_HEAD = "vxlan-0.0.0.0";

    /* Port chain params */
    private short nshSi;
    List<DeviceId> classifierList;
    List<DeviceId> forwarderList;

    /**
     * Default constructor.
     */
    public SfcFlowRuleInstallerImpl() {
    }

    /**
     * Explicit constructor.
     *
     * @param appId application id.
     */
    public SfcFlowRuleInstallerImpl(ApplicationId appId) {
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
        this.tenantNetworkService = serviceDirectory.get(TenantNetworkService.class);
        nshSi = 0xff;
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

        return installSfcClassifierRules(portChain, portPair, nshSpiId, null, Objective.Operation.ADD);
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

        return installSfcClassifierRules(portChain, portPair, nshSpiId, null, Objective.Operation.REMOVE);
    }

    @Override
    public ConnectPoint installLoadBalancedFlowRules(PortChain portChain, FiveTuple fiveTuple,
            NshServicePathId nshSpiId) {
        checkNotNull(portChain, PORT_CHAIN_NOT_NULL);

        return installSfcFlowRules(portChain, fiveTuple, nshSpiId, Objective.Operation.ADD);
    }

    @Override
    public ConnectPoint unInstallLoadBalancedFlowRules(PortChain portChain, FiveTuple fiveTuple,
            NshServicePathId nshSpiId) {
        checkNotNull(portChain, PORT_CHAIN_NOT_NULL);
        return installSfcFlowRules(portChain, fiveTuple, nshSpiId, Objective.Operation.REMOVE);
    }

    @Override
    public ConnectPoint unInstallLoadBalancedClassifierRules(PortChain portChain, FiveTuple fiveTuple,
            NshServicePathId nshSpiId) {
        checkNotNull(portChain, PORT_CHAIN_NOT_NULL);

        List<PortPairId> portPairs = portChain.getLoadBalancePath(fiveTuple);
        // Get the first port pair
        ListIterator<PortPairId> portPairListIterator = portPairs.listIterator();
        PortPairId portPairId = portPairListIterator.next();
        PortPair portPair = portPairService.getPortPair(portPairId);

        return installSfcClassifierRules(portChain, portPair, nshSpiId, fiveTuple, Objective.Operation.REMOVE);
    }

    public ConnectPoint installSfcFlowRules(PortChain portChain, FiveTuple fiveTuple, NshServicePathId nshSpiId,
            Objective.Operation type) {
        checkNotNull(portChain, PORT_CHAIN_NOT_NULL);

        classifierList = Lists.newArrayList();
        forwarderList = Lists.newArrayList();

        // Get the load balanced path
        List<PortPairId> portPairs = portChain.getLoadBalancePath(fiveTuple);

        // Get the first port pair
        ListIterator<PortPairId> portPairListIterator = portPairs.listIterator();
        PortPairId portPairId = portPairListIterator.next();
        PortPair currentPortPair = portPairService.getPortPair(portPairId);

        ConnectPoint connectPoint = installSfcClassifierRules(portChain, currentPortPair, nshSpiId, fiveTuple, type);

        log.info("Installing encap and output for first port pair");

        installSfcEncapOutputRule(currentPortPair, nshSpiId, type);

        PortPair nextPortPair;
        while (portPairListIterator.hasNext()) {
            portPairId = portPairListIterator.next();
            nextPortPair = portPairService.getPortPair(portPairId);
            installSfcForwardRule(currentPortPair, nextPortPair, nshSpiId, type);
            installSfcEncapOutputRule(nextPortPair, nshSpiId, type);
            currentPortPair = nextPortPair;
        }
        installSfcEndRule(currentPortPair, nshSpiId, type);

        if (type.equals(Objective.Operation.ADD)) {
            portChain.addSfcClassifiers(portChain.getLoadBalanceId(fiveTuple), classifierList);
            portChain.addSfcForwarders(portChain.getLoadBalanceId(fiveTuple), forwarderList);
        } else {
            portChain.removeSfcClassifiers(portChain.getLoadBalanceId(fiveTuple), classifierList);
            portChain.removeSfcForwarders(portChain.getLoadBalanceId(fiveTuple), forwarderList);
        }
        return connectPoint;
    }

    public void installSfcTunnelReceiveRule(DeviceId deviceId, NshServicePathId nshSpiId, Objective.Operation type) {

        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionSelectorResolver selectorResolver = handler.behaviour(ExtensionSelectorResolver.class);
        ExtensionSelector nshSpiSelector = selectorResolver.getExtensionSelector(NICIRA_MATCH_NSH_SPI.type());
        ExtensionSelector nshSiSelector = selectorResolver.getExtensionSelector(NICIRA_MATCH_NSH_SI.type());

        try {
            nshSpiSelector.setPropertyValue("nshSpi", nshSpiId);
        } catch (Exception e) {
            log.error("Failed to set extension selector to match Nsh Spi Id for end rule {}", e.getMessage());
        }
        try {
            nshSiSelector.setPropertyValue("nshSi", NshServiceIndex.of(nshSi));
        } catch (Exception e) {
            log.error("Failed to set extension selector to match Nsh Si Id for end rule {}", e.getMessage());
        }

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.extension(nshSpiSelector, deviceId);
        selector.extension(nshSiSelector, deviceId);

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(ENCAP_OUTPUT_TABLE);

        sendSfcRule(selector, treatment, deviceId, type, DEFAULT_FORWARDER_PRIORITY);
    }

    public void installSfcTunnelSendRule(DeviceId deviceId, NshServicePathId nshSpiId, Objective.Operation type) {

        // Prepare selector with nsp, nsi and inport from egress of port pair
        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionSelectorResolver selectorResolver = handler.behaviour(ExtensionSelectorResolver.class);
        ExtensionSelector nshSpiSelector = selectorResolver.getExtensionSelector(NICIRA_MATCH_NSH_SPI.type());
        ExtensionSelector nshSiSelector = selectorResolver.getExtensionSelector(NICIRA_MATCH_NSH_SI.type());
        ExtensionSelector encapEthTypeSelector = selectorResolver.getExtensionSelector(NICIRA_MATCH_ENCAP_ETH_TYPE
                                                                                       .type());
        try {
            nshSpiSelector.setPropertyValue("nshSpi", nshSpiId);
        } catch (Exception e) {
            log.error("Failed to set extension selector to match Nsh Spi Id for end rule {}", e.getMessage());
        }
        try {
            nshSiSelector.setPropertyValue("nshSi", NshServiceIndex.of(nshSi));
        } catch (Exception e) {
            log.error("Failed to set extension selector to match Nsh Si Id for end rule {}", e.getMessage());
        }
        try {
            encapEthTypeSelector.setPropertyValue("encapEthType", ENCAP_ETH_TYPE);
        } catch (Exception e) {
            log.error("Failed to set extension selector to match encapEthType {}", deviceId);
        }

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.extension(nshSpiSelector, deviceId);
        selector.extension(nshSiSelector, deviceId);

        ExtensionTreatmentResolver treatmentResolver = handler.behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment tunGpeNpTreatment = treatmentResolver.getExtensionInstruction(NICIRA_TUN_GPE_NP.type());
        try {
            tunGpeNpTreatment.setPropertyValue("tunGpeNp", ((byte) 4));
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set tunGpeNp {}", deviceId);
        }

        ExtensionTreatment moveC1ToC1 = treatmentResolver
                .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes
                                         .NICIRA_MOV_NSH_C1_TO_C1.type());

        ExtensionTreatment moveC2ToC2 = treatmentResolver
                .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes
                                         .NICIRA_MOV_NSH_C2_TO_C2.type());

        ExtensionTreatment moveC3ToC3 = treatmentResolver
                .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes
                                         .NICIRA_MOV_NSH_C3_TO_C3.type());

        ExtensionTreatment moveC4ToC4 = treatmentResolver
                .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes
                                         .NICIRA_MOV_NSH_C4_TO_C4.type());

        ExtensionTreatment moveTunIpv4DstToTunIpv4Dst = treatmentResolver
                .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes
                                         .NICIRA_MOV_TUN_IPV4_DST_TO_TUN_IPV4_DST.type());

        ExtensionTreatment moveTunIdToTunId = treatmentResolver
                .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes
                                         .NICIRA_MOV_TUN_ID_TO_TUN_ID.type());

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(tunGpeNpTreatment, deviceId);
        treatment.extension(moveC1ToC1, deviceId);
        treatment.extension(moveC2ToC2, deviceId);
        treatment.extension(moveC3ToC3, deviceId);
        treatment.extension(moveC4ToC4, deviceId);
        treatment.extension(moveTunIpv4DstToTunIpv4Dst, deviceId);
        treatment.extension(moveTunIdToTunId, deviceId);

        Iterable<Device> devices = deviceService.getAvailableDevices();
        DeviceId localControllerId = getControllerId(deviceService.getDevice(deviceId), devices);
        DriverHandler controllerHandler = driverService.createHandler(localControllerId);

        BridgeConfig bridgeConfig = controllerHandler.behaviour(BridgeConfig.class);
        Set<PortNumber> ports = bridgeConfig.getPortNumbers();
        String tunnelName = "vxlan-" + DEFAULT_IP;
        ports.stream()
        .filter(p -> p.name().equalsIgnoreCase(tunnelName))
        .forEach(p -> {
            treatment.setOutput(p);
            sendSfcRule(selector, treatment, deviceId, type, TUNNEL_SEND_PRIORITY);
        });
    }

    public void installSfcEndRule(PortPair portPair, NshServicePathId nshSpiId, Objective.Operation type) {
        DeviceId deviceId = vtnRscService.getSfToSffMaping(VirtualPortId.portId(portPair.egress()));
        MacAddress srcMacAddress = virtualPortService.getPort(VirtualPortId.portId(portPair.egress())).macAddress();
        Host host = hostService.getHost(HostId.hostId(srcMacAddress));
        PortNumber port = host.location().port();

        // Prepare selector with nsp, nsi and inport from egress of port pair
        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionSelectorResolver selectorResolver = handler.behaviour(ExtensionSelectorResolver.class);
        ExtensionSelector nshSpiSelector = selectorResolver.getExtensionSelector(NICIRA_MATCH_NSH_SPI.type());
        ExtensionSelector nshSiSelector = selectorResolver.getExtensionSelector(NICIRA_MATCH_NSH_SI.type());
        ExtensionSelector encapEthTypeSelector = selectorResolver.getExtensionSelector(NICIRA_MATCH_ENCAP_ETH_TYPE
                .type());
        try {
            nshSpiSelector.setPropertyValue("nshSpi", nshSpiId);
        } catch (Exception e) {
            log.error("Failed to set extension selector to match Nsh Spi Id for end rule {}", e.getMessage());
        }
        // Decrement the SI
        nshSi = (short) (nshSi - 1);
        try {
            nshSiSelector.setPropertyValue("nshSi", NshServiceIndex.of(nshSi));
        } catch (Exception e) {
            log.error("Failed to set extension selector to match Nsh Si Id for end rule {}", e.getMessage());
        }
        try {
            encapEthTypeSelector.setPropertyValue("encapEthType", ENCAP_ETH_TYPE);
        } catch (Exception e) {
            log.error("Failed to set extension selector to match encapEthType {}", deviceId);
        }
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.extension(encapEthTypeSelector, deviceId);
        selector.extension(nshSpiSelector, deviceId);
        selector.extension(nshSiSelector, deviceId);
        selector.matchInPort(port);

        // Set treatment to pop nsh header, set tunnel id and resubmit to table
        // 0.
        ExtensionTreatmentResolver treatmentResolver = handler.behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment popNshTreatment = treatmentResolver.getExtensionInstruction(NICIRA_POP_NSH.type());

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(popNshTreatment, deviceId);

        VirtualPort virtualPort = virtualPortService.getPort(VirtualPortId.portId(portPair.ingress()));
        SegmentationId segmentationId = tenantNetworkService.getNetwork(virtualPort.networkId()).segmentationId();
        treatment.add(Instructions.modTunnelId(Long.parseLong(segmentationId.toString())));

        ExtensionTreatment resubmitTableTreatment = treatmentResolver.getExtensionInstruction(NICIRA_RESUBMIT_TABLE
                .type());

        PortNumber vxlanPortNumber = getVxlanPortNumber(deviceId);

        try {
            resubmitTableTreatment.setPropertyValue("inPort", vxlanPortNumber);
        } catch (Exception e) {
            log.error("Failed to set extension treatment for resubmit table in port {}", deviceId);
        }
        try {
            resubmitTableTreatment.setPropertyValue("table", ((short) 0));
        } catch (Exception e) {
            log.error("Failed to set extension treatment for resubmit table {}", deviceId);
        }
        treatment.extension(resubmitTableTreatment, deviceId);

        sendSfcRule(selector, treatment, deviceId, type, DEFAULT_FORWARDER_PRIORITY);
    }

    public void installSfcForwardRule(PortPair portPair, PortPair nextPortPair, NshServicePathId nshSpiId,
            Objective.Operation type) {
        DeviceId deviceId = vtnRscService.getSfToSffMaping(VirtualPortId.portId(portPair.egress()));
        MacAddress srcMacAddress = virtualPortService.getPort(VirtualPortId.portId(portPair.egress())).macAddress();
        Host host = hostService.getHost(HostId.hostId(srcMacAddress));
        PortNumber port = host.location().port();

        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionSelectorResolver resolver = handler.behaviour(ExtensionSelectorResolver.class);

        // Prepare selector with nsp, nsi and inport from egress of port pair
        ExtensionSelector nshSpiSelector = resolver.getExtensionSelector(NICIRA_MATCH_NSH_SPI.type());
        ExtensionSelector nshSiSelector = resolver.getExtensionSelector(NICIRA_MATCH_NSH_SI.type());
        try {
            nshSpiSelector.setPropertyValue("nshSpi", nshSpiId);
        } catch (Exception e) {
            log.error("Failed to set extension selector to match Nsh Spi Id for forward rule {}", e.getMessage());
        }
        // Decrement the SI
        nshSi = (short) (nshSi - 1);
        try {
            nshSiSelector.setPropertyValue("nshSi", NshServiceIndex.of(nshSi));
        } catch (Exception e) {
            log.error("Failed to set extension selector to match Nsh Si Id for forward rule {}", e.getMessage());
        }
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.extension(nshSpiSelector, deviceId);
        selector.extension(nshSiSelector, deviceId);
        selector.matchInPort(port);

        DeviceId nextDeviceId = vtnRscService.getSfToSffMaping(VirtualPortId.portId(nextPortPair.ingress()));
        if (deviceId.equals(nextDeviceId)) {

            // Treatment with transition
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.transition(ENCAP_OUTPUT_TABLE);

            sendSfcRule(selector, treatment, deviceId, type, DEFAULT_FORWARDER_PRIORITY);
        } else {
            // Treatment with with transition to send on tunnel
            ExtensionTreatmentResolver treatmentResolver = handler.behaviour(ExtensionTreatmentResolver.class);
            ExtensionTreatment moveC2ToTunId = treatmentResolver
                    .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes
                                             .NICIRA_MOV_NSH_C2_TO_TUN_ID.type());

            Device remoteDevice = deviceService.getDevice(nextDeviceId);
            String url = remoteDevice.annotations().value(SWITCH_CHANNEL_ID);
            String remoteControllerIp = url.substring(0, url.lastIndexOf(":"));
            if (remoteControllerIp == null) {
                log.error("Can't find remote controller of device: {}", nextDeviceId.toString());
                return;
            }

            ExtensionTreatment tunnelDsttreatment = treatmentResolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST
                                                                                              .type());
            try {
                tunnelDsttreatment.setPropertyValue("tunnelDst", Ip4Address.valueOf(remoteControllerIp));
            } catch (Exception e) {
                log.error("Failed to get extension instruction to set tunnel dst {}", deviceId);
            }

            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.extension(moveC2ToTunId, deviceId);
            treatment.extension(tunnelDsttreatment, deviceId);
            treatment.transition(TUNNEL_SEND_TABLE);

            sendSfcRule(selector, treatment, deviceId, type, DEFAULT_FORWARDER_PRIORITY);

            installSfcTunnelSendRule(deviceId, nshSpiId, type);
            installSfcTunnelReceiveRule(nextDeviceId, nshSpiId, type);
        }
    }

    public void installSfcEncapOutputRule(PortPair portPair, NshServicePathId nshSpiId, Objective.Operation type) {

        DeviceId deviceId = vtnRscService.getSfToSffMaping(VirtualPortId.portId(portPair.ingress()));
        MacAddress srcMacAddress = virtualPortService.getPort(VirtualPortId.portId(portPair.ingress())).macAddress();
        Host host = hostService.getHost(HostId.hostId(srcMacAddress));
        PortNumber port = host.location().port();

        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionSelectorResolver resolver = handler.behaviour(ExtensionSelectorResolver.class);

        // Prepare selector with nsp, nsi and encap eth type
        ExtensionSelector nshSpiSelector = resolver.getExtensionSelector(NICIRA_MATCH_NSH_SPI.type());
        ExtensionSelector nshSiSelector = resolver.getExtensionSelector(NICIRA_MATCH_NSH_SI.type());
        ExtensionSelector nshEncapEthTypeSelector = resolver.getExtensionSelector(NICIRA_MATCH_ENCAP_ETH_TYPE.type());

        try {
            nshSpiSelector.setPropertyValue("nshSpi", nshSpiId);
        } catch (Exception e) {
            log.error("Failed to set extension selector to match Nsh Spi Id for encap rule {}", e.getMessage());
        }
        try {
            nshSiSelector.setPropertyValue("nshSi", NshServiceIndex.of(nshSi));
        } catch (Exception e) {
            log.error("Failed to set extension selector to match Nsh Si Id for encap rule {}", e.getMessage());
        }
        try {
            nshEncapEthTypeSelector.setPropertyValue("encapEthType", ENCAP_ETH_TYPE);
        } catch (Exception e) {
            log.error("Failed to set extension selector to match Nsh Si Id {}", deviceId);
        }
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.extension(nshSpiSelector, deviceId);
        selector.extension(nshSiSelector, deviceId);

        ExtensionTreatmentResolver treatmentResolver = handler.behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment encapEthSrcTreatment = treatmentResolver
                .getExtensionInstruction(NICIRA_ENCAP_ETH_SRC.type());
        ExtensionTreatment encapEthDstTreatment = treatmentResolver
                .getExtensionInstruction(NICIRA_ENCAP_ETH_DST.type());

        try {
            encapEthDstTreatment.setPropertyValue("encapEthDst", srcMacAddress);
        } catch (Exception e) {
            log.error("Failed to set extension treatment to set encap eth dst {}", deviceId);
        }
        // TODO: move from packet source mac address
        try {
            encapEthSrcTreatment.setPropertyValue("encapEthSrc", srcMacAddress);
        } catch (Exception e) {
            log.error("Failed to set extension treatment to set encap eth src {}", deviceId);
        }

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(encapEthSrcTreatment, deviceId);
        treatment.extension(encapEthDstTreatment, deviceId);
        treatment.setOutput(port);

        sendSfcRule(selector, treatment, deviceId, type, ENCAP_OUTPUT_PRIORITY);
        forwarderList.add(deviceId);
    }

    public ConnectPoint installSfcClassifierRules(PortChain portChain, PortPair portPair, NshServicePathId nshSpiId,
            FiveTuple fiveTuple, Objective.Operation type) {

        DeviceId deviceIdfromPortPair = vtnRscService.getSfToSffMaping(VirtualPortId.portId(portPair.ingress()));
        MacAddress srcMacAddress = virtualPortService.getPort(VirtualPortId.portId(portPair.ingress())).macAddress();
        VirtualPort virtualPort = virtualPortService.getPort(VirtualPortId.portId(portPair.ingress()));
        Host host = hostService.getHost(HostId.hostId(srcMacAddress));
        PortNumber port = host.location().port();

        DeviceId deviceId = deviceIdfromPortPair;

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
            TrafficSelector.Builder selector = packClassifierSelector(flowClassifier, fiveTuple);

            if (fiveTuple == null) {
                // Send the packet to controller
                log.info("Downloading rule to send packet to controller");
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                treatment.setOutput(PortNumber.CONTROLLER);
                sendSfcRule(selector, treatment, deviceId, type, FLOW_CLASSIFIER_PRIORITY);
                continue;
            }

            if (deviceId != null && !deviceId.equals(deviceIdfromPortPair)) {
                // First SF is in another device. Set tunnel ipv4 destination to
                // treatment
                Device remoteDevice = deviceService.getDevice(deviceIdfromPortPair);
                String url = remoteDevice.annotations().value(SWITCH_CHANNEL_ID);
                String remoteControllerIp = url.substring(0, url.lastIndexOf(":"));
                if (remoteControllerIp == null) {
                    log.error("Can't find remote controller of device: {}", deviceIdfromPortPair.toString());
                    return null;
                }

                DriverHandler handler = driverService.createHandler(deviceId);
                ExtensionTreatmentResolver resolver = handler.behaviour(ExtensionTreatmentResolver.class);
                ExtensionTreatment tunnelDsttreatment = resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
                try {
                    tunnelDsttreatment.setPropertyValue("tunnelDst", Ip4Address.valueOf(remoteControllerIp));
                } catch (Exception e) {
                    log.error("Failed to get extension instruction to set tunnel dst {}", deviceId);
                }

                TrafficTreatment.Builder treatment = packClassifierTreatment(deviceId, virtualPort, port,
                                                                             nshSpiId, flowClassifier);
                treatment.extension(tunnelDsttreatment, deviceId);
                treatment.transition(TUNNEL_SEND_TABLE);
                sendSfcRule(selector, treatment, deviceId, type, flowClassifier.priority());

                selector.matchInPort(PortNumber.CONTROLLER);
                sendSfcRule(selector, treatment, deviceId, type, flowClassifier.priority());
                classifierList.add(deviceId);

                installSfcTunnelSendRule(deviceId, nshSpiId, type);
                installSfcTunnelReceiveRule(deviceIdfromPortPair, nshSpiId, type);

            } else {
                // classifier and port pair are in the same OVS. So directly
                // send packet to first port pair
                TrafficTreatment.Builder treatment = packClassifierTreatment(deviceIdfromPortPair, virtualPort, port,
                                                                             nshSpiId, flowClassifier);
                treatment.transition(ENCAP_OUTPUT_TABLE);
                sendSfcRule(selector, treatment, deviceIdfromPortPair, type, flowClassifier.priority());

                selector.matchInPort(PortNumber.CONTROLLER);
                sendSfcRule(selector, treatment, deviceId, type, flowClassifier.priority());
                classifierList.add(deviceIdfromPortPair);
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
    public TrafficSelector.Builder packClassifierSelector(FlowClassifier flowClassifier, FiveTuple fiveTuple) {

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
            if ("TCP".equalsIgnoreCase(flowClassifier.protocol())) {
                selector.add(Criteria.matchIPProtocol(IPv4.PROTOCOL_TCP));
            } else if ("UDP".equalsIgnoreCase(flowClassifier.protocol())) {
                selector.add(Criteria.matchIPProtocol(IPv4.PROTOCOL_UDP));
            } else if ("ICMP".equalsIgnoreCase(flowClassifier.protocol())) {
                selector.add(Criteria.matchIPProtocol(IPv4.PROTOCOL_ICMP));
            }
        } else if (fiveTuple != null && fiveTuple.protocol() != 0) {
            selector.add(Criteria.matchIPProtocol(fiveTuple.protocol()));
        }

        if (((flowClassifier.etherType() != null) && (!flowClassifier.etherType().isEmpty()))
                && ("IPv4".equals(flowClassifier.etherType()) || "IPv6".equals(flowClassifier.etherType()))) {
            if ("IPv4".equals(flowClassifier.etherType())) {
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

        // Take the port information from five tuple only when the protocol is
        // TCP.
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
     * @param virtualPort virtual port
     * @param port port number
     * @param nshSpi nsh spi
     * @param flowClassifier flow-classifier
     * @return traffic treatment
     */
    public TrafficTreatment.Builder packClassifierTreatment(DeviceId deviceId, VirtualPort virtualPort,
            PortNumber port, NshServicePathId nshSpi, FlowClassifier flowClassifier) {

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        // set tunnel id
        SegmentationId segmentationId = tenantNetworkService.getNetwork(virtualPort.networkId()).segmentationId();
        treatmentBuilder.add(Instructions.modTunnelId(Long.parseLong(segmentationId.toString())));

        // Set all NSH header fields
        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionTreatmentResolver resolver = handler.behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment nspIdTreatment = resolver.getExtensionInstruction(NICIRA_SET_NSH_SPI.type());
        ExtensionTreatment nsiIdTreatment = resolver.getExtensionInstruction(NICIRA_SET_NSH_SI.type());
        ExtensionTreatment pushNshTreatment = resolver.getExtensionInstruction(NICIRA_PUSH_NSH.type());

        ExtensionTreatment nshCh1Treatment = resolver.getExtensionInstruction(NICIRA_SET_NSH_CH1.type());
        ExtensionTreatment nshCh2Treatment = resolver.getExtensionInstruction(NICIRA_SET_NSH_CH2.type());
        ExtensionTreatment nshCh3Treatment = resolver.getExtensionInstruction(NICIRA_SET_NSH_CH3.type());
        ExtensionTreatment nshCh4Treatment = resolver.getExtensionInstruction(NICIRA_SET_NSH_CH4.type());
        ExtensionTreatment nshMdTypeTreatment = resolver.getExtensionInstruction(NICIRA_NSH_MDTYPE.type());
        ExtensionTreatment nshNpTreatment = resolver.getExtensionInstruction(NICIRA_NSH_NP.type());

        try {
            nshMdTypeTreatment.setPropertyValue("nshMdType", ((byte) 1));
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set nshMdType {}", deviceId);
        }
        try {
            nshNpTreatment.setPropertyValue("nshNp", ((byte) 3));
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set nshNp {}", deviceId);
        }
        try {
            nspIdTreatment.setPropertyValue("nshSpi", nshSpi);
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set Nsh Spi Id {}", deviceId);
        }
        try {
            nsiIdTreatment.setPropertyValue("nshSi", NshServiceIndex.of(nshSi));
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set Nsh Si Id {}", deviceId);
        }
        try {
            nshCh1Treatment.setPropertyValue("nshCh", NshContextHeader.of(1));
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set NshCh1 {}", deviceId);
        }
        try {
            nshCh2Treatment.setPropertyValue("nshCh", NshContextHeader.of(Integer.parseInt(segmentationId.toString())));
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set NshCh2 {}", deviceId);
        }
        try {
            nshCh3Treatment.setPropertyValue("nshCh", NshContextHeader.of(3));
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set NshCh3 {}", deviceId);
        }
        try {
            nshCh4Treatment.setPropertyValue("nshCh", NshContextHeader.of(4));
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set NshCh4 {}", deviceId);
        }
        treatmentBuilder.extension(pushNshTreatment, deviceId);
        treatmentBuilder.extension(nshMdTypeTreatment, deviceId);
        treatmentBuilder.extension(nshNpTreatment, deviceId);
        treatmentBuilder.extension(nspIdTreatment, deviceId);
        treatmentBuilder.extension(nsiIdTreatment, deviceId);
        treatmentBuilder.extension(nshCh1Treatment, deviceId);
        treatmentBuilder.extension(nshCh2Treatment, deviceId);
        treatmentBuilder.extension(nshCh3Treatment, deviceId);
        treatmentBuilder.extension(nshCh4Treatment, deviceId);

        return treatmentBuilder;
    }

    /**
     * Get the ControllerId from the device .
     *
     * @param device Device
     * @param devices Devices
     * @return Controller Id
     */
    public DeviceId getControllerId(Device device, Iterable<Device> devices) {
        for (Device d : devices) {
            if (d.type() == Device.Type.CONTROLLER && d.id().toString()
                    .contains(getControllerIpOfSwitch(device))) {
                return d.id();
            }
        }
        log.info("Can not find controller for device : {}", device.id());
        return null;
    }

    /**
     * Get the ControllerIp from the device .
     *
     * @param device Device
     * @return Controller Ip
     */
    public String getControllerIpOfSwitch(Device device) {
        String url = device.annotations().value(SWITCH_CHANNEL_ID);
        return url.substring(0, url.lastIndexOf(":"));
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
    public void sendSfcRule(TrafficSelector.Builder selector, TrafficTreatment.Builder treatment, DeviceId deviceId,
            Objective.Operation type, int priority) {

        log.info("Sending sfc flow rule. Selector {}, Treatment {}", selector.toString(),
                 treatment.toString());
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

    private PortNumber getVxlanPortNumber(DeviceId deviceId) {
        Iterable<Port> ports = deviceService.getPorts(deviceId);
        Port vxlanPort = Sets.newHashSet(ports).stream()
                .filter(p -> !p.number().equals(PortNumber.LOCAL))
                .filter(p -> p.annotations().value(AnnotationKeys.PORT_NAME)
                        .startsWith(VXLANPORT_HEAD))
                .findFirst().get();
        return vxlanPort.number();
    }
}
