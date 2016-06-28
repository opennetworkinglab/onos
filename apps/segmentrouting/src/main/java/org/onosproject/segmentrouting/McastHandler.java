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
import org.onlab.packet.Ip4Prefix;
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
import org.onosproject.net.flowobjective.DefaultObjectiveContext;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.McastRouteInfo;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.onosproject.segmentrouting.storekey.McastStoreKey;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * Handles multicast-related events.
 */
public class McastHandler {
    private static final Logger log = LoggerFactory.getLogger(McastHandler.class);
    private final SegmentRoutingManager srManager;
    private final ApplicationId coreAppId;
    private final StorageService storageService;
    private final TopologyService topologyService;
    private final ConsistentMap<McastStoreKey, NextObjective> mcastNextObjStore;
    private final KryoNamespace.Builder mcastKryo;
    private final ConsistentMap<McastStoreKey, McastRole> mcastRoleStore;

    /**
     * Role in the multicast tree.
     */
    public enum McastRole {
        /**
         * The device is the ingress device of this group.
         */
        INGRESS,
        /**
         * The device is the transit device of this group.
         */
        TRANSIT,
        /**
         * The device is the egress device of this group.
         */
        EGRESS
    }

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
        mcastKryo = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(McastStoreKey.class)
                .register(McastRole.class);
        mcastNextObjStore = storageService
                .<McastStoreKey, NextObjective>consistentMapBuilder()
                .withName("onos-mcast-nextobj-store")
                .withSerializer(Serializer.using(mcastKryo.build("McastHandler-NextObj")))
                .build();
        mcastRoleStore = storageService
                .<McastStoreKey, McastRole>consistentMapBuilder()
                .withName("onos-mcast-role-store")
                .withSerializer(Serializer.using(mcastKryo.build("McastHandler-Role")))
                .build();
    }

    /**
     * Read initial multicast from mcast store.
     */
    protected void init() {
        srManager.multicastRouteService.getRoutes().forEach(mcastRoute -> {
            ConnectPoint source = srManager.multicastRouteService.fetchSource(mcastRoute);
            Set<ConnectPoint> sinks = srManager.multicastRouteService.fetchSinks(mcastRoute);
            sinks.forEach(sink -> {
                processSinkAddedInternal(source, sink, mcastRoute.group());
            });
        });
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

        // Continue only when this instance is the master of source device
        if (!srManager.mastershipService.isLocalMaster(source.deviceId())) {
            log.info("Skip {} due to lack of mastership of the source device {}",
                    mcastIp, source.deviceId());
            return;
        }

        // When source and sink are on the same device
        if (source.deviceId().equals(sink.deviceId())) {
            // Source and sink are on even the same port. There must be something wrong.
            if (source.port().equals(sink.port())) {
                log.warn("Sink is on the same port of source. Abort");
                return;
            }
            removePortFromDevice(sink.deviceId(), sink.port(), mcastIp, assignedVlan(source));
            return;
        }

        // Process the egress device
        boolean isLast = removePortFromDevice(sink.deviceId(), sink.port(), mcastIp, assignedVlan(null));
        if (isLast) {
            mcastRoleStore.remove(new McastStoreKey(mcastIp, sink.deviceId()));
        }

        // If this is the last sink on the device, also update upstream
        Optional<Path> mcastPath = getPath(source.deviceId(), sink.deviceId(), mcastIp);
        if (mcastPath.isPresent()) {
            List<Link> links = Lists.newArrayList(mcastPath.get().links());
            Collections.reverse(links);
            for (Link link : links) {
                if (isLast) {
                    isLast = removePortFromDevice(link.src().deviceId(), link.src().port(),
                            mcastIp,
                            assignedVlan(link.src().deviceId().equals(source.deviceId()) ? source : null));
                    mcastRoleStore.remove(new McastStoreKey(mcastIp, link.src().deviceId()));
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
        // Continue only when this instance is the master of source device
        if (!srManager.mastershipService.isLocalMaster(source.deviceId())) {
            log.info("Skip {} due to lack of mastership of the source device {}",
                    source.deviceId());
            return;
        }

        // Process the ingress device
        addFilterToDevice(source.deviceId(), source.port(), assignedVlan(source));

        // When source and sink are on the same device
        if (source.deviceId().equals(sink.deviceId())) {
            // Source and sink are on even the same port. There must be something wrong.
            if (source.port().equals(sink.port())) {
                log.warn("Sink is on the same port of source. Abort");
                return;
            }
            addPortToDevice(sink.deviceId(), sink.port(), mcastIp, assignedVlan(source));
            mcastRoleStore.put(new McastStoreKey(mcastIp, sink.deviceId()), McastRole.INGRESS);
            return;
        }

        // Find a path. If present, create/update groups and flows for each hop
        Optional<Path> mcastPath = getPath(source.deviceId(), sink.deviceId(), mcastIp);
        if (mcastPath.isPresent()) {
            List<Link> links = mcastPath.get().links();
            checkState(links.size() == 2,
                    "Path in leaf-spine topology should always be two hops: ", links);

            links.forEach(link -> {
                addPortToDevice(link.src().deviceId(), link.src().port(), mcastIp,
                        assignedVlan(link.src().deviceId().equals(source.deviceId()) ? source : null));
                addFilterToDevice(link.dst().deviceId(), link.dst().port(), assignedVlan(null));
            });

            // Process the egress device
            addPortToDevice(sink.deviceId(), sink.port(), mcastIp, assignedVlan(null));

            // Setup mcast roles
            mcastRoleStore.put(new McastStoreKey(mcastIp, source.deviceId()),
                    McastRole.INGRESS);
            mcastRoleStore.put(new McastStoreKey(mcastIp, links.get(0).dst().deviceId()),
                    McastRole.TRANSIT);
            mcastRoleStore.put(new McastStoreKey(mcastIp, sink.deviceId()),
                    McastRole.EGRESS);
        } else {
            log.warn("Unable to find a path from {} to {}. Abort sinkAdded",
                    source.deviceId(), sink.deviceId());
        }
    }

    /**
     * Processes the LINK_DOWN event.
     *
     * @param affectedLink Link that is going down
     */
    protected void processLinkDown(Link affectedLink) {
        getAffectedGroups(affectedLink).forEach(mcastIp -> {
            // Find out the ingress, transit and egress device of affected group
            DeviceId ingressDevice = getDevice(mcastIp, McastRole.INGRESS)
                    .stream().findAny().orElse(null);
            DeviceId transitDevice = getDevice(mcastIp, McastRole.TRANSIT)
                    .stream().findAny().orElse(null);
            Set<DeviceId> egressDevices = getDevice(mcastIp, McastRole.EGRESS);
            ConnectPoint source = getSource(mcastIp);

            // Do not proceed if any of these info is missing
            if (ingressDevice == null || transitDevice == null
                    || egressDevices == null || source == null) {
                log.warn("Missing ingress {}, transit {}, egress {} devices or source {}",
                        ingressDevice, transitDevice, egressDevices, source);
                return;
            }

            // Continue only when this instance is the master of source device
            if (!srManager.mastershipService.isLocalMaster(source.deviceId())) {
                log.info("Skip {} due to lack of mastership of the source device {}",
                        source.deviceId());
                return;
            }

            // Remove entire transit
            removeGroupFromDevice(transitDevice, mcastIp, assignedVlan(null));

            // Remove transit-facing port on ingress device
            PortNumber ingressTransitPort = ingressTransitPort(mcastIp);
            if (ingressTransitPort != null) {
                removePortFromDevice(ingressDevice, ingressTransitPort, mcastIp, assignedVlan(source));
                mcastRoleStore.remove(new McastStoreKey(mcastIp, transitDevice));
            }

            // Construct a new path for each egress device
            egressDevices.forEach(egressDevice -> {
                Optional<Path> mcastPath = getPath(ingressDevice, egressDevice, mcastIp);
                if (mcastPath.isPresent()) {
                    List<Link> links = mcastPath.get().links();
                    links.forEach(link -> {
                        addPortToDevice(link.src().deviceId(), link.src().port(), mcastIp,
                                assignedVlan(link.src().deviceId().equals(source.deviceId()) ? source : null));
                        addFilterToDevice(link.dst().deviceId(), link.dst().port(), assignedVlan(null));
                    });
                    // Setup new transit mcast role
                    mcastRoleStore.put(new McastStoreKey(mcastIp,
                            links.get(0).dst().deviceId()), McastRole.TRANSIT);
                } else {
                    log.warn("Fail to recover egress device {} from link failure {}",
                            egressDevice, affectedLink);
                    removeGroupFromDevice(egressDevice, mcastIp, assignedVlan(null));
                }
            });
        });
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
        ConnectPoint connectPoint = new ConnectPoint(deviceId, port);
        SegmentRoutingAppConfig appConfig = srManager.cfgService
                .getConfig(srManager.appId, SegmentRoutingAppConfig.class);
        if (appConfig != null && appConfig.suppressSubnet().contains(connectPoint)) {
            log.info("Ignore suppressed port {}", connectPoint);
            return;
        }

        // Reuse unicast VLAN if the port has subnet configured
        Ip4Prefix portSubnet = srManager.deviceConfiguration.getPortSubnet(deviceId, port);
        VlanId unicastVlan = srManager.getSubnetAssignedVlanId(deviceId, portSubnet);
        final VlanId finalVlanId = (unicastVlan != null) ? unicastVlan : assignedVlan;

        FilteringObjective.Builder filtObjBuilder =
                filterObjBuilder(deviceId, port, finalVlanId);
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully add filter on {}/{}, vlan {}",
                        deviceId, port.toLong(), finalVlanId),
                (objective, error) ->
                        log.warn("Failed to add filter on {}/{}, vlan {}: {}",
                                deviceId, port.toLong(), finalVlanId, error));
        srManager.flowObjectiveService.filter(deviceId, filtObjBuilder.add(context));
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
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, deviceId);
        ImmutableSet.Builder<PortNumber> portBuilder = ImmutableSet.builder();
        if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
            // First time someone request this mcast group via this device
            portBuilder.add(port);
        } else {
            // This device already serves some subscribers of this mcast group
            NextObjective nextObj = mcastNextObjStore.get(mcastStoreKey).value();
            // Stop if the port is already in the nextobj
            Set<PortNumber> existingPorts = getPorts(nextObj.next());
            if (existingPorts.contains(port)) {
                log.info("NextObj for {}/{} already exists. Abort", deviceId, port);
                return;
            }
            portBuilder.addAll(existingPorts).add(port).build();
        }
        // Create, store and apply the new nextObj and fwdObj
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully add {} on {}/{}, vlan {}",
                        mcastIp, deviceId, port.toLong(), assignedVlan),
                (objective, error) ->
                        log.warn("Failed to add {} on {}/{}, vlan {}: {}",
                                mcastIp, deviceId, port.toLong(), assignedVlan, error));
        NextObjective newNextObj =
                nextObjBuilder(mcastIp, assignedVlan, portBuilder.build()).add();
        ForwardingObjective fwdObj =
                fwdObjBuilder(mcastIp, assignedVlan, newNextObj.id()).add(context);
        mcastNextObjStore.put(mcastStoreKey, newNextObj);
        srManager.flowObjectiveService.next(deviceId, newNextObj);
        srManager.flowObjectiveService.forward(deviceId, fwdObj);
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
        McastStoreKey mcastStoreKey =
                new McastStoreKey(mcastIp, deviceId);
        // This device is not serving this multicast group
        if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
            log.warn("{} is not serving {} on port {}. Abort.", deviceId, mcastIp, port);
            return false;
        }
        NextObjective nextObj = mcastNextObjStore.get(mcastStoreKey).value();

        Set<PortNumber> existingPorts = getPorts(nextObj.next());
        // This port does not serve this multicast group
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
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Successfully remove {} on {}/{}, vlan {}",
                            mcastIp, deviceId, port.toLong(), assignedVlan),
                    (objective, error) ->
                            log.warn("Failed to remove {} on {}/{}, vlan {}: {}",
                                    mcastIp, deviceId, port.toLong(), assignedVlan, error));
            fwdObj = fwdObjBuilder(mcastIp, assignedVlan, nextObj.id()).remove(context);
            mcastNextObjStore.remove(mcastStoreKey);
            srManager.flowObjectiveService.forward(deviceId, fwdObj);
        } else {
            // If this is not the last sink, update flows and groups
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Successfully update {} on {}/{}, vlan {}",
                            mcastIp, deviceId, port.toLong(), assignedVlan),
                    (objective, error) ->
                            log.warn("Failed to update {} on {}/{}, vlan {}: {}",
                                    mcastIp, deviceId, port.toLong(), assignedVlan, error));
            newNextObj = nextObjBuilder(mcastIp, assignedVlan, existingPorts).add();
            fwdObj = fwdObjBuilder(mcastIp, assignedVlan, newNextObj.id()).add(context);
            mcastNextObjStore.put(mcastStoreKey, newNextObj);
            srManager.flowObjectiveService.next(deviceId, newNextObj);
            srManager.flowObjectiveService.forward(deviceId, fwdObj);
        }
        return existingPorts.isEmpty();
    }


    /**
     * Removes entire group on given device.
     *
     * @param deviceId device ID
     * @param mcastIp multicast group to be removed
     * @param assignedVlan assigned VLAN ID
     */
    private void removeGroupFromDevice(DeviceId deviceId, IpAddress mcastIp,
            VlanId assignedVlan) {
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, deviceId);
        // This device is not serving this multicast group
        if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
            log.warn("{} is not serving {}. Abort.", deviceId, mcastIp);
            return;
        }
        NextObjective nextObj = mcastNextObjStore.get(mcastStoreKey).value();
        // NOTE: Rely on GroupStore garbage collection rather than explicitly
        //       remove L3MG since there might be other flows/groups refer to
        //       the same L2IG
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully remove {} on {}, vlan {}",
                        mcastIp, deviceId, assignedVlan),
                (objective, error) ->
                        log.warn("Failed to remove {} on {}, vlan {}: {}",
                                mcastIp, deviceId, assignedVlan, error));
        ForwardingObjective fwdObj = fwdObjBuilder(mcastIp, assignedVlan, nextObj.id()).remove(context);
        srManager.flowObjectiveService.forward(deviceId, fwdObj);
        mcastNextObjStore.remove(mcastStoreKey);
        mcastRoleStore.remove(mcastStoreKey);
    }

    /**
     * Remove all groups on given device.
     *
     * @param deviceId device ID
     */
    public void removeDevice(DeviceId deviceId) {
        mcastNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                .forEach(entry -> {
                    ConnectPoint source = getSource(entry.getKey().mcastIp());
                    removeGroupFromDevice(entry.getKey().deviceId(), entry.getKey().mcastIp(),
                            assignedVlan(deviceId.equals(source.deviceId()) ? source : null));
                    mcastNextObjStore.remove(entry.getKey());
                });
        log.debug("{} is removed from mcastNextObjStore", deviceId);

        mcastRoleStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                .forEach(entry -> {
                    mcastRoleStore.remove(entry.getKey());
                });
        log.debug("{} is removed from mcastRoleStore", deviceId);
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

        TrafficTreatment tt = DefaultTrafficTreatment.builder()
                .pushVlan().setVlanId(assignedVlan).build();
        filtBuilder.withMeta(tt);

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
        log.debug("{} path(s) found from {} to {}", allPaths.size(), src, dst);
        if (allPaths.isEmpty()) {
            return Optional.empty();
        }

        // If one of the available path is used before, use the same path
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, src);
        if (mcastNextObjStore.containsKey(mcastStoreKey)) {
            NextObjective nextObj = mcastNextObjStore.get(mcastStoreKey).value();
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
     * Gets device(s) of given role in given multicast group.
     *
     * @param mcastIp multicast IP
     * @param role multicast role
     * @return set of device ID or empty set if not found
     */
    private Set<DeviceId> getDevice(IpAddress mcastIp, McastRole role) {
        return mcastRoleStore.entrySet().stream()
                .filter(entry -> entry.getKey().mcastIp().equals(mcastIp) &&
                        entry.getValue().value() == role)
                .map(Map.Entry::getKey).map(McastStoreKey::deviceId)
                .collect(Collectors.toSet());
    }

    /**
     * Gets source connect point of given multicast group.
     *
     * @param mcastIp multicast IP
     * @return source connect point or null if not found
     */
    private ConnectPoint getSource(IpAddress mcastIp) {
        return srManager.multicastRouteService.getRoutes().stream()
                .filter(mcastRoute -> mcastRoute.group().equals(mcastIp))
                .map(mcastRoute -> srManager.multicastRouteService.fetchSource(mcastRoute))
                .findAny().orElse(null);
    }

    /**
     * Gets groups which is affected by the link down event.
     *
     * @param link link going down
     * @return a set of multicast IpAddress
     */
    private Set<IpAddress> getAffectedGroups(Link link) {
        DeviceId deviceId = link.src().deviceId();
        PortNumber port = link.src().port();
        return mcastNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId) &&
                        getPorts(entry.getValue().value().next()).contains(port))
                .map(Map.Entry::getKey).map(McastStoreKey::mcastIp)
                .collect(Collectors.toSet());
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
     * If connect point is specified, try to reuse the assigned VLAN on the connect point.
     *
     * @param cp connect point; Can be null if not specified
     * @return assigned VLAN ID
     */
    private VlanId assignedVlan(ConnectPoint cp) {
        // Use the egressVlan if it is tagged
        if (!egressVlan().equals(VlanId.NONE)) {
            return egressVlan();
        }
        // Reuse unicast VLAN if the port has subnet configured
        if (cp != null) {
            Ip4Prefix portSubnet = srManager.deviceConfiguration
                    .getPortSubnet(cp.deviceId(), cp.port());
            VlanId unicastVlan = srManager.getSubnetAssignedVlanId(cp.deviceId(), portSubnet);
            if (unicastVlan != null) {
                return unicastVlan;
            }
        }
        // By default, use VLAN_NO_SUBNET
        return VlanId.vlanId(SegmentRoutingManager.ASSIGNED_VLAN_NO_SUBNET);
    }

    /**
     * Gets the spine-facing port on ingress device of given multicast group.
     *
     * @param mcastIp multicast IP
     * @return spine-facing port on ingress device
     */
    private PortNumber ingressTransitPort(IpAddress mcastIp) {
        DeviceId ingressDevice = getDevice(mcastIp, McastRole.INGRESS)
                .stream().findAny().orElse(null);
        if (ingressDevice != null) {
            NextObjective nextObj = mcastNextObjStore
                    .get(new McastStoreKey(mcastIp, ingressDevice)).value();
            Set<PortNumber> ports = getPorts(nextObj.next());

            for (PortNumber port : ports) {
                // Spine-facing port should have no subnet and no xconnect
                if (srManager.deviceConfiguration != null &&
                        srManager.deviceConfiguration.getPortSubnet(ingressDevice, port) == null &&
                        !srManager.xConnectHandler.hasXConnect(new ConnectPoint(ingressDevice, port))) {
                    return port;
                }
            }
        }
        return null;
    }
}
