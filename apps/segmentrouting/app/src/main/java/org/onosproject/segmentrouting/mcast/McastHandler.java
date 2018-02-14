/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.segmentrouting.mcast;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.basics.McastConfig;
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
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.McastRouteInfo;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.onosproject.segmentrouting.storekey.McastStoreKey;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.segmentrouting.SegmentRoutingManager.INTERNAL_VLAN;

/**
 * Handles Multicast related events.
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

    // Mcast lock to serialize local operations
    private final Lock mcastLock = new ReentrantLock();

    /**
     * Acquires the lock used when making mcast changes.
     */
    private void mcastLock() {
        mcastLock.lock();
    }

    /**
     * Releases the lock used when making mcast changes.
     */
    private void mcastUnlock() {
        mcastLock.unlock();
    }

    // Stability threshold for Mcast. Seconds
    private static final long MCAST_STABLITY_THRESHOLD = 5;
    // Last change done
    private Instant lastMcastChange = Instant.now();

    /**
     * Determines if mcast in the network has been stable in the last
     * MCAST_STABLITY_THRESHOLD seconds, by comparing the current time
     * to the last mcast change timestamp.
     *
     * @return true if stable
     */
    private boolean isMcastStable() {
        long last = (long) (lastMcastChange.toEpochMilli() / 1000.0);
        long now = (long) (Instant.now().toEpochMilli() / 1000.0);
        log.trace("Mcast stable since {}s", now - last);
        return (now - last) > MCAST_STABLITY_THRESHOLD;
    }

    // Verify interval for Mcast
    private static final long MCAST_VERIFY_INTERVAL = 30;

    // Executor for mcast bucket corrector
    private ScheduledExecutorService executorService
            = newScheduledThreadPool(1, groupedThreads("mcastBktCorrector", "mcastbktC-%d", log));

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
        // Init the executor service and the buckets corrector
        executorService.scheduleWithFixedDelay(new McastBucketCorrector(), 10,
                                               MCAST_VERIFY_INTERVAL,
                                               TimeUnit.SECONDS);
    }

    /**
     * Read initial multicast from mcast store.
     */
    public void init() {
        srManager.multicastRouteService.getRoutes().forEach(mcastRoute -> {
            ConnectPoint source = srManager.multicastRouteService.fetchSource(mcastRoute);
            Set<ConnectPoint> sinks = srManager.multicastRouteService.fetchSinks(mcastRoute);
            sinks.forEach(sink -> {
                processSinkAddedInternal(source, sink, mcastRoute.group());
            });
        });
    }

    /**
     * Clean up when deactivating the application.
     */
    public void terminate() {
        executorService.shutdown();
    }

    /**
     * Processes the SOURCE_ADDED event.
     *
     * @param event McastEvent with SOURCE_ADDED type
     */
    public void processSourceAdded(McastEvent event) {
        log.info("processSourceAdded {}", event);
        McastRouteInfo mcastRouteInfo = event.subject();
        if (!mcastRouteInfo.isComplete()) {
            log.info("Incompleted McastRouteInfo. Abort.");
            return;
        }
        ConnectPoint source = mcastRouteInfo.source().orElse(null);
        Set<ConnectPoint> sinks = mcastRouteInfo.sinks();
        IpAddress mcastIp = mcastRouteInfo.route().group();

        sinks.forEach(sink -> processSinkAddedInternal(source, sink, mcastIp));
    }

    /**
     * Processes the SOURCE_UPDATED event.
     *
     * @param event McastEvent with SOURCE_UPDATED type
     */
    public void processSourceUpdated(McastEvent event) {
        log.info("processSourceUpdated {}", event);
        // Get old and new data
        McastRouteInfo mcastRouteInfo = event.subject();
        ConnectPoint newSource = mcastRouteInfo.source().orElse(null);
        mcastRouteInfo = event.prevSubject();
        ConnectPoint oldSource = mcastRouteInfo.source().orElse(null);
        // and group ip
        IpAddress mcastIp = mcastRouteInfo.route().group();
        // Process the update event
        processSourceUpdatedInternal(mcastIp, newSource, oldSource);
    }

    /**
     * Processes the SINK_ADDED event.
     *
     * @param event McastEvent with SINK_ADDED type
     */
    public void processSinkAdded(McastEvent event) {
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
    public void processSinkRemoved(McastEvent event) {
        log.info("processSinkRemoved {}", event);
        McastRouteInfo mcastRouteInfo = event.subject();
        if (!mcastRouteInfo.isComplete()) {
            log.info("Incompleted McastRouteInfo. Abort.");
            return;
        }
        ConnectPoint source = mcastRouteInfo.source().orElse(null);
        ConnectPoint sink = mcastRouteInfo.sink().orElse(null);
        IpAddress mcastIp = mcastRouteInfo.route().group();

        processSinkRemovedInternal(source, sink, mcastIp);
    }

    /**
     * Processes the ROUTE_REMOVED event.
     *
     * @param event McastEvent with ROUTE_REMOVED type
     */
    public void processRouteRemoved(McastEvent event) {
        log.info("processRouteRemoved {}", event);
        McastRouteInfo mcastRouteInfo = event.subject();
        if (!mcastRouteInfo.source().isPresent()) {
            log.info("Incompleted McastRouteInfo. Abort.");
            return;
        }
        // Get group ip and ingress connect point
        IpAddress mcastIp = mcastRouteInfo.route().group();
        ConnectPoint source = mcastRouteInfo.source().orElse(null);

        processRouteRemovedInternal(source, mcastIp);
    }

    /**
     * Process the SOURCE_UPDATED event.
     *
     * @param newSource the updated srouce info
     * @param oldSource the outdated source info
     */
    private void processSourceUpdatedInternal(IpAddress mcastIp,
                                              ConnectPoint newSource,
                                              ConnectPoint oldSource) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            log.debug("Processing source updated for group {}", mcastIp);

            // Build key for the store and retrieve old data
            McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, oldSource.deviceId());

            // Verify leadership on the operation
            if (!isLeader(oldSource)) {
                log.debug("Skip {} due to lack of leadership", mcastIp);
                return;
            }

            // This device is not serving this multicast group
            if (!mcastRoleStore.containsKey(mcastStoreKey) ||
                    !mcastNextObjStore.containsKey(mcastStoreKey)) {
                log.warn("{} is not serving {}. Abort.", oldSource.deviceId(), mcastIp);
                return;
            }
            NextObjective nextObjective = mcastNextObjStore.get(mcastStoreKey).value();
            Set<PortNumber> outputPorts = getPorts(nextObjective.next());

            // Let's remove old flows and groups
            removeGroupFromDevice(oldSource.deviceId(), mcastIp, assignedVlan(oldSource));
            // Push new flows and group
            outputPorts.forEach(portNumber -> addPortToDevice(newSource.deviceId(), portNumber,
                                                              mcastIp, assignedVlan(newSource)));
            addFilterToDevice(newSource.deviceId(), newSource.port(),
                              assignedVlan(newSource), mcastIp);
            // Setup mcast roles
            mcastRoleStore.put(new McastStoreKey(mcastIp, newSource.deviceId()),
                               McastRole.INGRESS);
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Removes the entire mcast tree related to this group.
     *
     * @param mcastIp multicast group IP address
     */
    private void processRouteRemovedInternal(ConnectPoint source, IpAddress mcastIp) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            log.debug("Processing route removed for group {}", mcastIp);

            // Find out the ingress, transit and egress device of the affected group
            DeviceId ingressDevice = getDevice(mcastIp, McastRole.INGRESS)
                    .stream().findAny().orElse(null);
            DeviceId transitDevice = getDevice(mcastIp, McastRole.TRANSIT)
                    .stream().findAny().orElse(null);
            Set<DeviceId> egressDevices = getDevice(mcastIp, McastRole.EGRESS);

            // Verify leadership on the operation
            if (!isLeader(source)) {
                log.debug("Skip {} due to lack of leadership", mcastIp);
                return;
            }

            // If there are egress devices, sinks could be only on the ingress
            if (!egressDevices.isEmpty()) {
                egressDevices.forEach(
                        deviceId -> removeGroupFromDevice(deviceId, mcastIp, assignedVlan(null))
                );
            }
            // Transit could be null
            if (transitDevice != null) {
                removeGroupFromDevice(transitDevice, mcastIp, assignedVlan(null));
            }
            // Ingress device should be not null
            if (ingressDevice != null) {
                removeGroupFromDevice(ingressDevice, mcastIp, assignedVlan(source));
            }
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Removes a path from source to sink for given multicast group.
     *
     * @param source connect point of the multicast source
     * @param sink connection point of the multicast sink
     * @param mcastIp multicast group IP address
     */
    private void processSinkRemovedInternal(ConnectPoint source, ConnectPoint sink,
                                          IpAddress mcastIp) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            // Verify leadership on the operation
            if (!isLeader(source)) {
                log.debug("Skip {} due to lack of leadership", mcastIp);
                return;
            }

            // When source and sink are on the same device
            if (source.deviceId().equals(sink.deviceId())) {
                // Source and sink are on even the same port. There must be something wrong.
                if (source.port().equals(sink.port())) {
                    log.warn("Skip {} since sink {} is on the same port of source {}. Abort",
                             mcastIp, sink, source);
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
                        isLast = removePortFromDevice(
                                link.src().deviceId(),
                                link.src().port(),
                                mcastIp,
                                assignedVlan(link.src().deviceId().equals(source.deviceId()) ? source : null)
                        );
                        mcastRoleStore.remove(new McastStoreKey(mcastIp, link.src().deviceId()));
                    }
                }
            }
        } finally {
            mcastUnlock();
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
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            // Continue only when this instance is the master of source device
            if (!srManager.mastershipService.isLocalMaster(source.deviceId())) {
                log.debug("Skip {} due to lack of mastership of the source device {}",
                          mcastIp, source.deviceId());
                return;
            }

            // Process the ingress device
            addFilterToDevice(source.deviceId(), source.port(), assignedVlan(source), mcastIp);

            // When source and sink are on the same device
            if (source.deviceId().equals(sink.deviceId())) {
                // Source and sink are on even the same port. There must be something wrong.
                if (source.port().equals(sink.port())) {
                    log.warn("Skip {} since sink {} is on the same port of source {}. Abort",
                             mcastIp, sink, source);
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
                    addFilterToDevice(link.dst().deviceId(), link.dst().port(), assignedVlan(null), mcastIp);
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
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Processes the LINK_DOWN event.
     *
     * @param affectedLink Link that is going down
     */
    public void processLinkDown(Link affectedLink) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            // Get groups affected by the link down event
            getAffectedGroups(affectedLink).forEach(mcastIp -> {
                // TODO Optimize when the group editing is in place
                log.debug("Processing link down {} for group {}",
                          affectedLink, mcastIp);

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
                    log.debug("Skip {} due to lack of mastership of the source device {}",
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
                        installPath(mcastIp, source, mcastPath.get());
                    } else {
                        log.warn("Fail to recover egress device {} from link failure {}",
                                 egressDevice, affectedLink);
                        removeGroupFromDevice(egressDevice, mcastIp, assignedVlan(null));
                    }
                });
            });
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Process the DEVICE_DOWN event.
     *
     * @param deviceDown device going down
     */
    public void processDeviceDown(DeviceId deviceDown) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            // Get the mcast groups affected by the device going down
            getAffectedGroups(deviceDown).forEach(mcastIp -> {
                // TODO Optimize when the group editing is in place
                log.debug("Processing device down {} for group {}",
                          deviceDown, mcastIp);

                // Find out the ingress, transit and egress device of affected group
                DeviceId ingressDevice = getDevice(mcastIp, McastRole.INGRESS)
                        .stream().findAny().orElse(null);
                DeviceId transitDevice = getDevice(mcastIp, McastRole.TRANSIT)
                        .stream().findAny().orElse(null);
                Set<DeviceId> egressDevices = getDevice(mcastIp, McastRole.EGRESS);
                ConnectPoint source = getSource(mcastIp);

                // Do not proceed if ingress device or source of this group are missing
                // If sinks are in other leafs, we have ingress, transit, egress, and source
                // If sinks are in the same leaf, we have just ingress and source
                if (ingressDevice == null || source == null) {
                    log.warn("Missing ingress {} or source {} for group {}",
                             ingressDevice, source, mcastIp);
                    return;
                }

                // Verify leadership on the operation
                if (!isLeader(source)) {
                    log.debug("Skip {} due to lack of leadership", mcastIp);
                    return;
                }

                // If it exists, we have to remove it in any case
                if (transitDevice != null) {
                    // Remove entire transit
                    removeGroupFromDevice(transitDevice, mcastIp, assignedVlan(null));
                }
                // If the ingress is down
                if (ingressDevice.equals(deviceDown)) {
                    // Remove entire ingress
                    removeGroupFromDevice(ingressDevice, mcastIp, assignedVlan(source));
                    // If other sinks different from the ingress exist
                    if (!egressDevices.isEmpty()) {
                        // Remove all the remaining egress
                        egressDevices.forEach(
                                egressDevice -> removeGroupFromDevice(egressDevice, mcastIp, assignedVlan(null))
                        );
                    }
                } else {
                    // Egress or transit could be down at this point
                    // Get the ingress-transit port if it exists
                    PortNumber ingressTransitPort = ingressTransitPort(mcastIp);
                    if (ingressTransitPort != null) {
                        // Remove transit-facing port on ingress device
                        removePortFromDevice(ingressDevice, ingressTransitPort, mcastIp, assignedVlan(source));
                    }
                    // One of the egress device is down
                    if (egressDevices.contains(deviceDown)) {
                        // Remove entire device down
                        removeGroupFromDevice(deviceDown, mcastIp, assignedVlan(null));
                        // Remove the device down from egress
                        egressDevices.remove(deviceDown);
                        // If there are no more egress and ingress does not have sinks
                        if (egressDevices.isEmpty() && !hasSinks(ingressDevice, mcastIp)) {
                            // Remove entire ingress
                            mcastRoleStore.remove(new McastStoreKey(mcastIp, ingressDevice));
                            // We have done
                            return;
                        }
                    }
                    // Construct a new path for each egress device
                    egressDevices.forEach(egressDevice -> {
                        Optional<Path> mcastPath = getPath(ingressDevice, egressDevice, mcastIp);
                        // If there is a new path
                        if (mcastPath.isPresent()) {
                            // Let's install the new mcast path for this egress
                            installPath(mcastIp, source, mcastPath.get());
                        } else {
                            // We were not able to find an alternative path for this egress
                            log.warn("Fail to recover egress device {} from device down {}",
                                     egressDevice, deviceDown);
                            removeGroupFromDevice(egressDevice, mcastIp, assignedVlan(null));
                        }
                    });
                }
            });
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Adds filtering objective for given device and port.
     *
     * @param deviceId device ID
     * @param port ingress port number
     * @param assignedVlan assigned VLAN ID
     */
    private void addFilterToDevice(DeviceId deviceId, PortNumber port, VlanId assignedVlan, IpAddress mcastIp) {
        // Do nothing if the port is configured as suppressed
        ConnectPoint connectPoint = new ConnectPoint(deviceId, port);
        SegmentRoutingAppConfig appConfig = srManager.cfgService
                .getConfig(srManager.appId(), SegmentRoutingAppConfig.class);
        if (appConfig != null && appConfig.suppressSubnet().contains(connectPoint)) {
            log.info("Ignore suppressed port {}", connectPoint);
            return;
        }

        FilteringObjective.Builder filtObjBuilder =
                filterObjBuilder(deviceId, port, assignedVlan, mcastIp);
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully add filter on {}/{}, vlan {}",
                        deviceId, port.toLong(), assignedVlan),
                (objective, error) ->
                        log.warn("Failed to add filter on {}/{}, vlan {}: {}",
                                deviceId, port.toLong(), assignedVlan, error));
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
        NextObjective newNextObj;
        if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
            // First time someone request this mcast group via this device
            portBuilder.add(port);
            // New nextObj
            newNextObj = nextObjBuilder(mcastIp, assignedVlan,
                                        portBuilder.build(), null).add();
            // Store the new port
            mcastNextObjStore.put(mcastStoreKey, newNextObj);
        } else {
            // This device already serves some subscribers of this mcast group
            NextObjective nextObj = mcastNextObjStore.get(mcastStoreKey).value();
            // Stop if the port is already in the nextobj
            Set<PortNumber> existingPorts = getPorts(nextObj.next());
            if (existingPorts.contains(port)) {
                log.info("NextObj for {}/{} already exists. Abort", deviceId, port);
                return;
            }
            // Let's add the port and reuse the previous one
            portBuilder.addAll(existingPorts).add(port);
            // Reuse previous nextObj
            newNextObj = nextObjBuilder(mcastIp, assignedVlan,
                                        portBuilder.build(), nextObj.id()).addToExisting();
            // Store the final next objective and send only the difference to the driver
            mcastNextObjStore.put(mcastStoreKey, newNextObj);
            // Add just the new port
            portBuilder = ImmutableSet.builder();
            portBuilder.add(port);
            newNextObj = nextObjBuilder(mcastIp, assignedVlan,
                                        portBuilder.build(), nextObj.id()).addToExisting();
        }
        // Create, store and apply the new nextObj and fwdObj
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully add {} on {}/{}, vlan {}",
                        mcastIp, deviceId, port.toLong(), assignedVlan),
                (objective, error) ->
                        log.warn("Failed to add {} on {}/{}, vlan {}: {}",
                                mcastIp, deviceId, port.toLong(), assignedVlan, error));
        ForwardingObjective fwdObj =
                fwdObjBuilder(mcastIp, assignedVlan, newNextObj.id()).add(context);
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
        ObjectiveContext context;
        ForwardingObjective fwdObj;
        if (existingPorts.isEmpty()) {
            // If this is the last sink, remove flows and last bucket
            // NOTE: Rely on GroupStore garbage collection rather than explicitly
            //       remove L3MG since there might be other flows/groups refer to
            //       the same L2IG
            context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Successfully remove {} on {}/{}, vlan {}",
                            mcastIp, deviceId, port.toLong(), assignedVlan),
                    (objective, error) ->
                            log.warn("Failed to remove {} on {}/{}, vlan {}: {}",
                                    mcastIp, deviceId, port.toLong(), assignedVlan, error));
            fwdObj = fwdObjBuilder(mcastIp, assignedVlan, nextObj.id()).remove(context);
            mcastNextObjStore.remove(mcastStoreKey);
        } else {
            // If this is not the last sink, update flows and groups
            context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Successfully update {} on {}/{}, vlan {}",
                            mcastIp, deviceId, port.toLong(), assignedVlan),
                    (objective, error) ->
                            log.warn("Failed to update {} on {}/{}, vlan {}: {}",
                                    mcastIp, deviceId, port.toLong(), assignedVlan, error));
            // Here we store the next objective with the remaining port
            newNextObj = nextObjBuilder(mcastIp, assignedVlan,
                                        existingPorts, nextObj.id()).removeFromExisting();
            fwdObj = fwdObjBuilder(mcastIp, assignedVlan, newNextObj.id()).add(context);
            mcastNextObjStore.put(mcastStoreKey, newNextObj);
        }
        // Let's modify the next objective removing the bucket
        newNextObj = nextObjBuilder(mcastIp, assignedVlan,
                                    ImmutableSet.of(port), nextObj.id()).removeFromExisting();
        srManager.flowObjectiveService.next(deviceId, newNextObj);
        srManager.flowObjectiveService.forward(deviceId, fwdObj);
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

    private void installPath(IpAddress mcastIp, ConnectPoint source, Path mcastPath) {
        // Get Links
        List<Link> links = mcastPath.links();
        // For each link, modify the next on the source device adding the src port
        // and a new filter objective on the destination port
        links.forEach(link -> {
            addPortToDevice(link.src().deviceId(), link.src().port(), mcastIp,
                            assignedVlan(link.src().deviceId().equals(source.deviceId()) ? source : null));
            addFilterToDevice(link.dst().deviceId(), link.dst().port(), assignedVlan(null),
                              mcastIp);
        });
        // Setup new transit mcast role
        mcastRoleStore.put(new McastStoreKey(mcastIp, links.get(0).dst().deviceId()),
                           McastRole.TRANSIT);
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
            VlanId assignedVlan, Set<PortNumber> outPorts, Integer nextId) {
        // If nextId is null allocate a new one
        if (nextId == null) {
            nextId = srManager.flowObjectiveService.allocateNextId();
        }

        TrafficSelector metadata =
                DefaultTrafficSelector.builder()
                        .matchVlanId(assignedVlan)
                        .matchIPDst(mcastIp.toIpPrefix())
                        .build();

        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.BROADCAST).fromApp(srManager.appId())
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
        IpPrefix mcastPrefix = mcastIp.toIpPrefix();

        if (mcastIp.isIp4()) {
            sbuilder.matchEthType(Ethernet.TYPE_IPV4);
            sbuilder.matchIPDst(mcastPrefix);
        } else {
            sbuilder.matchEthType(Ethernet.TYPE_IPV6);
            sbuilder.matchIPv6Dst(mcastPrefix);
        }


        TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder();
        metabuilder.matchVlanId(assignedVlan);

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective.builder();
        fwdBuilder.withSelector(sbuilder.build())
                .withMeta(metabuilder.build())
                .nextStep(nextId)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(srManager.appId())
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
            VlanId assignedVlan, IpAddress mcastIp) {
        FilteringObjective.Builder filtBuilder = DefaultFilteringObjective.builder();

        if (mcastIp.isIp4()) {
            filtBuilder.withKey(Criteria.matchInPort(ingressPort))
            .addCondition(Criteria.matchEthDstMasked(MacAddress.IPV4_MULTICAST,
                    MacAddress.IPV4_MULTICAST_MASK))
            .addCondition(Criteria.matchVlanId(egressVlan()))
            .withPriority(SegmentRoutingService.DEFAULT_PRIORITY);
        } else {
            filtBuilder.withKey(Criteria.matchInPort(ingressPort))
            .addCondition(Criteria.matchEthDstMasked(MacAddress.IPV6_MULTICAST,
                     MacAddress.IPV6_MULTICAST_MASK))
            .addCondition(Criteria.matchVlanId(egressVlan()))
            .withPriority(SegmentRoutingService.DEFAULT_PRIORITY);
        }
        TrafficTreatment tt = DefaultTrafficTreatment.builder()
                .pushVlan().setVlanId(assignedVlan).build();
        filtBuilder.withMeta(tt);

        return filtBuilder.permit().fromApp(srManager.appId());
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

    // Utility method to verify is a link is a pair-link
    private boolean isPairLink(Link link) {
        // Take src id, src port, dst id and dst port
        final DeviceId srcId = link.src().deviceId();
        final PortNumber srcPort = link.src().port();
        final DeviceId dstId = link.dst().deviceId();
        final PortNumber dstPort = link.dst().port();
        // init as true
        boolean isPairLink = true;
        try {
            // If one of this condition is not true; it is not a pair link
            if (!(srManager.deviceConfiguration().isEdgeDevice(srcId) &&
                  srManager.deviceConfiguration().isEdgeDevice(dstId) &&
                  srManager.deviceConfiguration().getPairDeviceId(srcId).equals(dstId) &&
                  srManager.deviceConfiguration().getPairLocalPort(srcId).equals(srcPort) &&
                  srManager.deviceConfiguration().getPairLocalPort(dstId).equals(dstPort))) {
                    isPairLink = false;
                }
        } catch (DeviceConfigNotFoundException e) {
            // Configuration not provided
            log.warn("Could not check if the link {} is pairlink "
                             + "config not yet provided", link);
            isPairLink = false;
        }
        return isPairLink;
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
        // Takes a snapshot of the topology
        final Topology currentTopology = topologyService.currentTopology();
        List<Path> allPaths = Lists.newArrayList(
                topologyService.getPaths(currentTopology, src, dst)
        );
        // Create list of valid paths
        allPaths.removeIf(path -> path.links().stream().anyMatch(this::isPairLink));
        // If there are no valid paths, just exit
        log.debug("{} path(s) found from {} to {}", allPaths.size(), src, dst);
        if (allPaths.isEmpty()) {
            return Optional.empty();
        }

        // Create a map index of suitablity-to-list of paths. For example
        // a path in the list associated to the index 1 shares only the
        // first hop and it is less suitable of a path belonging to the index
        // 2 that shares leaf-spine.
        Map<Integer, List<Path>> eligiblePaths = Maps.newHashMap();
        // Some init steps
        int nhop;
        McastStoreKey mcastStoreKey;
        Link hop;
        PortNumber srcPort;
        Set<PortNumber> existingPorts;
        NextObjective nextObj;
        // Iterate over paths looking for eligible paths
        for (Path path : allPaths) {
            // Unlikely, it will happen...
            if (!src.equals(path.links().get(0).src().deviceId())) {
                continue;
            }
            nhop = 0;
            // Iterate over the links
            while (nhop < path.links().size()) {
                // Get the link and verify if a next related
                // to the src device exist in the store
                hop = path.links().get(nhop);
                mcastStoreKey = new McastStoreKey(mcastIp, hop.src().deviceId());
                // It does not exist in the store, exit
                if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
                    break;
                }
                // Get the output ports on the next
                nextObj = mcastNextObjStore.get(mcastStoreKey).value();
                existingPorts = getPorts(nextObj.next());
                // And the src port on the link
                srcPort = hop.src().port();
                // the src port is not used as output, exit
                if (!existingPorts.contains(srcPort)) {
                    break;
                }
                nhop++;
            }
            // n_hop defines the index
            if (nhop > 0) {
                eligiblePaths.compute(nhop, (index, paths) -> {
                    paths = paths == null ? Lists.newArrayList() : paths;
                    paths.add(path);
                    return paths;
                });
            }
        }

        // No suitable paths
        if (eligiblePaths.isEmpty()) {
            log.debug("No eligiblePath(s) found from {} to {}", src, dst);
            // Otherwise, randomly pick a path
            Collections.shuffle(allPaths);
            return allPaths.stream().findFirst();
        }

        // Let's take the best ones
        Integer bestIndex = eligiblePaths.keySet()
                .stream()
                .sorted(Comparator.reverseOrder())
                .findFirst().orElse(null);
        List<Path> bestPaths = eligiblePaths.get(bestIndex);
        log.debug("{} eligiblePath(s) found from {} to {}",
                  bestPaths.size(), src, dst);
        // randomly pick a path on the highest index
        Collections.shuffle(bestPaths);
        return bestPaths.stream().findFirst();
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
     * Gets groups which are affected by the device down event.
     *
     * @param deviceId device going down
     * @return a set of multicast IpAddress
     */
    private Set<IpAddress> getAffectedGroups(DeviceId deviceId) {
        return mcastNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
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
            VlanId untaggedVlan = srManager.getInternalVlanId(cp);
            return (untaggedVlan != null) ? untaggedVlan : INTERNAL_VLAN;
        }
        // Use DEFAULT_VLAN if none of the above matches
        return SegmentRoutingManager.INTERNAL_VLAN;
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
                if (srManager.deviceConfiguration() != null &&
                        srManager.deviceConfiguration().getPortSubnets(ingressDevice, port).isEmpty() &&
                        !srManager.xConnectHandler.hasXConnect(new ConnectPoint(ingressDevice, port))) {
                    return port;
                }
            }
        }
        return null;
    }

    /**
     * Verify if the given device has sinks
     * for the multicast group.
     *
     * @param deviceId device Id
     * @param mcastIp multicast IP
     * @return true if the device has sink for the group.
     * False otherwise.
     */
    private boolean hasSinks(DeviceId deviceId, IpAddress mcastIp) {
        if (deviceId != null) {
            // Get the nextobjective
            Versioned<NextObjective> versionedNextObj = mcastNextObjStore.get(
                    new McastStoreKey(mcastIp, deviceId)
            );
            // If it exists
            if (versionedNextObj != null) {
                NextObjective nextObj = versionedNextObj.value();
                // Retrieves all the output ports
                Set<PortNumber> ports = getPorts(nextObj.next());
                // Tries to find at least one port that is not spine-facing
                for (PortNumber port : ports) {
                    // Spine-facing port should have no subnet and no xconnect
                    if (srManager.deviceConfiguration() != null &&
                            (!srManager.deviceConfiguration().getPortSubnets(deviceId, port).isEmpty() ||
                            srManager.xConnectHandler.hasXConnect(new ConnectPoint(deviceId, port)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Removes filtering objective for given device and port.
     *
     * @param deviceId device ID
     * @param port ingress port number
     * @param assignedVlan assigned VLAN ID
     * @param mcastIp multicast IP address
     */
    private void removeFilterToDevice(DeviceId deviceId, PortNumber port, VlanId assignedVlan, IpAddress mcastIp) {
        // Do nothing if the port is configured as suppressed
        ConnectPoint connectPoint = new ConnectPoint(deviceId, port);
        SegmentRoutingAppConfig appConfig = srManager.cfgService
                .getConfig(srManager.appId(), SegmentRoutingAppConfig.class);
        if (appConfig != null && appConfig.suppressSubnet().contains(connectPoint)) {
            log.info("Ignore suppressed port {}", connectPoint);
            return;
        }

        FilteringObjective.Builder filtObjBuilder =
                filterObjBuilder(deviceId, port, assignedVlan, mcastIp);
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully removed filter on {}/{}, vlan {}",
                                         deviceId, port.toLong(), assignedVlan),
                (objective, error) ->
                        log.warn("Failed to remove filter on {}/{}, vlan {}: {}",
                                 deviceId, port.toLong(), assignedVlan, error));
        srManager.flowObjectiveService.filter(deviceId, filtObjBuilder.remove(context));
    }

    /**
     * Updates filtering objective for given device and port.
     * It is called in general when the mcast config has been
     * changed.
     *
     * @param deviceId device ID
     * @param portNum ingress port number
     * @param vlanId assigned VLAN ID
     * @param install true to add, false to remove
     */
    public void updateFilterToDevice(DeviceId deviceId, PortNumber portNum,
                                        VlanId vlanId, boolean install) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            // Iterates over the route and updates properly the filtering objective
            // on the source device.
            srManager.multicastRouteService.getRoutes().forEach(mcastRoute -> {
                ConnectPoint source = srManager.multicastRouteService.fetchSource(mcastRoute);
                if (source.deviceId().equals(deviceId) && source.port().equals(portNum)) {
                    if (install) {
                        addFilterToDevice(deviceId, portNum, vlanId, mcastRoute.group());
                    } else {
                        removeFilterToDevice(deviceId, portNum, vlanId, mcastRoute.group());
                    }
                }
            });
        } finally {
            mcastUnlock();
        }
    }

    private boolean isLeader(ConnectPoint source) {
        // Continue only when we have the mastership on the operation
        if (!srManager.mastershipService.isLocalMaster(source.deviceId())) {
            // When the source is available we just check the mastership
            if (srManager.deviceService.isAvailable(source.deviceId())) {
                return false;
            }
            // Fallback with Leadership service
            // source id is used a topic
            NodeId leader = srManager.leadershipService.runForLeadership(
                    source.deviceId().toString()).leaderNodeId();
            // Verify if this node is the leader
            if (!srManager.clusterService.getLocalNode().id().equals(leader)) {
                return false;
            }
        }
        // Done
        return true;
    }

    /**
     * Performs bucket verification operation for all mcast groups in the devices.
     * Firstly, it verifies that mcast is stable before trying verification operation.
     * Verification consists in creating new nexts with VERIFY operation. Actually,
     * the operation is totally delegated to the driver.
     */
     private final class McastBucketCorrector implements Runnable {

        @Override
        public void run() {
            // Verify if the Mcast has been stable for MCAST_STABLITY_THRESHOLD
            if (!isMcastStable()) {
                return;
            }
            // Acquires lock
            mcastLock();
            try {
                // Iterates over the routes and verify the related next objectives
                srManager.multicastRouteService.getRoutes()
                    .stream()
                    .map(McastRoute::group)
                    .forEach(mcastIp -> {
                        log.trace("Running mcast buckets corrector for mcast group: {}",
                                  mcastIp);

                        // For each group we get current information in the store
                        // and issue a check of the next objectives in place
                        DeviceId ingressDevice = getDevice(mcastIp, McastRole.INGRESS)
                                .stream().findAny().orElse(null);
                        DeviceId transitDevice = getDevice(mcastIp, McastRole.TRANSIT)
                                .stream().findAny().orElse(null);
                        Set<DeviceId> egressDevices = getDevice(mcastIp, McastRole.EGRESS);
                        ConnectPoint source = getSource(mcastIp);

                        // Do not proceed if ingress device or source of this group are missing
                        if (ingressDevice == null || source == null) {
                            log.warn("Unable to run buckets corrector. " +
                                             "Missing ingress {} or source {} for group {}",
                                     ingressDevice, source, mcastIp);
                            return;
                        }

                        // Continue only when this instance is the master of source device
                        if (!srManager.mastershipService.isLocalMaster(source.deviceId())) {
                            log.trace("Unable to run buckets corrector. " +
                                             "Skip {} due to lack of mastership " +
                                             "of the source device {}",
                                     mcastIp, source.deviceId());
                            return;
                        }

                        // Create the set of the devices to be processed
                        ImmutableSet.Builder<DeviceId> devicesBuilder = ImmutableSet.builder();
                        devicesBuilder.add(ingressDevice);
                        if (transitDevice != null) {
                            devicesBuilder.add(transitDevice);
                        }
                        if (!egressDevices.isEmpty()) {
                            devicesBuilder.addAll(egressDevices);
                        }
                        Set<DeviceId> devicesToProcess = devicesBuilder.build();

                        // Iterate over the devices
                        devicesToProcess.forEach(deviceId -> {
                            McastStoreKey currentKey = new McastStoreKey(mcastIp, deviceId);
                            // If next exists in our store verify related next objective
                            if (mcastNextObjStore.containsKey(currentKey)) {
                                NextObjective currentNext = mcastNextObjStore.get(currentKey).value();
                                // Get current ports
                                Set<PortNumber> currentPorts = getPorts(currentNext.next());
                                // Rebuild the next objective
                                currentNext = nextObjBuilder(
                                        mcastIp,
                                        assignedVlan(deviceId.equals(source.deviceId()) ? source : null),
                                        currentPorts,
                                        currentNext.id()
                                ).verify();
                                // Send to the flowobjective service
                                srManager.flowObjectiveService.next(deviceId, currentNext);
                            } else {
                                log.warn("Unable to run buckets corrector. " +
                                                 "Missing next for {} and group {}",
                                         deviceId, mcastIp);
                            }
                        });

                    });
            } finally {
                // Finally, it releases the lock
                mcastUnlock();
            }

        }
    }

    public Map<McastStoreKey, Integer> getMcastNextIds(IpAddress mcastIp) {
        // If mcast ip is present
        if (mcastIp != null) {
            return mcastNextObjStore.entrySet().stream()
                    .filter(mcastEntry -> mcastIp.equals(mcastEntry.getKey().mcastIp()))
                    .collect(Collectors.toMap(Map.Entry::getKey,
                                              entry -> entry.getValue().value().id()));
        }
        // Otherwise take all the groups
        return mcastNextObjStore.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          entry -> entry.getValue().value().id()));
    }

    public Map<McastStoreKey, McastRole> getMcastRoles(IpAddress mcastIp) {
        // If mcast ip is present
        if (mcastIp != null) {
            return mcastRoleStore.entrySet().stream()
                    .filter(mcastEntry -> mcastIp.equals(mcastEntry.getKey().mcastIp()))
                    .collect(Collectors.toMap(Map.Entry::getKey,
                                              entry -> entry.getValue().value()));
        }
        // Otherwise take all the groups
        return mcastRoleStore.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          entry -> entry.getValue().value()));
    }

    public Map<ConnectPoint, List<ConnectPoint>> getMcastPaths(IpAddress mcastIp) {
        Map<ConnectPoint, List<ConnectPoint>> mcastPaths = Maps.newHashMap();
        // Get the source
        ConnectPoint source = getSource(mcastIp);
        // Source cannot be null, we don't know the starting point
        if (source != null) {
            // Init steps
            Set<DeviceId> visited = Sets.newHashSet();
            List<ConnectPoint> currentPath = Lists.newArrayList(
                    source
            );
            // Build recursively the mcast paths
            buildMcastPaths(source.deviceId(), visited, mcastPaths, currentPath, mcastIp);
        }
        return mcastPaths;
    }

    private void buildMcastPaths(DeviceId toVisit, Set<DeviceId> visited,
                                 Map<ConnectPoint, List<ConnectPoint>> mcastPaths,
                                 List<ConnectPoint> currentPath, IpAddress mcastIp) {
        // If we have visited the node to visit
        // there is a loop
        if (visited.contains(toVisit)) {
            return;
        }
        // Visit next-hop
        visited.add(toVisit);
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, toVisit);
        // Looking for next-hops
        if (mcastNextObjStore.containsKey(mcastStoreKey)) {
            // Build egress connectpoints
            NextObjective nextObjective = mcastNextObjStore.get(mcastStoreKey).value();
            // Get Ports
            Set<PortNumber> outputPorts = getPorts(nextObjective.next());
            // Build relative cps
            ImmutableSet.Builder<ConnectPoint> cpBuilder = ImmutableSet.builder();
            outputPorts.forEach(portNumber -> cpBuilder.add(new ConnectPoint(toVisit, portNumber)));
            Set<ConnectPoint> egressPoints = cpBuilder.build();
            // Define other variables for the next steps
            Set<Link> egressLinks;
            List<ConnectPoint> newCurrentPath;
            Set<DeviceId> newVisited;
            DeviceId newToVisit;
            for (ConnectPoint egressPoint : egressPoints) {
                egressLinks = srManager.linkService.getEgressLinks(egressPoint);
                // If it does not have egress links, stop
                if (egressLinks.isEmpty()) {
                    // Add the connect points to the path
                    newCurrentPath = Lists.newArrayList(currentPath);
                    newCurrentPath.add(0, egressPoint);
                    // Save in the map
                    mcastPaths.put(egressPoint, newCurrentPath);
                } else {
                    newVisited = Sets.newHashSet(visited);
                    // Iterate over the egress links for the next hops
                    for (Link egressLink : egressLinks) {
                        // Update to visit
                        newToVisit = egressLink.dst().deviceId();
                        // Add the connect points to the path
                        newCurrentPath = Lists.newArrayList(currentPath);
                        newCurrentPath.add(0, egressPoint);
                        newCurrentPath.add(0, egressLink.dst());
                        // Go to the next hop
                        buildMcastPaths(newToVisit, newVisited, mcastPaths, newCurrentPath, mcastIp);
                    }
                }
            }
        }
    }

}
