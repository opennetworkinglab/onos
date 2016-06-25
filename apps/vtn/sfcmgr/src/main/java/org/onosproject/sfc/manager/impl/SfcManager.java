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
package org.onosproject.sfc.manager.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TCP;
import org.onlab.packet.UDP;
import org.onlab.util.ItemNotFoundException;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.NshServicePathId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.sfc.forwarder.ServiceFunctionForwarderService;
import org.onosproject.sfc.forwarder.impl.ServiceFunctionForwarderImpl;
import org.onosproject.sfc.installer.FlowClassifierInstallerService;
import org.onosproject.sfc.installer.impl.FlowClassifierInstallerImpl;
import org.onosproject.sfc.manager.SfcService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.DefaultFiveTuple;
import org.onosproject.vtnrsc.FiveTuple;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.LoadBalanceId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.event.VtnRscEvent;
import org.onosproject.vtnrsc.event.VtnRscEventFeedback;
import org.onosproject.vtnrsc.event.VtnRscListener;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierService;
import org.onosproject.vtnrsc.portchain.PortChainService;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.onosproject.vtnrsc.service.VtnRscService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

/**
 * Provides implementation of SFC Service.
 */
@Component(immediate = true)
@Service
public class SfcManager implements SfcService {

    private final Logger log = getLogger(getClass());

    private String nshSpiIdTopic = "nsh-spi-id";
    private static final String APP_ID = "org.onosproject.app.vtn";
    private static final int SFC_PRIORITY = 1000;
    private static final int NULL_PORT = 0;
    private static final int MAX_NSH_SPI_ID = 0x7FFFF;
    private static final int MAX_LOAD_BALANCE_ID = 0x20;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VtnRscService vtnRscService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PortChainService portChainService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PortPairGroupService portPairGroupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowClassifierService flowClassifierService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualPortService virtualPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    protected SfcPacketProcessor processor = new SfcPacketProcessor();

    protected ApplicationId appId;
    protected ServiceFunctionForwarderService serviceFunctionForwarder;
    protected FlowClassifierInstallerService flowClassifierInstaller;
    protected IdGenerator nshSpiIdGenerator;
    protected EventuallyConsistentMap<PortChainId, Integer> nshSpiPortChainMap;
    protected DistributedSet<Integer> nshSpiIdFreeList;

    private final VtnRscListener vtnRscListener = new InnerVtnRscListener();

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        serviceFunctionForwarder = new ServiceFunctionForwarderImpl(appId);
        flowClassifierInstaller = new FlowClassifierInstallerImpl(appId);
        nshSpiIdGenerator = coreService.getIdGenerator(nshSpiIdTopic);

        vtnRscService.addListener(vtnRscListener);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(TenantId.class)
                .register(PortPairId.class)
                .register(PortPairGroupId.class)
                .register(FlowClassifierId.class)
                .register(PortChainId.class);

        nshSpiPortChainMap = storageService.<PortChainId, Integer>eventuallyConsistentMapBuilder()
                .withName("nshSpiPortChainMap")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        nshSpiIdFreeList = storageService.<Integer>setBuilder()
                .withName("nshSpiIdDeletedList")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build()
                .asDistributedSet();

        packetService.addProcessor(processor, PacketProcessor.director(SFC_PRIORITY));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        vtnRscService.removeListener(vtnRscListener);
        packetService.removeProcessor(processor);
        log.info("Stopped");
    }

    /*
     * Handle events.
     */
    private class InnerVtnRscListener implements VtnRscListener {
        @Override
        public void event(VtnRscEvent event) {

            if (VtnRscEvent.Type.PORT_PAIR_PUT == event.type()) {
                PortPair portPair = ((VtnRscEventFeedback) event.subject()).portPair();
                onPortPairCreated(portPair);
            } else if (VtnRscEvent.Type.PORT_PAIR_DELETE == event.type()) {
                PortPair portPair = ((VtnRscEventFeedback) event.subject()).portPair();
                onPortPairDeleted(portPair);
            } else if (VtnRscEvent.Type.PORT_PAIR_UPDATE == event.type()) {
                PortPair portPair = ((VtnRscEventFeedback) event.subject()).portPair();
                onPortPairDeleted(portPair);
                onPortPairCreated(portPair);
            } else if (VtnRscEvent.Type.PORT_PAIR_GROUP_PUT == event.type()) {
                PortPairGroup portPairGroup = ((VtnRscEventFeedback) event.subject()).portPairGroup();
                onPortPairGroupCreated(portPairGroup);
            } else if (VtnRscEvent.Type.PORT_PAIR_GROUP_DELETE == event.type()) {
                PortPairGroup portPairGroup = ((VtnRscEventFeedback) event.subject()).portPairGroup();
                onPortPairGroupDeleted(portPairGroup);
            } else if (VtnRscEvent.Type.PORT_PAIR_GROUP_UPDATE == event.type()) {
                PortPairGroup portPairGroup = ((VtnRscEventFeedback) event.subject()).portPairGroup();
                onPortPairGroupDeleted(portPairGroup);
                onPortPairGroupCreated(portPairGroup);
            } else if (VtnRscEvent.Type.FLOW_CLASSIFIER_PUT == event.type()) {
                FlowClassifier flowClassifier = ((VtnRscEventFeedback) event.subject()).flowClassifier();
                onFlowClassifierCreated(flowClassifier);
            } else if (VtnRscEvent.Type.FLOW_CLASSIFIER_DELETE == event.type()) {
                FlowClassifier flowClassifier = ((VtnRscEventFeedback) event.subject()).flowClassifier();
                onFlowClassifierDeleted(flowClassifier);
            } else if (VtnRscEvent.Type.FLOW_CLASSIFIER_UPDATE == event.type()) {
                FlowClassifier flowClassifier = ((VtnRscEventFeedback) event.subject()).flowClassifier();
                onFlowClassifierDeleted(flowClassifier);
                onFlowClassifierCreated(flowClassifier);
            } else if (VtnRscEvent.Type.PORT_CHAIN_PUT == event.type()) {
                PortChain portChain = (PortChain) ((VtnRscEventFeedback) event.subject()).portChain();
                onPortChainCreated(portChain);
            } else if (VtnRscEvent.Type.PORT_CHAIN_DELETE == event.type()) {
                PortChain portChain = (PortChain) ((VtnRscEventFeedback) event.subject()).portChain();
                onPortChainDeleted(portChain);
            } else if (VtnRscEvent.Type.PORT_CHAIN_UPDATE == event.type()) {
                PortChain portChain = (PortChain) ((VtnRscEventFeedback) event.subject()).portChain();
                onPortChainDeleted(portChain);
                onPortChainCreated(portChain);
            }
        }
    }

    @Override
    public void onPortPairCreated(PortPair portPair) {
        log.debug("onPortPairCreated");
        // TODO: Modify forwarding rule on port-pair creation.
    }

    @Override
    public void onPortPairDeleted(PortPair portPair) {
        log.debug("onPortPairDeleted");
        // TODO: Modify forwarding rule on port-pair deletion.
    }

    @Override
    public void onPortPairGroupCreated(PortPairGroup portPairGroup) {
        log.debug("onPortPairGroupCreated");
        // TODO: Modify forwarding rule on port-pair-group creation.
    }

    @Override
    public void onPortPairGroupDeleted(PortPairGroup portPairGroup) {
        log.debug("onPortPairGroupDeleted");
        // TODO: Modify forwarding rule on port-pair-group deletion.
    }

    @Override
    public void onFlowClassifierCreated(FlowClassifier flowClassifier) {
        log.debug("onFlowClassifierCreated");
        // TODO: Modify forwarding rule on flow-classifier creation.
    }

    @Override
    public void onFlowClassifierDeleted(FlowClassifier flowClassifier) {
        log.debug("onFlowClassifierDeleted");
        // TODO: Modify forwarding rule on flow-classifier deletion.
    }

    @Override
    public void onPortChainCreated(PortChain portChain) {
        NshServicePathId nshSpi;
        log.info("onPortChainCreated");
        if (nshSpiPortChainMap.containsKey(portChain.portChainId())) {
            nshSpi = NshServicePathId.of(nshSpiPortChainMap.get(portChain.portChainId()));
        } else {
            int id = getNextNshSpi();
            if (id > MAX_NSH_SPI_ID) {
                log.error("Reached max limit of service path index."
                        + "Failed to install SFC for port chain {}", portChain.portChainId().toString());
                return;
            }
            nshSpi = NshServicePathId.of(id);
            nshSpiPortChainMap.put(portChain.portChainId(), new Integer(id));
        }

        // Install classifier rule to send the packet to controller
        flowClassifierInstaller.installFlowClassifier(portChain, nshSpi);
    }

    @Override
    public void onPortChainDeleted(PortChain portChain) {
        log.info("onPortChainDeleted");
        if (!nshSpiPortChainMap.containsKey(portChain.portChainId())) {
            throw new ItemNotFoundException("Unable to find NSH SPI");
        }

        int nshSpiId = nshSpiPortChainMap.get(portChain.portChainId());
        // Uninstall classifier rules
        flowClassifierInstaller.unInstallFlowClassifier(portChain, NshServicePathId.of(nshSpiId));
        // remove from nshSpiPortChainMap and add to nshSpiIdFreeList
        nshSpiPortChainMap.remove(portChain.portChainId());
        nshSpiIdFreeList.add(nshSpiId);

        // Uninstall load balanced classifier and forwarding rules.
        NshServicePathId nshSpi;
        LoadBalanceId id;
        List<LoadBalanceId> processedIdList = Lists.newArrayList();
        Set<FiveTuple> fiveTupleSet = portChain.getLoadBalanceIdMapKeys();
        for (FiveTuple fiveTuple : fiveTupleSet) {
            id = portChain.getLoadBalanceId(fiveTuple);
            if (processedIdList.contains(id)) {
                // multiple five tuple can have single path.
                continue;
            } else {
                processedIdList.add(id);
            }
            nshSpi = NshServicePathId.of(getNshServicePathId(id, nshSpiId));
            flowClassifierInstaller.unInstallLoadBalancedFlowClassifier(portChain, fiveTuple, nshSpi);
            serviceFunctionForwarder.unInstallLoadBalancedForwardingRule(portChain.getLoadBalancePath(fiveTuple),
                                                                         nshSpi);
        }
    }

    /**
     * Get next nsh service path identifier.
     *
     * @return value of service path identifier
     */
    int getNextNshSpi() {
        // If there is any free id use it. Otherwise generate new id.
        if (nshSpiIdFreeList.isEmpty()) {
            return (int) nshSpiIdGenerator.getNewId();
        }
        Iterator<Integer> it = nshSpiIdFreeList.iterator();
        Integer value = it.next();
        nshSpiIdFreeList.remove(value);
        return value;
    }

    private class SfcPacketProcessor implements PacketProcessor {

        /**
         * Check for given ip match with the fixed ips for the virtual port.
         *
         * @param vPortId virtual port id
         * @param ip ip address to match
         * @return true if the ip match with the fixed ips in virtual port false otherwise
         */
        private boolean checkIpInVirtualPort(VirtualPortId vPortId, IpAddress ip) {
            boolean found = false;
            Set<FixedIp> ips = virtualPortService.getPort(vPortId).fixedIps();
            for (FixedIp fixedIp : ips) {
                if (fixedIp.ip().equals(ip)) {
                    found = true;
                    break;
                }
            }
            return found;
        }

        /**
         * Find the port chain for the received packet.
         *
         * @param fiveTuple five tuple info from the packet
         * @return portChainId id of port chain
         */
        private PortChainId findPortChainFromFiveTuple(FiveTuple fiveTuple) {

            PortChainId portChainId = null;

            Iterable<PortChain> portChains = portChainService.getPortChains();
            if (portChains == null) {
                log.error("Could not retrive port chain list");
            }

            // Identify the port chain to which the packet belongs
            for (final PortChain portChain : portChains) {

                if (!portChain.tenantId().equals(fiveTuple.tenantId())) {
                    continue;
                }

                Iterable<FlowClassifierId> flowClassifiers = portChain.flowClassifiers();

                // One port chain can have multiple flow classifiers.
                for (final FlowClassifierId flowClassifierId : flowClassifiers) {

                    FlowClassifier flowClassifier = flowClassifierService.getFlowClassifier(flowClassifierId);
                    boolean match = false;
                    // Check whether protocol is set in flow classifier
                    if (flowClassifier.protocol() != null) {
                        if ((flowClassifier.protocol().equals("TCP") && fiveTuple.protocol() == IPv4.PROTOCOL_TCP) ||
                                (flowClassifier.protocol().equals("UDP") &&
                                        fiveTuple.protocol() == IPv4.PROTOCOL_UDP)) {
                            match = true;
                        } else {
                            continue;
                        }
                    }

                    // Check whether source ip prefix is set in flow classifier
                    if (flowClassifier.srcIpPrefix() != null) {
                        if (flowClassifier.srcIpPrefix().contains(fiveTuple.ipSrc())) {
                            match = true;
                        } else {
                            continue;
                        }
                    }

                    // Check whether destination ip prefix is set in flow classifier
                    if (flowClassifier.dstIpPrefix() != null) {
                        if (flowClassifier.dstIpPrefix().contains(fiveTuple.ipDst())) {
                            match = true;
                        } else {
                            continue;
                        }
                    }

                    // Check whether source port is set in flow classifier
                    if (fiveTuple.portSrc().toLong() >= flowClassifier.minSrcPortRange() ||
                            fiveTuple.portSrc().toLong() <= flowClassifier.maxSrcPortRange()) {
                        match = true;
                    } else {
                        continue;
                    }

                    // Check whether destination port is set in flow classifier
                    if (fiveTuple.portDst().toLong() >= flowClassifier.minSrcPortRange() ||
                            fiveTuple.portDst().toLong() <= flowClassifier.maxSrcPortRange()) {
                        match = true;
                    } else {
                        continue;
                    }

                    // Check whether neutron source port is set in flow classfier
                    if ((flowClassifier.srcPort() != null) && (!flowClassifier.srcPort().portId().isEmpty())) {
                        match = checkIpInVirtualPort(VirtualPortId.portId(flowClassifier.srcPort().portId()),
                                                     fiveTuple.ipSrc());
                        if (!match) {
                            continue;
                        }
                    }

                    // Check whether destination neutron destination port is set in flow classifier
                    if ((flowClassifier.dstPort() != null) && (!flowClassifier.dstPort().portId().isEmpty())) {
                        match = checkIpInVirtualPort(VirtualPortId.portId(flowClassifier.dstPort().portId()),
                                                     fiveTuple.ipDst());
                        if (!match) {
                            continue;
                        }
                    }

                    if (match) {
                        portChainId = portChain.portChainId();
                        break;
                    }
                }
            }
            return portChainId;
        }

        /**
         * Find the load balanced path set it to port chain for the given five tuple.
         *
         * @param portChainId port chain id
         * @param fiveTuple five tuple info
         * @return load balance id
         */
        private LoadBalanceId loadBalanceSfc(PortChainId portChainId, FiveTuple fiveTuple) {

            // Get the port chain
            PortChain portChain = portChainService.getPortChain(portChainId);
            List<PortPairId> loadBalancePath = Lists.newArrayList();
            LoadBalanceId id;
            int paths = portChain.getLoadBalancePathSize();
            if (paths >= MAX_LOAD_BALANCE_ID) {
                log.info("Max limit reached for load balance paths. "
                        + "Reusing the created path for port chain {} with five tuple {}",
                        portChainId, fiveTuple);
                id = LoadBalanceId.of((byte) ((paths + 1) % MAX_LOAD_BALANCE_ID));
                portChain.addLoadBalancePath(fiveTuple, id, portChain.getLoadBalancePath(id));
            }

            // Get the list of port pair groups from port chain
            Iterable<PortPairGroupId> portPairGroups = portChain.portPairGroups();
            for (final PortPairGroupId portPairGroupId : portPairGroups) {
                PortPairGroup portPairGroup = portPairGroupService.getPortPairGroup(portPairGroupId);

                // Get the list of port pair ids from port pair group.
                Iterable<PortPairId> portPairs = portPairGroup.portPairs();
                int minLoad = 0xFFF;
                PortPairId minLoadPortPairId = null;
                for (final PortPairId portPairId : portPairs) {
                    int load = portPairGroup.getLoad(portPairId);
                    if (load == 0) {
                        minLoadPortPairId = portPairId;
                        break;
                    } else {
                        // Check the port pair which has min load.
                        if (load < minLoad) {
                            minLoad = load;
                            minLoadPortPairId = portPairId;
                        }
                    }
                }
                if (minLoadPortPairId != null) {
                    loadBalancePath.add(minLoadPortPairId);
                    portPairGroup.addLoad(minLoadPortPairId);
                }
            }

            // Check if the path already exists, if not create a new id
            Optional<LoadBalanceId> output = portChain.matchPath(loadBalancePath);
            if (output.isPresent()) {
                id = output.get();
            } else {
                id = LoadBalanceId.of((byte) (paths + 1));
            }

            portChain.addLoadBalancePath(fiveTuple, id, loadBalancePath);
            return id;
        }

        /**
         * Get the tenant id for the given mac address.
         *
         * @param mac mac address
         * @return tenantId tenant id for the given mac address
         */
        private TenantId getTenantId(MacAddress mac) {
            Collection<VirtualPort> virtualPorts = virtualPortService.getPorts();
            for (VirtualPort virtualPort : virtualPorts) {
                if (virtualPort.macAddress().equals(mac)) {
                    return virtualPort.tenantId();
                }
            }
            return null;
        }

        @Override
        public void process(PacketContext context) {
            Ethernet packet = context.inPacket().parsed();
            if (packet == null || portChainService.getPortChainCount() == 0) {
                return;
            }
            // get the five tuple parameters for the packet
            short ethType = packet.getEtherType();
            IpAddress ipSrc = null;
            IpAddress ipDst = null;
            int portSrc = 0;
            int portDst = 0;
            byte protocol = 0;
            MacAddress macSrc = packet.getSourceMAC();
            TenantId tenantId = getTenantId(macSrc);

            if (ethType == Ethernet.TYPE_IPV4) {
                IPv4 ipv4Packet = (IPv4) packet.getPayload();
                ipSrc = IpAddress.valueOf(ipv4Packet.getSourceAddress());
                ipDst = IpAddress.valueOf(ipv4Packet.getDestinationAddress());
                protocol = ipv4Packet.getProtocol();
                if (protocol == IPv4.PROTOCOL_TCP) {
                    TCP tcpPacket = (TCP) ipv4Packet.getPayload();
                    portSrc = tcpPacket.getSourcePort();
                    portDst = tcpPacket.getDestinationPort();
                } else  if (protocol == IPv4.PROTOCOL_UDP) {
                    UDP udpPacket = (UDP) ipv4Packet.getPayload();
                    portSrc = udpPacket.getSourcePort();
                    portDst = udpPacket.getDestinationPort();
                } else if (protocol == IPv4.PROTOCOL_ICMP) {
                    // do nothing
                } else {
                    // No need to process other packets received by controller.
                    return;
                }
            } else if (ethType == Ethernet.TYPE_IPV6) {
                return;
            }

            FiveTuple fiveTuple = DefaultFiveTuple.builder()
                    .setIpSrc(ipSrc)
                    .setIpDst(ipDst)
                    .setPortSrc(PortNumber.portNumber(portSrc))
                    .setPortDst(PortNumber.portNumber(portDst))
                    .setProtocol(protocol)
                    .setTenantId(tenantId)
                    .build();

            PortChainId portChainId = findPortChainFromFiveTuple(fiveTuple);

            if (portChainId == null) {
                log.error("Packet does not match with any classifier");
                return;
            }

            // Once the 5 tuple and port chain are identified, give this input for load balancing
            LoadBalanceId id = loadBalanceSfc(portChainId, fiveTuple);
            // Get nsh service path index
            NshServicePathId nshSpi;
            PortChain portChain = portChainService.getPortChain(portChainId);
            if (nshSpiPortChainMap.containsKey(portChain.portChainId())) {
                int nshSpiId = nshSpiPortChainMap.get(portChain.portChainId());
                nshSpi = NshServicePathId.of(getNshServicePathId(id, nshSpiId));
            } else {
                int nshSpiId = getNextNshSpi();
                if (nshSpiId > MAX_NSH_SPI_ID) {
                    log.error("Reached max limit of service path index."
                            + "Failed to install SFC for port chain {}", portChain.portChainId());
                    return;
                }
                nshSpi = NshServicePathId.of(getNshServicePathId(id, nshSpiId));
                nshSpiPortChainMap.put(portChain.portChainId(), new Integer(nshSpiId));
            }
            // download the required flow rules for classifier and forwarding
            // install in OVS.
            ConnectPoint connectPoint = flowClassifierInstaller.installLoadBalancedFlowClassifier(portChain,
                                                                                                  fiveTuple, nshSpi);
            serviceFunctionForwarder.installLoadBalancedForwardingRule(portChain.getLoadBalancePath(fiveTuple),
                                                                       nshSpi);
            sendPacket(context, connectPoint);
        }

        /**
         * Send packet back to classifier.
         *
         * @param context packet context
         * @param connectPoint connect point of first service function
         */
        private void sendPacket(PacketContext context, ConnectPoint connectPoint) {

            TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(connectPoint.port()).build();
            OutboundPacket packet = new DefaultOutboundPacket(connectPoint.deviceId(), treatment,
                                                              context.inPacket().unparsed());
            packetService.emit(packet);
            log.trace("Sending packet: {}", packet);
        }
    }

    /**
     * Encapsulate 5 bit load balance id to nsh spi.
     *
     * @param id load balance identifier
     * @param nshSpiId nsh service path index
     * @return updated service path index
     */
    protected int getNshServicePathId(LoadBalanceId id, int nshSpiId) {
        int nshSpiNew = nshSpiId << 5;
        nshSpiNew = nshSpiNew | id.loadBalanceId();
        return nshSpiNew;
    }
}
