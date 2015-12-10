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
package org.onosproject.sfc.forwarder.impl;

import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.net.flow.criteria.ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SPI;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.ListIterator;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.NshServicePathId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.host.HostService;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.service.VtnRscService;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierService;
import org.onosproject.vtnrsc.portchain.PortChainService;
import org.onosproject.sfc.forwarder.ServiceFunctionForwarderService;

import org.slf4j.Logger;

/**
 * Provides Service Function Forwarder implementation.
 */
@Component(immediate = true)
@Service
public class ServiceFunctionForwarderImpl implements ServiceFunctionForwarderService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;
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
    protected PortChainService portChainService;

    private final Logger log = getLogger(getClass());
    protected ApplicationId appId;
    protected FlowObjectiveService flowObjectiveService;

    private static final String DRIVER_NAME = "onosfw";
    private static final String PORT_CHAIN_NOT_NULL = "Port-Chain cannot be null";
    private static final String PORT_CHAIN_ID_NOT_NULL = "Port-Chain-Id cannot be null";
    private static final String APP_ID_NOT_NULL = "Application-Id cannot be null";
    private static final int NULL = 0;

    /**
     * Default constructor.
     */
    public ServiceFunctionForwarderImpl() {
    }

    /**
     * Explicit constructor.
     *
     * @param appId Application id
     */
    public ServiceFunctionForwarderImpl(ApplicationId appId) {
        this.appId = checkNotNull(appId, APP_ID_NOT_NULL);
        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        this.flowObjectiveService = serviceDirectory.get(FlowObjectiveService.class);
    }

    @Override
    public void installForwardingRule(PortChain portChain, NshServicePathId nshSPI) {
        checkNotNull(portChain, PORT_CHAIN_NOT_NULL);
        prepareServiceFunctionForwarder(portChain, nshSPI, Objective.Operation.ADD);
    }

    @Override
    public void unInstallForwardingRule(PortChain portChain, NshServicePathId nshSPI) {
        checkNotNull(portChain, PORT_CHAIN_NOT_NULL);
        prepareServiceFunctionForwarder(portChain, nshSPI, Objective.Operation.REMOVE);
    }

    @Override
    public void prepareServiceFunctionForwarder(PortChain portChain, NshServicePathId nshSPI,
                                                Objective.Operation type) {

        // Go through the port pair group list
        List<PortPairGroupId> portPairGrpList = portChain.portPairGroups();
        ListIterator<PortPairGroupId> listGrpIterator = portPairGrpList.listIterator();

        // Get source port pair group
        if (!listGrpIterator.hasNext()) {
            return;
        }
        PortPairGroupId portPairGrpId = listGrpIterator.next();
        PortPairGroup currentPortPairGroup = portPairGroupService.getPortPairGroup(portPairGrpId);

        // Get destination port pair group
        if (!listGrpIterator.hasNext()) {
            return;
        }
        portPairGrpId = listGrpIterator.next();
        PortPairGroup nextPortPairGroup = portPairGroupService.getPortPairGroup(portPairGrpId);

        // push SFF to OVS
        pushServiceFunctionForwarder(currentPortPairGroup, nextPortPairGroup, listGrpIterator, nshSPI, type);
    }

    /**
     * Push service-function-forwarder to OVS.
     *
     * @param currentPortPairGroup current port-pair-group
     * @param nextPortPairGroup next port-pair-group
     * @param listGrpIterator pointer to port-pair-group list
     * @param nshSPI nsh service path id
     * @param type objective type
     */
    public void pushServiceFunctionForwarder(PortPairGroup currentPortPairGroup, PortPairGroup nextPortPairGroup,
            ListIterator<PortPairGroupId> listGrpIterator, NshServicePathId nshSPI, Objective.Operation type) {
        DeviceId deviceId = null;
        DeviceId currentDeviceId = null;
        DeviceId nextDeviceId = null;
        PortPairGroupId portPairGrpId = null;

        // Travel from SF to SF.
        do {
            // Get the required information on port pairs from source port pair
            // group
            List<PortPairId> portPairList = currentPortPairGroup.portPairs();
            ListIterator<PortPairId> portPLIterator = portPairList.listIterator();
            if (!portPLIterator.hasNext()) {
                break;
            }

            PortPairId portPairId = portPLIterator.next();
            PortPair portPair = portPairService.getPortPair(portPairId);

            currentDeviceId = vtnRscService.getSFToSFFMaping(VirtualPortId.portId(portPair.ingress()));
            if (deviceId == null) {
                deviceId = currentDeviceId;
            }

            // pack traffic selector
            TrafficSelector.Builder selector = packTrafficSelector(deviceId, portPair, nshSPI);

            // Get the required information on port pairs from destination port
            // pair group
            portPairList = nextPortPairGroup.portPairs();
            portPLIterator = portPairList.listIterator();
            if (!portPLIterator.hasNext()) {
                break;
            }

            portPairId = portPLIterator.next();
            portPair = portPairService.getPortPair(portPairId);

            nextDeviceId = vtnRscService.getSFToSFFMaping(VirtualPortId.portId(portPair.ingress()));

            // pack traffic treatment
            TrafficTreatment.Builder treatment = packTrafficTreatment(currentDeviceId, nextDeviceId, portPair);

            // Send SFF to OVS
            sendServiceFunctionForwarder(selector, treatment, deviceId, type);

            // Replace source port pair group with destination port pair group
            // for moving to next SFF processing.
            currentPortPairGroup = nextPortPairGroup;
            if (!listGrpIterator.hasNext()) {
                break;
            }
            portPairGrpId = listGrpIterator.next();
            nextPortPairGroup = portPairGroupService.getPortPairGroup(portPairGrpId);
        } while (true);
    }

    /**
     * Pack Traffic selector.
     *
     * @param deviceId device id
     * @param portPair port-pair
     * @param nshSPI nsh spi
     * @return traffic treatment
     */
    public TrafficSelector.Builder packTrafficSelector(DeviceId deviceId, PortPair portPair, NshServicePathId nshSPI) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        MacAddress dstMacAddress = virtualPortService.getPort(VirtualPortId.portId(portPair.egress())).macAddress();
        Host host = hostService.getHost(HostId.hostId(dstMacAddress));
        PortNumber port = host.location().port();
        selector.matchInPort(port);

        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionSelectorResolver resolver = handler.behaviour(ExtensionSelectorResolver.class);
        ExtensionSelector nspSpiSelector = resolver.getExtensionSelector(NICIRA_MATCH_NSH_SPI.type());

        try {
            nspSpiSelector.setPropertyValue("nshSpi", nshSPI);
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set Nsh Spi Id {}", deviceId);
        }

        selector.extension(nspSpiSelector, deviceId);

        return selector;
    }

    /**
     * Pack Traffic treatment.
     *
     * @param currentDeviceId current device id
     * @param nextDeviceId next device id
     * @param portPair port-pair
     * @return traffic treatment
     */
    public TrafficTreatment.Builder packTrafficTreatment(DeviceId currentDeviceId, DeviceId nextDeviceId,
            PortPair portPair) {
        MacAddress srcMacAddress = null;

        // Check the treatment whether destination SF is on same OVS or in
        // different OVS.
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        if (currentDeviceId.equals(nextDeviceId)) {
            srcMacAddress = virtualPortService.getPort(VirtualPortId.portId(portPair.ingress())).macAddress();

            Host host = hostService.getHost(HostId.hostId(srcMacAddress));
            PortNumber port = host.location().port();
            treatment.setOutput(port);
        } else {
            VlanId vlanId = VlanId.vlanId(Short.parseShort((vtnRscService.getL3vni(portPair.tenantId()).toString())));
            treatment.setVlanId(vlanId);
        }

        return treatment;
    }

    /**
     * Send service function forwarder to OVS.
     *
     * @param selector traffic selector
     * @param treatment traffic treatment
     * @param deviceId device id
     * @param type operation type
     */
    public void sendServiceFunctionForwarder(TrafficSelector.Builder selector, TrafficTreatment.Builder treatment,
            DeviceId deviceId, Objective.Operation type) {
        ForwardingObjective.Builder objective = DefaultForwardingObjective.builder().withTreatment(treatment.build())
                .withSelector(selector.build()).fromApp(appId).makePermanent().withFlag(Flag.VERSATILE);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }
}
