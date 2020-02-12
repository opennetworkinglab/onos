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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.basics.McastConfig;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.DefaultObjectiveContext;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.segmentrouting.SRLinkWeigher;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for Multicast Handler.
 */
class McastUtils {
    // Internal reference to the log
    private final Logger log;
    // Internal reference to SR Manager and topology service
    private final SegmentRoutingManager srManager;
    private final TopologyService topologyService;
    // Internal reference to the app id
    private ApplicationId coreAppId;
    // Hashing function for the multicast hasher
    private static final HashFunction HASH_FN = Hashing.md5();
    // Read only cache of the Mcast leader
    private Map<IpAddress, NodeId> mcastLeaderCache;

    /**
     * Builds a new McastUtils object.
     *
     * @param srManager the SR manager
     * @param coreAppId the core application id
     * @param log log reference of the McastHandler
     */
    McastUtils(SegmentRoutingManager srManager, ApplicationId coreAppId, Logger log) {
        this.srManager = srManager;
        this.topologyService = srManager.topologyService;
        this.coreAppId = coreAppId;
        this.log = log;
        this.mcastLeaderCache = Maps.newConcurrentMap();
    }

    /**
     * Clean up when deactivating the application.
     */
    void terminate() {
        mcastLeaderCache.clear();
    }

    /**
     * Get router mac using application config and the connect point.
     *
     * @param deviceId the device id
     * @param port the port number
     * @return the router mac if the port is configured, otherwise null
     */
    private MacAddress getRouterMac(DeviceId deviceId, PortNumber port) {
        // Do nothing if the port is configured as suppressed
        ConnectPoint connectPoint = new ConnectPoint(deviceId, port);
        SegmentRoutingAppConfig appConfig = srManager.cfgService
                .getConfig(srManager.appId(), SegmentRoutingAppConfig.class);
        if (appConfig != null && appConfig.suppressSubnet().contains(connectPoint)) {
            log.info("Ignore suppressed port {}", connectPoint);
            return MacAddress.NONE;
        }
        // Get the router mac using the device configuration
        MacAddress routerMac;
        try {
            routerMac = srManager.deviceConfiguration().getDeviceMac(deviceId);
        } catch (DeviceConfigNotFoundException dcnfe) {
            log.warn("Failed to get device MAC since the device {} is not configured", deviceId);
            return null;
        }
        return routerMac;
    }

    /**
     * Adds filtering objective for given device and port.
     *
     * @param deviceId device ID
     * @param port ingress port number
     * @param assignedVlan assigned VLAN ID
     * @param mcastIp the group address
     * @param mcastRole the role of the device
     * @param matchOnMac match or not on macaddress
     */
    void addFilterToDevice(DeviceId deviceId, PortNumber port, VlanId assignedVlan,
                           IpAddress mcastIp, McastRole mcastRole, boolean matchOnMac) {
        if (!srManager.deviceConfiguration().isConfigured(deviceId)) {
            log.debug("skip update of fitering objective for unconfigured device: {}", deviceId);
            return;
        }
        MacAddress routerMac = getRouterMac(deviceId, port);

        if (MacAddress.NONE.equals(routerMac)) {
            return;
        }
        FilteringObjective.Builder filtObjBuilder = filterObjBuilder(port, assignedVlan, mcastIp,
                                                                     routerMac, mcastRole, matchOnMac);
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully add filter on {}/{}, vlan {}",
                                         deviceId, port.toLong(), assignedVlan),
                (objective, error) ->
                        log.warn("Failed to add filter on {}/{}, vlan {}: {}",
                                 deviceId, port.toLong(), assignedVlan, error));
        srManager.flowObjectiveService.filter(deviceId, filtObjBuilder.add(context));
    }

    /**
     * Removes filtering objective for given device and port.
     *
     * @param deviceId device ID
     * @param port ingress port number
     * @param assignedVlan assigned VLAN ID
     * @param mcastIp multicast IP address
     * @param mcastRole the multicast role of the device
     */
    void removeFilterToDevice(DeviceId deviceId, PortNumber port, VlanId assignedVlan,
                              IpAddress mcastIp, McastRole mcastRole) {
        if (!srManager.deviceConfiguration().isConfigured(deviceId)) {
            log.debug("skip update of fitering objective for unconfigured device: {}", deviceId);
            return;
        }
        MacAddress routerMac = getRouterMac(deviceId, port);

        if (MacAddress.NONE.equals(routerMac)) {
            return;
        }
        FilteringObjective.Builder filtObjBuilder =
                filterObjBuilder(port, assignedVlan, mcastIp, routerMac, mcastRole, false);
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully removed filter on {}/{}, vlan {}",
                                         deviceId, port.toLong(), assignedVlan),
                (objective, error) ->
                        log.warn("Failed to remove filter on {}/{}, vlan {}: {}",
                                 deviceId, port.toLong(), assignedVlan, error));
        srManager.flowObjectiveService.filter(deviceId, filtObjBuilder.remove(context));
    }

    /**
     * Gets ingress VLAN from McastConfig.
     *
     * @return ingress VLAN or VlanId.NONE if not configured
     */
    private VlanId ingressVlan() {
        McastConfig mcastConfig =
                srManager.cfgService.getConfig(coreAppId, McastConfig.class);
        return (mcastConfig != null) ? mcastConfig.ingressVlan() : VlanId.NONE;
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
    VlanId assignedVlan(ConnectPoint cp) {
        // Use the egressVlan if it is tagged
        if (!egressVlan().equals(VlanId.NONE)) {
            return egressVlan();
        }
        // Reuse unicast VLAN if the port has subnet configured
        if (cp != null) {
            VlanId untaggedVlan = srManager.getInternalVlanId(cp);
            return (untaggedVlan != null) ? untaggedVlan
                                          : srManager.getDefaultInternalVlan();
        }
        // Use DEFAULT_VLAN if none of the above matches
        return srManager.getDefaultInternalVlan();
    }



    /**
     * Gets sources connect points of given multicast group.
     *
     * @param mcastIp multicast IP
     * @return sources connect points or empty set if not found
     */
    Set<ConnectPoint> getSources(IpAddress mcastIp) {
        // TODO we should support different types of routes
        McastRoute mcastRoute = srManager.multicastRouteService.getRoutes().stream()
                .filter(mcastRouteInternal -> mcastRouteInternal.group().equals(mcastIp))
                .findFirst().orElse(null);
        return mcastRoute == null ? ImmutableSet.of() :
                srManager.multicastRouteService.sources(mcastRoute);
    }

    /**
     * Gets sinks of given multicast group.
     *
     * @param mcastIp multicast IP
     * @return map of sinks or empty map if not found
     */
    Map<HostId, Set<ConnectPoint>> getSinks(IpAddress mcastIp) {
        // TODO we should support different types of routes
        McastRoute mcastRoute = srManager.multicastRouteService.getRoutes().stream()
                .filter(mcastRouteInternal -> mcastRouteInternal.group().equals(mcastIp))
                .findFirst().orElse(null);
        return mcastRoute == null ?
                ImmutableMap.of() :
                srManager.multicastRouteService.routeData(mcastRoute).sinks();
    }

    /**
     * Get sinks affected by this egress device.
     *
     * @param egressDevice the egress device
     * @param mcastIp the mcast ip address
     * @return the map of the sinks affected
     */
    Map<HostId, Set<ConnectPoint>> getAffectedSinks(DeviceId egressDevice,
                                                    IpAddress mcastIp) {
        return getSinks(mcastIp).entrySet()
                .stream()
                .filter(hostIdSetEntry -> hostIdSetEntry.getValue().stream()
                        .map(ConnectPoint::deviceId)
                        .anyMatch(deviceId -> deviceId.equals(egressDevice))
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Creates a next objective builder for multicast.
     *
     * @param mcastIp multicast group
     * @param assignedVlan assigned VLAN ID
     * @param outPorts set of output port numbers
     * @param nextId the next id
     * @return next objective builder
     */
    NextObjective.Builder nextObjBuilder(IpAddress mcastIp, VlanId assignedVlan,
                                         Set<PortNumber> outPorts, Integer nextId) {
        // If nextId is null allocate a new one
        if (nextId == null) {
            nextId = srManager.flowObjectiveService.allocateNextId();
        }
        // Build the meta selector with the fwd objective info
        TrafficSelector metadata =
                DefaultTrafficSelector.builder()
                        .matchVlanId(assignedVlan)
                        .matchIPDst(mcastIp.toIpPrefix())
                        .build();
        // Define the nextobjective type
        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.BROADCAST)
                .fromApp(srManager.appId())
                .withMeta(metadata);
        // Add the output ports
        outPorts.forEach(port -> {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            if (egressVlan().equals(VlanId.NONE)) {
                tBuilder.popVlan();
            }
            tBuilder.setOutput(port);
            nextObjBuilder.addTreatment(tBuilder.build());
        });
        // Done return the complete builder
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
    ForwardingObjective.Builder fwdObjBuilder(IpAddress mcastIp,
                                                      VlanId assignedVlan, int nextId) {
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        // Let's the matching on the group address
        // TODO SSM support in future
        if (mcastIp.isIp6()) {
            sbuilder.matchEthType(Ethernet.TYPE_IPV6);
            sbuilder.matchIPv6Dst(mcastIp.toIpPrefix());
        } else {
            sbuilder.matchEthType(Ethernet.TYPE_IPV4);
            sbuilder.matchIPDst(mcastIp.toIpPrefix());
        }
        // Then build the meta selector
        TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder();
        metabuilder.matchVlanId(assignedVlan);
        // Finally return the completed builder
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
     * @param ingressPort ingress port of the multicast stream
     * @param assignedVlan assigned VLAN ID
     * @param mcastIp the group address
     * @param routerMac router MAC. This is carried in metadata and used from some switches that
     *                  need to put unicast entry before multicast entry in TMAC table.
     * @param mcastRole the Multicast role
     * @param matchOnMac match or not on macaddress
     * @return filtering objective builder
     */
    private FilteringObjective.Builder filterObjBuilder(PortNumber ingressPort, VlanId assignedVlan,
                                                        IpAddress mcastIp, MacAddress routerMac, McastRole mcastRole,
                                                        boolean matchOnMac) {
        FilteringObjective.Builder filtBuilder = DefaultFilteringObjective.builder();
        // Let's add the in port matching and the priority
        filtBuilder.withKey(Criteria.matchInPort(ingressPort))
                .withPriority(SegmentRoutingService.DEFAULT_PRIORITY);
        // According to the mcast role we match on the proper vlan
        // If the role is null we are on the transit or on the egress
        if (mcastRole == null) {
            filtBuilder.addCondition(Criteria.matchVlanId(egressVlan()));
        } else {
            filtBuilder.addCondition(Criteria.matchVlanId(ingressVlan()));
        }
        // Add vlan info to the treatment builder
        TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder()
                .pushVlan().setVlanId(assignedVlan);
        // Additionally match on mac address and augment the treatment
        if (matchOnMac) {
            // According to the IP type we set the proper match on the mac address
            if (mcastIp.isIp4()) {
                filtBuilder.addCondition(Criteria.matchEthDstMasked(MacAddress.IPV4_MULTICAST,
                        MacAddress.IPV4_MULTICAST_MASK));
            } else {
                filtBuilder.addCondition(Criteria.matchEthDstMasked(MacAddress.IPV6_MULTICAST,
                        MacAddress.IPV6_MULTICAST_MASK));
            }
            // We set mac address to the treatment
            if (routerMac != null && !routerMac.equals(MacAddress.NONE)) {
                ttb.setEthDst(routerMac);
            }
        }
        // We finally build the meta treatment
        TrafficTreatment tt = ttb.build();
        filtBuilder.withMeta(tt);
        // Done, we return a permit filtering objective
        return filtBuilder.permit().fromApp(srManager.appId());
    }

    /**
     * Gets output ports information from treatments.
     *
     * @param treatments collection of traffic treatments
     * @return set of output port numbers
     */
    Set<PortNumber> getPorts(Collection<TrafficTreatment> treatments) {
        ImmutableSet.Builder<PortNumber> builder = ImmutableSet.builder();
        treatments.forEach(treatment -> treatment.allInstructions().stream()
                    .filter(instr -> instr instanceof Instructions.OutputInstruction)
                    .forEach(instr -> builder.add(((Instructions.OutputInstruction) instr).port())));
        return builder.build();
    }

    /**
     * Returns the hash of the group address.
     *
     * @param ipAddress the ip address
     * @return the hash of the address
     */
    private Long hasher(IpAddress ipAddress) {
        return HASH_FN.newHasher()
                .putBytes(ipAddress.toOctets())
                .hash()
                .asLong();
    }

    /**
     * Given a multicast group define a leader for it.
     *
     * @param mcastIp the group address
     * @return true if the instance is the leader of the group
     */
    boolean isLeader(IpAddress mcastIp) {
        // Get our id
        final NodeId currentNodeId = srManager.clusterService.getLocalNode().id();
        // Get the leader for this group using the ip address as key
        final NodeId leader = srManager.workPartitionService.getLeader(mcastIp, this::hasher);
        // If there is not a leader, let's send an error
        if (leader == null) {
            log.error("Fail to elect a leader for {}.", mcastIp);
            return false;
        }
        // Update cache and return operation result
        mcastLeaderCache.put(mcastIp, leader);
        return currentNodeId.equals(leader);
    }

    /**
     * Given a multicast group withdraw its leader.
     *
     * @param mcastIp the group address
     */
    void withdrawLeader(IpAddress mcastIp) {
        // For now just update the cache
        mcastLeaderCache.remove(mcastIp);
    }

    Map<IpAddress, NodeId> getMcastLeaders(IpAddress mcastIp) {
        // If mcast ip is present
        if (mcastIp != null) {
            return mcastLeaderCache.entrySet().stream()
                    .filter(entry -> entry.getKey().equals(mcastIp))
                    .collect(Collectors.toMap(Map.Entry::getKey,
                                              Map.Entry::getValue));
        }
        // Otherwise take all the groups
        return ImmutableMap.copyOf(mcastLeaderCache);
    }

    /**
     * Go through all the paths, looking for shared links to be used
     * in the final path computation.
     *
     * @param egresses egress devices
     * @param availablePaths all the available paths towards the egress
     * @return shared links between egress devices
     */
    private Set<Link> exploreMcastTree(Set<DeviceId> egresses,
                                       Map<DeviceId, List<Path>> availablePaths) {
        int minLength = Integer.MAX_VALUE;
        int length;
        List<Path> currentPaths;
        // Verify the source can still reach all the egresses
        for (DeviceId egress : egresses) {
            // From the source we cannot reach all the sinks
            // just continue and let's figure out after
            currentPaths = availablePaths.get(egress);
            if (currentPaths.isEmpty()) {
                continue;
            }
            // Get the length of the first one available, update the min length
            length = currentPaths.get(0).links().size();
            if (length < minLength) {
                minLength = length;
            }
        }
        // If there are no paths
        if (minLength == Integer.MAX_VALUE) {
            return Collections.emptySet();
        }
        int index = 0;
        Set<Link> sharedLinks = Sets.newHashSet();
        Set<Link> currentSharedLinks;
        Set<Link> currentLinks;
        DeviceId egressToRemove = null;
        // Let's find out the shared links
        while (index < minLength) {
            // Initialize the intersection with the paths related to the first egress
            currentPaths = availablePaths.get(egresses.stream().findFirst().orElse(null));
            currentSharedLinks = Sets.newHashSet();
            // Iterate over the paths and take the "index" links
            for (Path path : currentPaths) {
                currentSharedLinks.add(path.links().get(index));
            }
            // Iterate over the remaining egress
            for (DeviceId egress : egresses) {
                // Iterate over the paths and take the "index" links
                currentLinks = Sets.newHashSet();
                for (Path path : availablePaths.get(egress)) {
                    currentLinks.add(path.links().get(index));
                }
                // Do intersection
                currentSharedLinks = Sets.intersection(currentSharedLinks, currentLinks);
                // If there are no shared paths exit and record the device to remove
                // we have to retry with a subset of sinks
                if (currentSharedLinks.isEmpty()) {
                    egressToRemove = egress;
                    index = minLength;
                    break;
                }
            }
            sharedLinks.addAll(currentSharedLinks);
            index++;
        }
        // If the shared links is empty and there are egress let's retry another time with less sinks,
        // we can still build optimal subtrees
        if (sharedLinks.isEmpty() && egresses.size() > 1 && egressToRemove != null) {
            egresses.remove(egressToRemove);
            sharedLinks = exploreMcastTree(egresses, availablePaths);
        }
        return sharedLinks;
    }

    /**
     * Build Mcast tree having as root the given source and as leaves the given egress points.
     *
     * @param mcastIp multicast group
     * @param source source of the tree
     * @param sinks leaves of the tree
     * @return the computed Mcast tree
     */
    Map<ConnectPoint, List<Path>> computeSinkMcastTree(IpAddress mcastIp,
                                                       DeviceId source,
                                                       Set<ConnectPoint> sinks) {
        // Get the egress devices, remove source from the egress if present
        Set<DeviceId> egresses = sinks.stream().map(ConnectPoint::deviceId)
                .filter(deviceId -> !deviceId.equals(source)).collect(Collectors.toSet());
        Map<DeviceId, List<Path>> mcastTree = computeMcastTree(mcastIp, source, egresses);
        final Map<ConnectPoint, List<Path>> finalTree = Maps.newHashMap();
        // We need to put back the source if it was originally present
        sinks.forEach(sink -> {
            List<Path> sinkPaths = mcastTree.get(sink.deviceId());
            finalTree.put(sink, sinkPaths != null ? sinkPaths : ImmutableList.of());
        });
        return finalTree;
    }

    /**
     * Build Mcast tree having as root the given source and as leaves the given egress.
     *
     * @param mcastIp multicast group
     * @param source source of the tree
     * @param egresses leaves of the tree
     * @return the computed Mcast tree
     */
    private Map<DeviceId, List<Path>> computeMcastTree(IpAddress mcastIp,
                                                       DeviceId source,
                                                       Set<DeviceId> egresses) {
        log.debug("Computing tree for Multicast group {}, source {} and leafs {}",
                  mcastIp, source, egresses);
        // Pre-compute all the paths
        Map<DeviceId, List<Path>> availablePaths = Maps.newHashMap();
        egresses.forEach(egress -> availablePaths.put(egress, getPaths(source, egress,
                                                                       Collections.emptySet())));
        // Explore the topology looking for shared links amongst the egresses
        Set<Link> linksToEnforce = exploreMcastTree(Sets.newHashSet(egresses), availablePaths);
        // Build the final paths enforcing the shared links between egress devices
        availablePaths.clear();
        egresses.forEach(egress -> availablePaths.put(egress, getPaths(source, egress,
                                                                       linksToEnforce)));
        return availablePaths;
    }

    /**
     * Gets path from src to dst computed using the custom link weigher.
     *
     * @param src source device ID
     * @param dst destination device ID
     * @param linksToEnforce links to be enforced
     * @return list of paths from src to dst
     */
    List<Path> getPaths(DeviceId src, DeviceId dst, Set<Link> linksToEnforce) {
        final Topology currentTopology = topologyService.currentTopology();
        final LinkWeigher linkWeigher = new SRLinkWeigher(srManager, src, linksToEnforce);
        List<Path> allPaths = Lists.newArrayList(topologyService.getPaths(currentTopology, src, dst, linkWeigher));
        log.trace("{} path(s) found from {} to {}", allPaths.size(), src, dst);
        return allPaths;
    }

    /**
     * Gets a stored path having dst as egress.
     *
     * @param dst destination device ID
     * @param storedPaths paths list
     * @return an optional path
     */
    Optional<? extends List<Link>> getStoredPath(DeviceId dst, Collection<? extends List<Link>> storedPaths) {
        return storedPaths.stream()
                .filter(path -> path.get(path.size() - 1).dst().deviceId().equals(dst))
                .findFirst();
    }

    /**
     * Returns a set of affected paths by the failed element.
     *
     * @param paths the paths to check
     * @param failedElement the failed element
     * @return the affected paths
     */
    Set<List<Link>> getAffectedPaths(Set<List<Link>> paths, Object failedElement) {
        if (failedElement instanceof DeviceId) {
            return getAffectedPathsByDevice(paths, failedElement);
        }
        return getAffectedPathsByLink(paths, failedElement);
    }

    private Set<List<Link>> getAffectedPathsByDevice(Set<List<Link>> paths, Object failedElement) {
        DeviceId affectedDevice = (DeviceId) failedElement;
        Set<List<Link>> affectedPaths = Sets.newHashSet();
        paths.forEach(path -> {
            if (path.stream().anyMatch(link -> link.src().deviceId().equals(affectedDevice))) {
                affectedPaths.add(path);
            }
        });
        return affectedPaths;
    }

    private Set<List<Link>> getAffectedPathsByLink(Set<List<Link>> paths, Object failedElement) {
        Link affectedLink = (Link) failedElement;
        Set<List<Link>> affectedPaths = Sets.newHashSet();
        paths.forEach(path -> {
            if (path.contains(affectedLink)) {
                affectedPaths.add(path);
            }
        });
        return affectedPaths;
    }

    /**
     * Checks if the failure is affecting the transit device.
     *
     * @param devices the transit devices
     * @param failedElement the failed element
     * @return true if the failed element is affecting the transit devices
     */
    boolean isInfraFailure(Set<DeviceId> devices, Object failedElement) {
        if (failedElement instanceof DeviceId) {
            return isInfraFailureByDevice(devices, failedElement);
        }
        return true;
    }

    private boolean isInfraFailureByDevice(Set<DeviceId> devices, Object failedElement) {
        DeviceId affectedDevice = (DeviceId) failedElement;
        return devices.contains(affectedDevice);
    }

    /**
     * Checks if a port is an infra port.
     *
     * @param connectPoint port to be checked
     * @param storedPaths paths to be checked against
     * @return true if the port is an infra port. False otherwise.
     */
    boolean isInfraPort(ConnectPoint connectPoint, Collection<? extends List<Link>> storedPaths) {
        for (List<Link> path : storedPaths) {
            if (path.stream().anyMatch(link -> link.src().equals(connectPoint) ||
                    link.dst().equals(connectPoint))) {
                return true;
            }
        }
        return false;
    }

}
