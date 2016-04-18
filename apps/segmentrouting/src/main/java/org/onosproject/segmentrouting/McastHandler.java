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

package org.onosproject.segmentrouting;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.McastConfig;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.McastRouteInfo;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.segmentrouting.storekey.McastNextObjectiveStoreKey;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Handles multicast-related events.
 */
public class McastHandler {
    private static final Logger log = LoggerFactory.getLogger(McastHandler.class);
    private final SegmentRoutingManager srManager;
    private final ApplicationId coreAppId;
    private StorageService storageService;
    private TopologyService topologyService;
    private final KryoNamespace.Builder kryoBuilder;
    private final ConsistentMap<McastNextObjectiveStoreKey, NextObjective> mcastNextObjStore;

    /**
     * Constructs the McastEventHandler.
     *
     * @param srManager Segment Routing manager
     */
    public McastHandler(SegmentRoutingManager srManager) {
        coreAppId = srManager.coreService.getAppId(CoreService.CORE_APP_NAME);

        this.srManager = srManager;
        this.storageService = srManager.storageService;
        this.topologyService = srManager.topologyService;

        kryoBuilder = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(McastNextObjectiveStoreKey.class);
        mcastNextObjStore = storageService
                .<McastNextObjectiveStoreKey, NextObjective>consistentMapBuilder()
                .withName("onos-mcast-nextobj-store")
                .withSerializer(Serializer.using(kryoBuilder.build()))
                .build();
    }

    /**
     * Processes the SOURCE_ADDED event.
     *
     * @param event McastEvent with SOURCE_ADDED type
     */
    protected void processSourceAdded(McastEvent event) {
        log.info("processSourceAdded {}", event);
        McastRouteInfo mcastRouteInfo = event.subject();
        if (!mcastRouteInfo.isComplete()) {
            log.info("Incompleted McastRouteInfo. Abort.");
            return;
        }
        ConnectPoint source = mcastRouteInfo.source().orElse(null);
        Set<ConnectPoint> sinks = mcastRouteInfo.sinks();
        IpAddress mcastIp = mcastRouteInfo.route().group();

        sinks.forEach(sink -> {
            processSinkAddedInternal(source, sink, mcastIp);
        });
    }

    /**
     * Processes the SINK_ADDED event.
     *
     * @param event McastEvent with SINK_ADDED type
     */
    protected void processSinkAdded(McastEvent event) {
        log.info("processSinkAdded {}", event);
        McastRouteInfo mcastRouteInfo = event.subject();
        if (!mcastRouteInfo.isComplete()) {
            log.info("Incompleted McastRouteInfo. Abort.");
            return;
        }
        ConnectPoint source = mcastRouteInfo.source().orElse(null);
        ConnectPoint sink = mcastRouteInfo.sink().orElse(null);
        IpAddress mcastIp = mcastRouteInfo.route().group();

        processSinkAddedInternal(source, sink, mcastIp);
    }

    /**
     * Processes the SINK_REMOVED event.
     *
     * @param event McastEvent with SINK_REMOVED type
     */
    protected void processSinkRemoved(McastEvent event) {
        log.info("processSinkRemoved {}", event);
        McastRouteInfo mcastRouteInfo = event.subject();
        if (!mcastRouteInfo.isComplete()) {
            log.info("Incompleted McastRouteInfo. Abort.");
            return;
        }
        ConnectPoint source = mcastRouteInfo.source().orElse(null);
        ConnectPoint sink = mcastRouteInfo.sink().orElse(null);
        IpAddress mcastIp = mcastRouteInfo.route().group();
        VlanId assignedVlan = assignedVlan();

        // When source and sink are on the same device
        if (source.deviceId().equals(sink.deviceId())) {
            // Source and sink are on even the same port. There must be something wrong.
            if (source.port().equals(sink.port())) {
                log.warn("Sink is on the same port of source. Abort");
                return;
            }
            removePortFromDevice(sink.deviceId(), sink.port(), mcastIp, assignedVlan);
            return;
        }

        // Process the egress device
        boolean isLast = removePortFromDevice(sink.deviceId(), sink.port(), mcastIp, assignedVlan);

        // If this is the last sink on the device, also update upstream
        Optional<Path> mcastPath = getPath(source.deviceId(), sink.deviceId(), mcastIp);
        if (mcastPath.isPresent()) {
            List<Link> links = Lists.newArrayList(mcastPath.get().links());
            Collections.reverse(links);
            for (Link link : links) {
                if (isLast) {
                    isLast = removePortFromDevice(link.src().deviceId(), link.src().port(),
                            mcastIp, assignedVlan);
                }
            }
        }
    }

    /**
     * Establishes a path from source to sink for given multicast group.
     *
     * @param source connect point of the multicast source
     * @param sink connection point of the multicast sink
     * @param mcastIp multicast group IP address
     */
    private void processSinkAddedInternal(ConnectPoint source, ConnectPoint sink,
            IpAddress mcastIp) {
        VlanId assignedVlan = assignedVlan();

        // When source and sink are on the same device
        if (source.deviceId().equals(sink.deviceId())) {
            // Source and sink are on even the same port. There must be something wrong.
            if (source.port().equals(sink.port())) {
                log.warn("Sink is on the same port of source. Abort");
                return;
            }
            addPortToDevice(sink.deviceId(), sink.port(), mcastIp, assignedVlan);
            return;
        }

        // Process the ingress device
        addFilterToDevice(source.deviceId(), source.port(), assignedVlan);

        // Find a path. If present, create/update groups and flows for each hop
        Optional<Path> mcastPath = getPath(source.deviceId(), sink.deviceId(), mcastIp);
        if (mcastPath.isPresent()) {
            mcastPath.get().links().forEach(link -> {
                addFilterToDevice(link.dst().deviceId(), link.dst().port(), assignedVlan);
                addPortToDevice(link.src().deviceId(), link.src().port(), mcastIp, assignedVlan);
            });
            // Process the egress device
            addPortToDevice(sink.deviceId(), sink.port(), mcastIp, assignedVlan);
        }
    }

    /**
     * Adds filtering objective for given device and port.
     *
     * @param deviceId device ID
     * @param port ingress port number
     * @param assignedVlan assigned VLAN ID
     */
    private void addFilterToDevice(DeviceId deviceId, PortNumber port, VlanId assignedVlan) {
        // Do nothing if the port is configured as suppressed
        ConnectPoint connectPt = new ConnectPoint(deviceId, port);
        if (srManager.deviceConfiguration.suppressSubnet().contains(connectPt) ||
                srManager.deviceConfiguration.suppressHost().contains(connectPt)) {
            log.info("Ignore suppressed port {}", connectPt);
            return;
        }

        FilteringObjective.Builder filtObjBuilder =
                filterObjBuilder(deviceId, port, assignedVlan);
        srManager.flowObjectiveService.filter(deviceId, filtObjBuilder.add());
        // TODO add objective context
    }

    /**
     * Adds a port to given multicast group on given device. This involves the
     * update of L3 multicast group and multicast routing table entry.
     *
     * @param deviceId device ID
     * @param port port to be added
     * @param mcastIp multicast group
     * @param assignedVlan assigned VLAN ID
     */
    private void addPortToDevice(DeviceId deviceId, PortNumber port,
            IpAddress mcastIp, VlanId assignedVlan) {
        log.info("Add port {} to {}. mcastIp={}, assignedVlan={}",
                port, deviceId, mcastIp, assignedVlan);
        McastNextObjectiveStoreKey mcastNextObjectiveStoreKey =
                new McastNextObjectiveStoreKey(mcastIp, deviceId);
        ImmutableSet.Builder<PortNumber> portBuilder = ImmutableSet.builder();
        if (!mcastNextObjStore.containsKey(mcastNextObjectiveStoreKey)) {
            // First time someone request this mcast group via this device
            portBuilder.add(port);
        } else {
            // This device already serves some subscribers of this mcast group
            NextObjective nextObj = mcastNextObjStore.get(mcastNextObjectiveStoreKey).value();
            // Stop if the port is already in the nextobj
            Set<PortNumber> existingPorts = getPorts(nextObj.next());
            if (existingPorts.contains(port)) {
                log.info("NextObj for {}/{} already exists. Abort", deviceId, port);
                return;
            }
            portBuilder.addAll(existingPorts).add(port).build();
        }
        // Create, store and apply the new nextObj and fwdObj
        NextObjective newNextObj =
                nextObjBuilder(mcastIp, assignedVlan, portBuilder.build()).add();
        ForwardingObjective fwdObj =
                fwdObjBuilder(mcastIp, assignedVlan, newNextObj.id()).add();
        mcastNextObjStore.put(mcastNextObjectiveStoreKey, newNextObj);
        srManager.flowObjectiveService.next(deviceId, newNextObj);
        srManager.flowObjectiveService.forward(deviceId, fwdObj);
        // TODO add objective callback
    }

    /**
     * Removes a port from given multicast group on given device.
     * This involves the update of L3 multicast group and multicast routing
     * table entry.
     *
     * @param deviceId device ID
     * @param port port to be added
     * @param mcastIp multicast group
     * @param assignedVlan assigned VLAN ID
     * @return true if this is the last sink on this device
     */
    private boolean removePortFromDevice(DeviceId deviceId, PortNumber port,
            IpAddress mcastIp, VlanId assignedVlan) {
        log.info("Remove port {} from {}. mcastIp={}, assignedVlan={}",
                port, deviceId, mcastIp, assignedVlan);
        McastNextObjectiveStoreKey mcastNextObjectiveStoreKey =
                new McastNextObjectiveStoreKey(mcastIp, deviceId);
        // This device is not serving this multicast group
        if (!mcastNextObjStore.containsKey(mcastNextObjectiveStoreKey)) {
            log.warn("{} is not serving {} on port {}. Abort.", deviceId, mcastIp, port);
            return false;
        }
        NextObjective nextObj = mcastNextObjStore.get(mcastNextObjectiveStoreKey).value();

        Set<PortNumber> existingPorts = getPorts(nextObj.next());
        // This device does not serve this multicast group
        if (!existingPorts.contains(port)) {
            log.warn("{} is not serving {} on port {}. Abort.", deviceId, mcastIp, port);
            return false;
        }
        // Copy and modify the ImmutableSet
        existingPorts = Sets.newHashSet(existingPorts);
        existingPorts.remove(port);

        NextObjective newNextObj;
        ForwardingObjective fwdObj;
        if (existingPorts.isEmpty()) {
            // If this is the last sink, remove flows and groups
            // NOTE: Rely on GroupStore garbage collection rather than explicitly
            //       remove L3MG since there might be other flows/groups refer to
            //       the same L2IG
            fwdObj = fwdObjBuilder(mcastIp, assignedVlan, nextObj.id()).remove();
            mcastNextObjStore.remove(mcastNextObjectiveStoreKey);
            srManager.flowObjectiveService.forward(deviceId, fwdObj);
        } else {
            // If this is not the last sink, update flows and groups
            newNextObj = nextObjBuilder(mcastIp, assignedVlan, existingPorts).add();
            fwdObj = fwdObjBuilder(mcastIp, assignedVlan, newNextObj.id()).add();
            mcastNextObjStore.put(mcastNextObjectiveStoreKey, newNextObj);
            srManager.flowObjectiveService.next(deviceId, newNextObj);
            srManager.flowObjectiveService.forward(deviceId, fwdObj);
        }
        // TODO add objective callback

        return existingPorts.isEmpty();
    }

    /**
     * Creates a next objective builder for multicast.
     *
     * @param mcastIp multicast group
     * @param assignedVlan assigned VLAN ID
     * @param outPorts set of output port numbers
     * @return next objective builder
     */
    private NextObjective.Builder nextObjBuilder(IpAddress mcastIp,
            VlanId assignedVlan, Set<PortNumber> outPorts) {
        int nextId = srManager.flowObjectiveService.allocateNextId();

        TrafficSelector metadata =
                DefaultTrafficSelector.builder()
                        .matchVlanId(assignedVlan)
                        .matchIPDst(mcastIp.toIpPrefix())
                        .build();

        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.BROADCAST).fromApp(srManager.appId)
                .withMeta(metadata);

        outPorts.forEach(port -> {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            if (egressVlan().equals(VlanId.NONE)) {
                tBuilder.popVlan();
            }
            tBuilder.setOutput(port);
            nextObjBuilder.addTreatment(tBuilder.build());
        });

        return nextObjBuilder;
    }

    /**
     * Creates a forwarding objective builder for multicast.
     *
     * @param mcastIp multicast group
     * @param assignedVlan assigned VLAN ID
     * @param nextId next ID of the L3 multicast group
     * @return forwarding objective builder
     */
    private ForwardingObjective.Builder fwdObjBuilder(IpAddress mcastIp,
            VlanId assignedVlan, int nextId) {
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        IpPrefix mcastPrefix = IpPrefix.valueOf(mcastIp, IpPrefix.MAX_INET_MASK_LENGTH);
        sbuilder.matchEthType(Ethernet.TYPE_IPV4);
        sbuilder.matchIPDst(mcastPrefix);
        TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder();
        metabuilder.matchVlanId(assignedVlan);

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective.builder();
        fwdBuilder.withSelector(sbuilder.build())
                .withMeta(metabuilder.build())
                .nextStep(nextId)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(srManager.appId)
                .withPriority(SegmentRoutingService.DEFAULT_PRIORITY);
        return fwdBuilder;
    }

    /**
     * Creates a filtering objective builder for multicast.
     *
     * @param deviceId Device ID
     * @param ingressPort ingress port of the multicast stream
     * @param assignedVlan assigned VLAN ID
     * @return filtering objective builder
     */
    private FilteringObjective.Builder filterObjBuilder(DeviceId deviceId, PortNumber ingressPort,
            VlanId assignedVlan) {
        FilteringObjective.Builder filtBuilder = DefaultFilteringObjective.builder();
        filtBuilder.withKey(Criteria.matchInPort(ingressPort))
                .addCondition(Criteria.matchEthDstMasked(MacAddress.IPV4_MULTICAST,
                        MacAddress.IPV4_MULTICAST_MASK))
                .addCondition(Criteria.matchVlanId(egressVlan()))
                .withPriority(SegmentRoutingService.DEFAULT_PRIORITY);
        // vlan assignment is valid only if this instance is master
        if (srManager.mastershipService.isLocalMaster(deviceId)) {
            TrafficTreatment tt = DefaultTrafficTreatment.builder()
                    .pushVlan().setVlanId(assignedVlan).build();
            filtBuilder.withMeta(tt);
        }
        return filtBuilder.permit().fromApp(srManager.appId);
    }

    /**
     * Gets output ports information from treatments.
     *
     * @param treatments collection of traffic treatments
     * @return set of output port numbers
     */
    private Set<PortNumber> getPorts(Collection<TrafficTreatment> treatments) {
        ImmutableSet.Builder<PortNumber> builder = ImmutableSet.builder();
        treatments.forEach(treatment -> {
            treatment.allInstructions().stream()
                    .filter(instr -> instr instanceof OutputInstruction)
                    .forEach(instr -> {
                        builder.add(((OutputInstruction) instr).port());
                    });
        });
        return builder.build();
    }

    /**
     * Gets a path from src to dst.
     * If a path was allocated before, returns the allocated path.
     * Otherwise, randomly pick one from available paths.
     *
     * @param src source device ID
     * @param dst destination device ID
     * @param mcastIp multicast group
     * @return an optional path from src to dst
     */
    private Optional<Path> getPath(DeviceId src, DeviceId dst, IpAddress mcastIp) {
        List<Path> allPaths = Lists.newArrayList(
                topologyService.getPaths(topologyService.currentTopology(), src, dst));
        if (allPaths.isEmpty()) {
            log.warn("Fail to find a path from {} to {}. Abort.", src, dst);
            return Optional.empty();
        }

        // If one of the available path is used before, use the same path
        McastNextObjectiveStoreKey mcastNextObjectiveStoreKey =
                new McastNextObjectiveStoreKey(mcastIp, src);
        if (mcastNextObjStore.containsKey(mcastNextObjectiveStoreKey)) {
            NextObjective nextObj = mcastNextObjStore.get(mcastNextObjectiveStoreKey).value();
            Set<PortNumber> existingPorts = getPorts(nextObj.next());
            for (Path path : allPaths) {
                PortNumber srcPort = path.links().get(0).src().port();
                if (existingPorts.contains(srcPort)) {
                    return Optional.of(path);
                }
            }
        }
        // Otherwise, randomly pick a path
        Collections.shuffle(allPaths);
        return allPaths.stream().findFirst();
    }

    /**
     * Gets egress VLAN from McastConfig.
     *
     * @return egress VLAN or VlanId.NONE if not configured
     */
    private VlanId egressVlan() {
        McastConfig mcastConfig =
                srManager.cfgService.getConfig(coreAppId, McastConfig.class);
        return (mcastConfig != null) ? mcastConfig.egressVlan() : VlanId.NONE;
    }

    /**
     * Gets assigned VLAN according to the value of egress VLAN.
     *
     * @return assigned VLAN
     */
    private VlanId assignedVlan() {
        return (egressVlan().equals(VlanId.NONE)) ?
                VlanId.vlanId(SegmentRoutingManager.ASSIGNED_VLAN_NO_SUBNET) :
                egressVlan();
    }
}
