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
package org.onosproject.sfc.forwarder.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.flow.criteria.ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SPI;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.ListIterator;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.NshServicePathId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.host.HostService;
import org.onosproject.sfc.forwarder.ServiceFunctionForwarderService;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierService;
import org.onosproject.vtnrsc.portchain.PortChainService;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.onosproject.vtnrsc.service.VtnRscService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

/**
 * Provides service function forwarder implementation.
 */
public class ServiceFunctionForwarderImpl implements ServiceFunctionForwarderService {

    private final Logger log = getLogger(getClass());
    protected VirtualPortService virtualPortService;
    protected VtnRscService vtnRscService;
    protected PortPairService portPairService;
    protected PortPairGroupService portPairGroupService;
    protected FlowClassifierService flowClassifierService;
    protected PortChainService portChainService;
    protected DriverService driverService;
    protected FlowObjectiveService flowObjectiveService;
    protected HostService hostService;
    protected ApplicationId appId;

    private static final String PATH_NOT_NULL = "Load balanced path cannot be null";
    private static final String APP_ID_NOT_NULL = "Application-Id cannot be null";

    /**
     * Default constructor.
     */
    public ServiceFunctionForwarderImpl() {
    }

    /**
     * Explicit constructor.
     *
     * @param appId application id
     */
    public ServiceFunctionForwarderImpl(ApplicationId appId) {
        this.appId = checkNotNull(appId, APP_ID_NOT_NULL);
        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        this.flowObjectiveService = serviceDirectory.get(FlowObjectiveService.class);
        this.driverService = serviceDirectory.get(DriverService.class);
        this.virtualPortService = serviceDirectory.get(VirtualPortService.class);
        this.vtnRscService = serviceDirectory.get(VtnRscService.class);
        this.portPairService = serviceDirectory.get(PortPairService.class);
        this.portPairGroupService = serviceDirectory.get(PortPairGroupService.class);
        this.flowClassifierService = serviceDirectory.get(FlowClassifierService.class);
        this.hostService = serviceDirectory.get(HostService.class);
        this.portChainService = serviceDirectory.get(PortChainService.class);
    }

    @Override
    public void installForwardingRule(PortChain portChain, NshServicePathId nshSpi) {
        //TODO this method will be removed
    }

    @Override
    public void unInstallForwardingRule(PortChain portChain, NshServicePathId nshSpi) {
        //TODO this method will be removed
    }

    @Override
    public void installLoadBalancedForwardingRule(List<PortPairId> path, NshServicePathId nshSpi) {
        checkNotNull(path, PATH_NOT_NULL);
        processForwardingRule(path, nshSpi, Objective.Operation.ADD);
    }

    @Override
    public void unInstallLoadBalancedForwardingRule(List<PortPairId> path, NshServicePathId nshSpi) {
        checkNotNull(path, PATH_NOT_NULL);
        processForwardingRule(path, nshSpi, Objective.Operation.REMOVE);
    }

    /**
     * Process the required forwarding rules for the given path.
     *
     * @param path list of port pair ids
     * @param nshSpi service path index
     * @param type operation type ADD/REMOVE
     */
    private void processForwardingRule(List<PortPairId> path, NshServicePathId nshSpi,
                                       Objective.Operation type) {

        // Get the first port pair
        ListIterator<PortPairId> portPairListIterator = path.listIterator();
        PortPair currentPortPair = portPairService.getPortPair(portPairListIterator.next());

        // Get destination port pair group
        if (!portPairListIterator.hasNext()) {
            log.debug("Path is empty");
            return;
        }
        PortPair nextPortPair = portPairService.getPortPair(portPairListIterator.next());
        DeviceId currentDeviceId = null;
        DeviceId nextDeviceId = null;

        // Travel from SF to SF.
        do {
            currentDeviceId = vtnRscService.getSfToSffMaping(VirtualPortId.portId(currentPortPair.egress()));
            nextDeviceId = vtnRscService.getSfToSffMaping(VirtualPortId.portId(nextPortPair.ingress()));
            // pack traffic selector
            TrafficSelector.Builder selector = packTrafficSelector(currentDeviceId, currentPortPair, nshSpi);
            // Pack treatment
            if (currentDeviceId.equals(nextDeviceId)) {
                TrafficTreatment.Builder treatment = packTrafficTreatment(nextPortPair, true);
                // Send SFF to SFF
                sendServiceFunctionForwarder(selector, treatment, currentDeviceId, type);
            } else {
                TrafficTreatment.Builder treatment = packTrafficTreatment(nextPortPair, false);
                // Send SFF to OVS
                sendServiceFunctionForwarder(selector, treatment, currentDeviceId, type);

                // At the other device get the packet from vlan and send to first port pair
                TrafficSelector.Builder selectorDst = DefaultTrafficSelector.builder();
                selectorDst.matchVlanId((VlanId.vlanId(Short.parseShort((vtnRscService
                        .getL3vni(nextPortPair.tenantId()).toString())))));
                TrafficTreatment.Builder treatmentDst = DefaultTrafficTreatment.builder();
                MacAddress macAddr = virtualPortService.getPort(VirtualPortId.portId(nextPortPair.ingress()))
                        .macAddress();
                Host host = hostService.getHost(HostId.hostId(macAddr));
                PortNumber port = host.location().port();
                treatmentDst.setOutput(port);
                // Send OVS to SFF
                sendServiceFunctionForwarder(selectorDst, treatmentDst, nextDeviceId, type);
            }

            // Move to next service function
            currentPortPair = nextPortPair;
            if (!portPairListIterator.hasNext()) {
                break;
            }
            nextPortPair = portPairService.getPortPair(portPairListIterator.next());
        } while (true);
    }

    /**
     * Pack traffic selector.
     *
     * @param deviceId device id
     * @param portPair port-pair
     * @param nshSpi nsh service path index
     * @return traffic selector
     */
    public TrafficSelector.Builder packTrafficSelector(DeviceId deviceId,
                                                       PortPair portPair, NshServicePathId nshSpi) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        MacAddress dstMacAddress = virtualPortService.getPort(VirtualPortId.portId(portPair.egress())).macAddress();
        Host host = hostService.getHost(HostId.hostId(dstMacAddress));
        PortNumber port = host.location().port();
        selector.matchInPort(port);

        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionSelectorResolver resolver = handler.behaviour(ExtensionSelectorResolver.class);
        ExtensionSelector nspSpiSelector = resolver.getExtensionSelector(NICIRA_MATCH_NSH_SPI.type());

        try {
            nspSpiSelector.setPropertyValue("nshSpi", nshSpi);
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set Nsh Spi Id {}", deviceId);
        }

        selector.extension(nspSpiSelector, deviceId);

        return selector;
    }

    /**
     * Pack traffic treatment.
     *
     * @param portPair port pair
     * @param isSameOvs whether the next port pair is in the same ovs
     * @return traffic treatment
     */
    public TrafficTreatment.Builder packTrafficTreatment(PortPair portPair, boolean isSameOvs) {
        MacAddress srcMacAddress = null;

        // Check the treatment whether destination SF is on same OVS or in
        // different OVS.
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        if (isSameOvs) {
            srcMacAddress = virtualPortService.getPort(VirtualPortId.portId(portPair.ingress())).macAddress();
            Host host = hostService.getHost(HostId.hostId(srcMacAddress));
            PortNumber port = host.location().port();
            treatment.setOutput(port);
        } else {
            // Vxlan tunnel port for NSH header(Vxlan + NSH).
            TpPort nshDstPort = TpPort.tpPort(6633);
            // TODO check whether this logic is correct
            VlanId vlanId = VlanId.vlanId(Short.parseShort((vtnRscService.getL3vni(portPair.tenantId()).toString())));
            treatment.setVlanId(vlanId);
            treatment.setUdpDst(nshDstPort);
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
        log.info("Sending flow to serfice-function-forwarder. Selector {}, Treatment {}",
                 selector.toString(), treatment.toString());
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
