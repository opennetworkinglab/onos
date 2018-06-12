/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.segmentrouting.grouphandler;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.RandomUtils;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.DefaultObjectiveContext;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.link.LinkService;
import org.onosproject.segmentrouting.DefaultRoutingHandler;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceProperties;
import org.onosproject.segmentrouting.storekey.DestinationSetNextObjectiveStoreKey;
import org.onosproject.segmentrouting.storekey.PortNextObjectiveStoreKey;
import org.onosproject.segmentrouting.storekey.VlanNextObjectiveStoreKey;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.slf4j.Logger;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.segmentrouting.SegmentRoutingManager.INTERNAL_VLAN;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default ECMP group handler creation module. This component creates a set of
 * ECMP groups for every neighbor that this device is connected to based on
 * whether the current device is an edge device or a transit device.
 */
public class DefaultGroupHandler {
    private static final Logger log = getLogger(DefaultGroupHandler.class);

    private static final long VERIFY_INTERVAL = 30; // secs

    protected final DeviceId deviceId;
    protected final ApplicationId appId;
    protected final DeviceProperties deviceConfig;
    protected final List<Integer> allSegmentIds;
    protected int ipv4NodeSegmentId = -1;
    protected int ipv6NodeSegmentId = -1;
    protected boolean isEdgeRouter = false;
    protected MacAddress nodeMacAddr = null;
    protected LinkService linkService;
    protected FlowObjectiveService flowObjectiveService;
    /**
     * local store for neighbor-device-ids and the set of ports on this device
     * that connect to the same neighbor.
     */
    protected ConcurrentHashMap<DeviceId, Set<PortNumber>> devicePortMap =
            new ConcurrentHashMap<>();
    /**
     *  local store for ports on this device connected to neighbor-device-id.
     */
    protected ConcurrentHashMap<PortNumber, DeviceId> portDeviceMap =
            new ConcurrentHashMap<>();

    // distributed store for (device+destination-set) mapped to next-id and neighbors
    protected EventuallyConsistentMap<DestinationSetNextObjectiveStoreKey, NextNeighbors>
            dsNextObjStore = null;
    // distributed store for (device+subnet-ip-prefix) mapped to next-id
    protected EventuallyConsistentMap<VlanNextObjectiveStoreKey, Integer>
            vlanNextObjStore = null;
    // distributed store for (device+port+treatment) mapped to next-id
    protected EventuallyConsistentMap<PortNextObjectiveStoreKey, Integer>
            portNextObjStore = null;
    private SegmentRoutingManager srManager;

    private ScheduledExecutorService executorService
    = newScheduledThreadPool(1, groupedThreads("bktCorrector", "bktC-%d", log));

    protected KryoNamespace.Builder kryo = new KryoNamespace.Builder()
            .register(URI.class).register(HashSet.class)
            .register(DeviceId.class).register(PortNumber.class)
            .register(DestinationSet.class).register(PolicyGroupIdentifier.class)
            .register(PolicyGroupParams.class)
            .register(GroupBucketIdentifier.class)
            .register(GroupBucketIdentifier.BucketOutputType.class);

    protected DefaultGroupHandler(DeviceId deviceId, ApplicationId appId,
                                  DeviceProperties config,
                                  LinkService linkService,
                                  FlowObjectiveService flowObjService,
                                  SegmentRoutingManager srManager) {
        this.deviceId = checkNotNull(deviceId);
        this.appId = checkNotNull(appId);
        this.deviceConfig = checkNotNull(config);
        this.linkService = checkNotNull(linkService);
        this.allSegmentIds = checkNotNull(config.getAllDeviceSegmentIds());
        try {
            this.ipv4NodeSegmentId = config.getIPv4SegmentId(deviceId);
            this.ipv6NodeSegmentId = config.getIPv6SegmentId(deviceId);
            this.isEdgeRouter = config.isEdgeDevice(deviceId);
            this.nodeMacAddr = checkNotNull(config.getDeviceMac(deviceId));
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage()
                    + " Skipping value assignment in DefaultGroupHandler");
        }
        this.flowObjectiveService = flowObjService;
        this.dsNextObjStore = srManager.dsNextObjStore();
        this.vlanNextObjStore = srManager.vlanNextObjStore();
        this.portNextObjStore = srManager.portNextObjStore();
        this.srManager = srManager;
        executorService.scheduleWithFixedDelay(new BucketCorrector(), 10,
                                               VERIFY_INTERVAL,
                                               TimeUnit.SECONDS);
        populateNeighborMaps();
    }

    /**
     * Gracefully shuts down a groupHandler. Typically called when the handler is
     * no longer needed.
     */
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Creates a group handler object.
     *
     * @param deviceId device identifier
     * @param appId application identifier
     * @param config interface to retrieve the device properties
     * @param linkService link service object
     * @param flowObjService flow objective service object
     * @param srManager segment routing manager
     * @throws DeviceConfigNotFoundException if the device configuration is not found
     * @return default group handler type
     */
    public static DefaultGroupHandler createGroupHandler(
                                                         DeviceId deviceId,
                                                         ApplicationId appId,
                                                         DeviceProperties config,
                                                         LinkService linkService,
                                                         FlowObjectiveService flowObjService,
                                                         SegmentRoutingManager srManager)
                                                                 throws DeviceConfigNotFoundException {
        return new DefaultGroupHandler(deviceId, appId, config,
                                       linkService,
                                       flowObjService,
                                       srManager);
    }

    /**
     * Updates local stores for link-src-device/port to neighbor (link-dst) for
     * link that has come up.
     *
     * @param link the infrastructure link
     */
    public void portUpForLink(Link link) {
        if (!link.src().deviceId().equals(deviceId)) {
            log.warn("linkUp: deviceId{} doesn't match with link src {}",
                     deviceId, link.src().deviceId());
            return;
        }

        log.info("* portUpForLink: Device {} linkUp at local port {} to "
                + "neighbor {}", deviceId, link.src().port(), link.dst().deviceId());
        // ensure local state is updated even if linkup is aborted later on
        addNeighborAtPort(link.dst().deviceId(),
                          link.src().port());
    }

    /**
     * Updates local stores for link-src-device/port to neighbor (link-dst) for
     * link that has gone down.
     *
     * @param link the infrastructure link
     */
    public void portDownForLink(Link link) {
        PortNumber port = link.src().port();
        if (portDeviceMap.get(port) == null) {
            log.warn("portDown: unknown port");
            return;
        }

        log.debug("Device {} portDown {} to neighbor {}", deviceId, port,
                  portDeviceMap.get(port));
        devicePortMap.get(portDeviceMap.get(port)).remove(port);
        portDeviceMap.remove(port);
    }

    /**
     * Cleans up local stores for removed neighbor device.
     *
     * @param neighborId the device identifier for the neighbor device
     */
    public void cleanUpForNeighborDown(DeviceId neighborId) {
        Set<PortNumber> ports = devicePortMap.remove(neighborId);
        if (ports != null) {
            ports.forEach(p -> portDeviceMap.remove(p));
        }
    }

    /**
     * Checks all groups in the src-device of link for neighbor sets that include
     * the dst-device of link, and edits the hash groups according to link up
     * or down. Should only be called by the master instance of the src-switch
     * of link. Typically used when there are no route-path changes due to the
     * link up or down, as the ECMPspg does not change.
     *
     * @param link the infrastructure link that has gone down or come up
     * @param linkDown true if link has gone down
     * @param firstTime true if link has come up for the first time i.e a link
     *                  not seen-before
     */
    public void retryHash(Link link, boolean linkDown, boolean firstTime) {
        MacAddress neighborMac;
        try {
            neighborMac = deviceConfig.getDeviceMac(link.dst().deviceId());
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting retryHash.");
            return;
        }
        // find all the destinationSets related to link
        Set<DestinationSetNextObjectiveStoreKey> dsKeySet = dsNextObjStore.entrySet()
                .stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                // Filter out PW transit groups or include them if MPLS ECMP is supported
                .filter(entry -> !entry.getKey().destinationSet().notBos() ||
                        (entry.getKey().destinationSet().notBos() && srManager.getMplsEcmp()))
                // Filter out simple SWAP groups or include them if MPLS ECMP is supported
                .filter(entry -> !entry.getKey().destinationSet().swap() ||
                        (entry.getKey().destinationSet().swap() && srManager.getMplsEcmp()))
                .filter(entry -> entry.getValue().containsNextHop(link.dst().deviceId()))
                .map(entry -> entry.getKey())
                .collect(Collectors.toSet());

        log.debug("retryHash: dsNextObjStore contents for linkSrc {} -> linkDst {}: {}",
                  deviceId, link.dst().deviceId(), dsKeySet);

        for (DestinationSetNextObjectiveStoreKey dsKey : dsKeySet) {
            NextNeighbors nextHops = dsNextObjStore.get(dsKey);
            if (nextHops == null) {
                log.warn("retryHash in device {}, but global store has no record "
                         + "for dsKey:{}", deviceId, dsKey);
                continue;
            }
            int nextId = nextHops.nextId();
            Set<DeviceId> dstSet = nextHops.getDstForNextHop(link.dst().deviceId());
            if (!linkDown) {
                List<PortLabel> pl = Lists.newArrayList();
                if (firstTime) {
                    // some links may have come up before the next-objective was created
                    // we take this opportunity to ensure other ports to same next-hop-dst
                    // are part of the hash group (see CORD-1180). Duplicate additions
                    // to the same hash group are avoided by the driver.
                    for (PortNumber p : devicePortMap.get(link.dst().deviceId())) {
                        dstSet.forEach(dst -> {
                            int edgeLabel = dsKey.destinationSet().getEdgeLabel(dst);
                            pl.add(new PortLabel(p, edgeLabel));
                        });
                    }
                    addToHashedNextObjective(pl, neighborMac, nextId);
                } else {
                    // handle only the port that came up
                    dstSet.forEach(dst -> {
                        int edgeLabel = dsKey.destinationSet().getEdgeLabel(dst);
                        pl.add(new PortLabel(link.src().port(), edgeLabel));
                    });
                    addToHashedNextObjective(pl, neighborMac, nextId);
                }
            } else {
                // linkdown
                List<PortLabel> pl = Lists.newArrayList();
                dstSet.forEach(dst -> {
                    int edgeLabel = dsKey.destinationSet().getEdgeLabel(dst);
                    pl.add(new PortLabel(link.src().port(), edgeLabel));
                });
                removeFromHashedNextObjective(pl, neighborMac, nextId);
            }
        }
    }

    /**
     * Utility class for associating output ports and the corresponding MPLS
     * labels to push. In dual-homing, there are different labels to push
     * corresponding to the destination switches in an edge-pair. If both
     * destinations are reachable via the same spine, then the output-port to
     * the spine will be associated with two labels i.e. there will be two
     * PortLabel objects for the same port but with different labels.
     */
    private class PortLabel {
        PortNumber port;
        int edgeLabel;

        PortLabel(PortNumber port, int edgeLabel) {
            this.port = port;
            this.edgeLabel = edgeLabel;
        }

        @Override
        public String toString() {
            return port.toString() + "/" + String.valueOf(edgeLabel);
        }
    }

    /**
     * Makes a call to the FlowObjective service to add buckets to
     * a hashed group. User must ensure that all the ports & labels are meant
     * same neighbor (ie. dstMac).
     *
     * @param portLabels a collection of port & label combinations to add
     *                   to the hash group identified by the nextId
     * @param dstMac destination mac address of next-hop
     * @param nextId id for next-objective to which buckets will be added
     *
     */
    private void addToHashedNextObjective(Collection<PortLabel> portLabels,
                                          MacAddress dstMac, Integer nextId) {
        // setup metadata to pass to nextObjective - indicate the vlan on egress
        // if needed by the switch pipeline. Since hashed next-hops are always to
        // other neighboring routers, there is no subnet assigned on those ports.
        TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder();
        metabuilder.matchVlanId(INTERNAL_VLAN);
        NextObjective.Builder nextObjBuilder = DefaultNextObjective.builder()
                .withId(nextId)
                .withType(NextObjective.Type.HASHED)
                .withMeta(metabuilder.build())
                .fromApp(appId);
        // Create the new buckets to be updated
        portLabels.forEach(pl -> {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(pl.port)
                .setEthDst(dstMac)
                .setEthSrc(nodeMacAddr);
            if (pl.edgeLabel != DestinationSet.NO_EDGE_LABEL) {
                tBuilder.pushMpls()
                    .copyTtlOut()
                    .setMpls(MplsLabel.mplsLabel(pl.edgeLabel));
            }
            nextObjBuilder.addTreatment(tBuilder.build());
        });

        log.debug("addToHash in device {}: Adding Bucket with port/label {} "
                + "to nextId {}", deviceId, portLabels, nextId);

        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("addToHash port/label {} addedTo "
                        + "NextObj {} on {}", portLabels, nextId, deviceId),
                (objective, error) ->
                        log.warn("addToHash failed to add port/label {} to"
                                + " NextObj {} on {}: {}", portLabels,
                                 nextId, deviceId, error));
        NextObjective nextObjective = nextObjBuilder.addToExisting(context);
        flowObjectiveService.next(deviceId, nextObjective);
    }

    /**
     * Makes a call to the FlowObjective service to remove buckets from
     * a hash group. User must ensure that all the ports & labels are meant
     * same neighbor (ie. dstMac).
     *
     * @param portLabels a collection of port & label combinations to remove
     *                   from the hash group identified by the nextId
     * @param dstMac destination mac address of next-hop
     * @param nextId id for next-objective from which buckets will be removed
     */
    private void removeFromHashedNextObjective(Collection<PortLabel> portLabels,
                                               MacAddress dstMac, Integer nextId) {
        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder()
                .withType(NextObjective.Type.HASHED) //same as original
                .withId(nextId)
                .fromApp(appId);
        // Create the buckets to be removed
        portLabels.forEach(pl -> {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(pl.port)
                .setEthDst(dstMac)
                .setEthSrc(nodeMacAddr);
            if (pl.edgeLabel != DestinationSet.NO_EDGE_LABEL) {
                tBuilder.pushMpls()
                    .copyTtlOut()
                    .setMpls(MplsLabel.mplsLabel(pl.edgeLabel));
            }
            nextObjBuilder.addTreatment(tBuilder.build());
        });
        log.debug("removeFromHash in device {}: Removing Bucket with port/label"
                + " {} from nextId {}", deviceId, portLabels, nextId);

        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("port/label {} removedFrom NextObj"
                        + " {} on {}", portLabels, nextId, deviceId),
                (objective, error) ->
                log.warn("port/label {} failed to removeFrom NextObj {} on "
                        + "{}: {}", portLabels, nextId, deviceId, error));
        NextObjective nextObjective = nextObjBuilder.removeFromExisting(context);
        flowObjectiveService.next(deviceId, nextObjective);
    }

    /**
     * Checks all the hash-groups in the target-switch meant for the destination
     * switch, and either adds or removes buckets to make the neighbor-set
     * match the given next-hops. Typically called by the master instance of the
     * destination switch, which may be different from the master instance of the
     * target switch where hash-group changes are made.
     *
     * @param targetSw the switch in which the hash groups will be edited
     * @param nextHops the current next hops for the target switch to reach
     *                  the dest sw
     * @param destSw  the destination switch
     * @param revoke true if hash groups need to remove buckets from the
     *                          the groups to match the current next hops
     * @return true if calls are made to edit buckets, or if no edits are required
     */
    public boolean fixHashGroups(DeviceId targetSw, Set<DeviceId> nextHops,
                                 DeviceId destSw, boolean revoke) {
        // temporary storage of keys to be updated
        Map<DestinationSetNextObjectiveStoreKey, Set<DeviceId>> tempStore =
                new HashMap<>();
        boolean foundNextObjective = false, success = true;

        // retrieve hash-groups meant for destSw, which have destinationSets
        // with different neighbors than the given next-hops
        for (DestinationSetNextObjectiveStoreKey dskey : dsNextObjStore.keySet()) {
            if (!dskey.deviceId().equals(targetSw) ||
                    !dskey.destinationSet().getDestinationSwitches().contains(destSw)) {
                continue;
            }
            foundNextObjective = true;
            NextNeighbors nhops = dsNextObjStore.get(dskey);
            Set<DeviceId> currNeighbors = nhops.nextHops(destSw);
            int edgeLabel = dskey.destinationSet().getEdgeLabel(destSw);
            Integer nextId = nhops.nextId();
            if (currNeighbors == null || nextHops == null) {
                log.warn("fixing hash groups but found currNeighbors:{} or nextHops:{}"
                        + " in targetSw:{} for dstSw:{}", currNeighbors,
                         nextHops, targetSw, destSw);
                success &= false;
                continue;
            }

            // some store elements may not be hashed next-objectives - ignore them
            if (isSimpleNextObjective(dskey)) {
                log.debug("Ignoring {} of SIMPLE nextObj for targetSw:{}"
                        + " -> dstSw:{} with current nextHops:{} to new"
                        + " nextHops: {} in nextId:{}",
                          (revoke) ? "removal" : "addition", targetSw, destSw,
                          currNeighbors, nextHops, nextId);
                if ((revoke && !nextHops.isEmpty())
                        || (!revoke && !nextHops.equals(currNeighbors))) {
                    log.debug("Simple next objective cannot be edited to "
                            + "move from {} to {}", currNeighbors, nextHops);
                }
                continue;
            }

            Set<DeviceId> diff;
            if (revoke) {
                diff = Sets.difference(currNeighbors, nextHops);
                log.debug("targetSw:{} -> dstSw:{} in nextId:{} has current next "
                        + "hops:{} ..removing {}", targetSw, destSw, nextId,
                        currNeighbors, diff);
            } else {
                diff = Sets.difference(nextHops, currNeighbors);
                log.debug("targetSw:{} -> dstSw:{} in nextId:{} has current next "
                        + "hops:{} ..adding {}", targetSw, destSw, nextId,
                        currNeighbors, diff);
            }
            boolean suc = updateAllPortsToNextHop(diff, edgeLabel, nextId,
                                                  revoke);
            if (suc) {
                // to update neighbor set with changes made
                if (revoke) {
                    tempStore.put(dskey, Sets.difference(currNeighbors, diff));
                } else {
                    tempStore.put(dskey, Sets.union(currNeighbors, diff));
                }
            }
            success &= suc;
        }

        if (!foundNextObjective) {
            log.debug("Cannot find any nextObjectives for route targetSw:{} "
                    + "-> dstSw:{}", targetSw, destSw);
            return true; // nothing to do, return true so ECMPspg is updated
        }

        // update the dsNextObjectiveStore with new destinationSet to nextId mappings
        for (DestinationSetNextObjectiveStoreKey key : tempStore.keySet()) {
            NextNeighbors currentNextHops = dsNextObjStore.get(key);
            if (currentNextHops == null) {
                log.warn("fixHashGroups could not update global store in "
                        + "device {} .. missing nextNeighbors for key {}",
                        deviceId, key);
                continue;
            }
            Set<DeviceId> newNeighbors = new HashSet<>();
            newNeighbors.addAll(tempStore.get(key));
            Map<DeviceId, Set<DeviceId>> oldDstNextHops =
                    ImmutableMap.copyOf(currentNextHops.dstNextHops());
            currentNextHops.dstNextHops().put(destSw, newNeighbors); //local change
            log.debug("Updating nsNextObjStore target:{} -> dst:{} in key:{} nextId:{}",
                      targetSw, destSw, key, currentNextHops.nextId());
            log.debug("Old dstNextHops: {}", oldDstNextHops);
            log.debug("New dstNextHops: {}", currentNextHops.dstNextHops());
            // update global store
            dsNextObjStore.put(key,
                               new NextNeighbors(currentNextHops.dstNextHops(),
                                                 currentNextHops.nextId()));
        }

        // even if one fails and others succeed, return false so ECMPspg not updated
        return success;
    }

    /**
     * Updates the DestinationSetNextObjectiveStore with any per-destination nexthops
     * that are not already in the store for the given DestinationSet. Note that
     * this method does not remove existing next hops for the destinations in the
     * DestinationSet.
     *
     * @param ds the DestinationSet for which the next hops need to be updated
     * @param newDstNextHops a map of per-destination next hops to update the
     *                          destinationSet with
     * @return true if successful in updating all next hops
     */
    private boolean updateNextHops(DestinationSet ds,
                                  Map<DeviceId, Set<DeviceId>> newDstNextHops) {
        DestinationSetNextObjectiveStoreKey key =
                new DestinationSetNextObjectiveStoreKey(deviceId, ds);
        NextNeighbors currNext = dsNextObjStore.get(key);
        Map<DeviceId, Set<DeviceId>> currDstNextHops = currNext.dstNextHops();

        // add newDstNextHops to currDstNextHops for each dst
        boolean success = true;
        for (DeviceId dstSw : ds.getDestinationSwitches()) {
            Set<DeviceId> currNhops = currDstNextHops.get(dstSw);
            Set<DeviceId> newNhops = newDstNextHops.get(dstSw);
            currNhops = (currNhops == null) ? Sets.newHashSet() : currNhops;
            newNhops = (newNhops == null) ? Sets.newHashSet() : newNhops;
            int edgeLabel = ds.getEdgeLabel(dstSw);
            int nextId = currNext.nextId();

            // new next hops should be added
            boolean suc = updateAllPortsToNextHop(Sets.difference(newNhops, currNhops),
                                                  edgeLabel, nextId, false);
            if (suc) {
                currNhops.addAll(newNhops);
                currDstNextHops.put(dstSw, currNhops); // this is only a local change
            }
            success &= suc;
        }

        if (success) {
            // update global store
            dsNextObjStore.put(key, new NextNeighbors(currDstNextHops,
                                                      currNext.nextId()));
            log.debug("Updated device:{} ds:{} new next-hops: {}", deviceId, ds,
                      dsNextObjStore.get(key));
        }
        return success;
    }

    /**
     * Adds or removes buckets for all ports to a set of neighbor devices. Caller
     * needs to ensure that the  given neighbors are all next hops towards the
     * same destination (represented by the given edgeLabel).
     *
     * @param neighbors set of neighbor device ids
     * @param edgeLabel MPLS label to use in buckets
     * @param nextId the nextObjective to change
     * @param revoke true if buckets need to be removed, false if they need to
     *          be added
     * @return true if successful in adding or removing buckets for all ports
     *                  to the neighbors
     */
    private boolean updateAllPortsToNextHop(Set<DeviceId> neighbors, int edgeLabel,
                                         int nextId, boolean revoke) {
        for (DeviceId neighbor : neighbors) {
            MacAddress neighborMac;
            try {
                neighborMac = deviceConfig.getDeviceMac(neighbor);
            } catch (DeviceConfigNotFoundException e) {
                log.warn(e.getMessage() + " Aborting updateAllPortsToNextHop"
                        + " for nextId:" + nextId);
                return false;
            }
            Collection<PortNumber> portsToNeighbor = devicePortMap.get(neighbor);
            if (portsToNeighbor == null || portsToNeighbor.isEmpty()) {
                log.warn("No ports found in dev:{} for neighbor:{} .. cannot "
                        + "updateAllPortsToNextHop for nextId: {}",
                         deviceId, neighbor, nextId);
                return false;
            }
            List<PortLabel> pl = Lists.newArrayList();
            portsToNeighbor.forEach(p -> pl.add(new PortLabel(p, edgeLabel)));
            if (revoke) {
                log.debug("updateAllPortsToNextHops in device {}: Removing Bucket(s) "
                        + "with Port/Label:{} to next object id {}",
                        deviceId, pl, nextId);
                removeFromHashedNextObjective(pl, neighborMac, nextId);
            } else {
                log.debug("fixHashGroup in device {}: Adding Bucket(s) "
                        + "with Port/Label: {} to next object id {}",
                        deviceId, pl, nextId);
                addToHashedNextObjective(pl, neighborMac, nextId);
            }
        }
        return true;
    }

    /**
     * Returns true if the destination set is meant for swap or multi-labeled
     * packet transport, and MPLS ECMP is not supported.
     *
     * @param dskey the key representing the destination set
     * @return true if destination set is meant for simple next objectives
     */
    boolean isSimpleNextObjective(DestinationSetNextObjectiveStoreKey dskey) {
        return (dskey.destinationSet().notBos() || dskey.destinationSet().swap())
                && !srManager.getMplsEcmp();
    }

    /**
     * Adds or removes a port that has been configured with a vlan to a broadcast group
     * for bridging. Should only be called by the master instance for this device.
     *
     * @param port the port on this device that needs to be added/removed to a bcast group
     * @param vlanId the vlan id corresponding to the broadcast domain/group
     * @param popVlan indicates if packets should be sent out untagged or not out
     *                of the port. If true, indicates an access (untagged) or native vlan
     *                configuration. If false, indicates a trunk (tagged) vlan config.
     * @param portUp true if port is enabled, false if disabled
     */
    public void processEdgePort(PortNumber port, VlanId vlanId,
                                boolean popVlan, boolean portUp) {
        //get the next id for the subnet and edit it.
        Integer nextId = getVlanNextObjectiveId(vlanId);
        if (nextId == -1) {
            if (portUp) {
                log.debug("**Creating flooding group for first port enabled in"
                        + " vlan {} on dev {} port {}", vlanId, deviceId, port);
                createBcastGroupFromVlan(vlanId, Collections.singleton(port));
            } else {
                log.warn("Could not find flooding group for subnet {} on dev:{} when"
                        + " removing port:{}", vlanId, deviceId, port);
            }
            return;
        }

        log.info("**port{} in device {}: {} Bucket with Port {} to"
                + " next-id {}", (portUp) ? "UP" : "DOWN", deviceId,
                                          (portUp) ? "Adding" : "Removing",
                                          port, nextId);
        // Create the bucket to be added or removed
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        if (popVlan) {
            tBuilder.popVlan();
        }
        tBuilder.setOutput(port);

        TrafficSelector metadata =
                DefaultTrafficSelector.builder().matchVlanId(vlanId).build();

        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.BROADCAST).fromApp(appId)
                .addTreatment(tBuilder.build())
                .withMeta(metadata);

        ObjectiveContext context = new DefaultObjectiveContext(
            (objective) -> log.debug("port {} successfully {} NextObj {} on {}",
                                     port, (portUp) ? "addedTo" : "removedFrom",
                                     nextId, deviceId),
            (objective, error) ->
            log.warn("port {} failed to {} NextObj {} on {}: {}",
                     port, (portUp) ? "addTo" : "removeFrom",
                     nextId, deviceId, error));

        NextObjective nextObj = (portUp) ? nextObjBuilder.addToExisting(context)
                                         : nextObjBuilder.removeFromExisting(context);
        log.debug("edgePort processed: Submited next objective {} in device {}",
                  nextId, deviceId);
        flowObjectiveService.next(deviceId, nextObj);
    }

    /**
     * Returns the next objective of type hashed (or simple) associated with the
     * destination set. In addition, updates the existing next-objective if new
     * route-paths found have resulted in the addition of new next-hops to a
     * particular destination. If there is no existing next objective for this
     * destination set, this method would create a next objective and return the
     * nextId. Optionally metadata can be passed in for the creation of the next
     * objective. If the parameter simple is true then a simple next objective
     * is created instead of a hashed one.
     *
     * @param ds destination set
     * @param nextHops a map of per destination next hops
     * @param meta metadata passed into the creation of a Next Objective
     * @param simple if true, a simple next objective will be created instead of
     *            a hashed next objective
     * @return int if found or -1 if there are errors in the creation of the
     *         neighbor set.
     */
    public int getNextObjectiveId(DestinationSet ds,
                                  Map<DeviceId, Set<DeviceId>> nextHops,
                                  TrafficSelector meta, boolean simple) {
        NextNeighbors next = dsNextObjStore.
                get(new DestinationSetNextObjectiveStoreKey(deviceId, ds));
        if (next == null) {
            log.debug("getNextObjectiveId in device{}: Next objective id "
                    + "not found for {} ... creating", deviceId, ds);
            log.trace("getNextObjectiveId: nsNextObjStore contents for device {}: {}",
                      deviceId,
                      dsNextObjStore.entrySet()
                      .stream()
                      .filter((nsStoreEntry) ->
                      (nsStoreEntry.getKey().deviceId().equals(deviceId)))
                      .collect(Collectors.toList()));

            createGroupFromDestinationSet(ds, nextHops, meta, simple);
            next = dsNextObjStore.
                    get(new DestinationSetNextObjectiveStoreKey(deviceId, ds));
            if (next == null) {
                log.warn("getNextObjectiveId: unable to create next objective");
                // failure in creating group
                return -1;
            } else {
                log.debug("getNextObjectiveId in device{}: Next objective id {} "
                    + "created for {}", deviceId, next.nextId(), ds);
            }
        } else {
            log.trace("getNextObjectiveId in device{}: Next objective id {} "
                    + "found for {}", deviceId, next.nextId(), ds);
            // should fix hash groups too if next-hops have changed
            if (!next.dstNextHops().equals(nextHops)) {
                log.debug("Nexthops have changed for dev:{} nextId:{} ..updating",
                          deviceId, next.nextId());
                if (!updateNextHops(ds, nextHops)) {
                    // failure in updating group
                    return -1;
                }
            }
        }
        return next.nextId();
    }

    /**
     * Returns the next objective of type broadcast associated with the vlan,
     * or -1 if no such objective exists. Note that this method does NOT create
     * the next objective as a side-effect. It is expected that is objective is
     * created at startup from network configuration. Typically this is used
     * for L2 flooding within the subnet configured on the switch.
     *
     * @param vlanId vlan id
     * @return int if found or -1
     */
    public int getVlanNextObjectiveId(VlanId vlanId) {
        Integer nextId = vlanNextObjStore.
                get(new VlanNextObjectiveStoreKey(deviceId, vlanId));

        return (nextId != null) ? nextId : -1;
    }

    /**
     * Returns the next objective of type simple associated with the port on the
     * device, given the treatment. Different treatments to the same port result
     * in different next objectives. If no such objective exists, this method
     * creates one (if requested) and returns the id. Optionally metadata can be passed in for
     * the creation of the objective. Typically this is used for L2 and L3 forwarding
     * to compute nodes and containers/VMs on the compute nodes directly attached
     * to the switch.
     *
     * @param portNum the port number for the simple next objective
     * @param treatment the actions to apply on the packets (should include outport)
     * @param meta optional metadata passed into the creation of the next objective
     * @param createIfMissing true if a next object should be created if not found
     * @return int if found or created, -1 if there are errors during the
     *          creation of the next objective.
     */
    public int getPortNextObjectiveId(PortNumber portNum, TrafficTreatment treatment,
                                      TrafficSelector meta, boolean createIfMissing) {
        Integer nextId = portNextObjStore
                .get(new PortNextObjectiveStoreKey(deviceId, portNum, treatment, meta));
        if (nextId != null) {
            return nextId;
        }
        log.debug("getPortNextObjectiveId in device {}: Next objective id "
                + "not found for port: {} .. {}", deviceId, portNum,
                (createIfMissing) ? "creating" : "aborting");
        if (!createIfMissing) {
            return -1;
        }
        // create missing next objective
        createGroupFromPort(portNum, treatment, meta);
        nextId = portNextObjStore.get(new PortNextObjectiveStoreKey(deviceId, portNum,
                                                                    treatment, meta));
        if (nextId == null) {
            log.warn("getPortNextObjectiveId: unable to create next obj"
                    + "for dev:{} port:{}", deviceId, portNum);
            return -1;
        }
        return nextId;
    }

    /**
     * Checks if the next objective ID (group) for the neighbor set exists or not.
     *
     * @param ns neighbor set to check
     * @return true if it exists, false otherwise
     */
    public boolean hasNextObjectiveId(DestinationSet ns) {
        NextNeighbors nextHops = dsNextObjStore.
                get(new DestinationSetNextObjectiveStoreKey(deviceId, ns));
        if (nextHops == null) {
            return false;
        }

        return true;
    }

    private void populateNeighborMaps() {
        Set<Link> outgoingLinks = linkService.getDeviceEgressLinks(deviceId);
        for (Link link : outgoingLinks) {
            if (link.type() != Link.Type.DIRECT) {
                continue;
            }
            addNeighborAtPort(link.dst().deviceId(), link.src().port());
        }
    }

    protected void addNeighborAtPort(DeviceId neighborId,
                                     PortNumber portToNeighbor) {
        // Update DeviceToPort database
        log.debug("Device {} addNeighborAtPort: neighbor {} at port {}",
                  deviceId, neighborId, portToNeighbor);
        Set<PortNumber> ports = Collections
                .newSetFromMap(new ConcurrentHashMap<PortNumber, Boolean>());
        ports.add(portToNeighbor);
        Set<PortNumber> portnums = devicePortMap.putIfAbsent(neighborId, ports);
        if (portnums != null) {
            portnums.add(portToNeighbor);
        }

        // Update portToDevice database
        // should always update as neighbor could have changed on this port
        DeviceId prev = portDeviceMap.put(portToNeighbor, neighborId);
        if (prev != null) {
            log.warn("Device/port: {}/{} previous neighbor: {}, current neighbor: {} ",
                      deviceId, portToNeighbor, prev, neighborId);
        }
    }

    /**
     * Creates a NextObjective for a hash group in this device from a given
     * DestinationSet. If the parameter simple is true, a simple next objective
     * is created instead.
     *
     * @param ds the DestinationSet
     * @param neighbors a map for each destination and its next-hops
     * @param meta metadata passed into the creation of a Next Objective
     * @param simple if true, a simple next objective will be created instead of
     *            a hashed next objective
     */
    public void createGroupFromDestinationSet(DestinationSet ds,
                                              Map<DeviceId, Set<DeviceId>> neighbors,
                                              TrafficSelector meta,
                                              boolean simple) {
        int nextId = flowObjectiveService.allocateNextId();
        NextObjective.Type type = (simple) ? NextObjective.Type.SIMPLE
                                           : NextObjective.Type.HASHED;
        if (neighbors == null || neighbors.isEmpty()) {
            log.warn("createGroupsFromDestinationSet: needs at least one neighbor"
                    + "to create group in dev:{} for ds: {} with next-hops {}",
                    deviceId, ds, neighbors);
            return;
        }

        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder()
                .withId(nextId)
                .withType(type)
                .fromApp(appId);
        if (meta != null) {
            nextObjBuilder.withMeta(meta);
        }

        // create treatment buckets for each neighbor for each dst Device
        // except in the special case where we only want to pick a single
        // neighbor/port for a simple nextObj
        boolean foundSingleNeighbor = false;
        boolean treatmentAdded = false;
        Map<DeviceId, Set<DeviceId>> dstNextHops = new ConcurrentHashMap<>();
        for (DeviceId dst : ds.getDestinationSwitches()) {
            Set<DeviceId> nextHops = neighbors.get(dst);
            if (nextHops == null || nextHops.isEmpty()) {
                continue;
            }

            if (foundSingleNeighbor) {
                break;
            }

            for (DeviceId neighborId : nextHops) {
                if (devicePortMap.get(neighborId) == null) {
                    log.warn("Neighbor {} is not in the port map yet for dev:{}",
                             neighborId, deviceId);
                    return;
                } else if (devicePortMap.get(neighborId).isEmpty()) {
                    log.warn("There are no ports for "
                            + "the Device {} in the port map yet", neighborId);
                    return;
                }

                MacAddress neighborMac;
                try {
                    neighborMac = deviceConfig.getDeviceMac(neighborId);
                } catch (DeviceConfigNotFoundException e) {
                    log.warn(e.getMessage() + " Aborting createGroupsFromDestinationset.");
                    return;
                }
                // For each port to the neighbor, we create a new treatment
                Set<PortNumber> neighborPorts = devicePortMap.get(neighborId);
                // In this case we need a SIMPLE nextObj. We randomly pick a port
                if (simple) {
                    int size = devicePortMap.get(neighborId).size();
                    int index = RandomUtils.nextInt(0, size);
                    neighborPorts = Collections.singleton(
                                        Iterables.get(devicePortMap.get(neighborId),
                                                      index));
                    foundSingleNeighbor = true;
                }

                for (PortNumber sp : neighborPorts) {
                    TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment
                            .builder();
                    tBuilder.setEthDst(neighborMac).setEthSrc(nodeMacAddr);
                    int edgeLabel = ds.getEdgeLabel(dst);
                    if (edgeLabel != DestinationSet.NO_EDGE_LABEL) {
                        if (simple) {
                            // swap label case
                            tBuilder.setMpls(MplsLabel.mplsLabel(edgeLabel));
                        } else {
                            // ecmp with label push case
                            tBuilder.pushMpls().copyTtlOut()
                                    .setMpls(MplsLabel.mplsLabel(edgeLabel));
                        }
                    }
                    if ((ds.getTypeOfDstSet() == DestinationSet.DestinationSetType.SWAP_NOT_BOS) ||
                         (ds.getTypeOfDstSet() == DestinationSet.DestinationSetType.POP_NOT_BOS)) {
                        tBuilder.setVlanId(srManager.PSEUDOWIRE_VLAN);
                    }
                    tBuilder.setOutput(sp);
                    nextObjBuilder.addTreatment(tBuilder.build());
                    treatmentAdded = true;
                    //update store
                    Set<DeviceId> existingNeighbors = dstNextHops.get(dst);
                    if (existingNeighbors == null) {
                        existingNeighbors = new HashSet<>();
                    }
                    existingNeighbors.add(neighborId);
                    dstNextHops.put(dst, existingNeighbors);
                    log.debug("creating treatment for port/label {}/{} in next:{}",
                              sp, edgeLabel, nextId);
                }

                if (foundSingleNeighbor) {
                    break;
                }
            }
        }

        if (!treatmentAdded) {
            log.warn("Could not createGroup from DestinationSet {} without any"
                    + "next hops {}", ds, neighbors);
            return;
        }
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) ->
                log.debug("createGroupsFromDestinationSet installed "
                        + "NextObj {} on {}", nextId, deviceId),
                (objective, error) ->
                log.warn("createGroupsFromDestinationSet failed to install"
                        + " NextObj {} on {}: {}", nextId, deviceId, error)
                );
        NextObjective nextObj = nextObjBuilder.add(context);
        log.debug(".. createGroupsFromDestinationSet: Submitted "
                + "next objective {} in device {}", nextId, deviceId);
        flowObjectiveService.next(deviceId, nextObj);
        //update store
        dsNextObjStore.put(new DestinationSetNextObjectiveStoreKey(deviceId, ds),
                           new NextNeighbors(dstNextHops, nextId));
    }

    /**
     * Creates broadcast groups for all ports in the same subnet for
     * all configured subnets.
     */
    public void createGroupsFromVlanConfig() {
        srManager.getVlanPortMap(deviceId).asMap().forEach((vlanId, ports) -> {
            createBcastGroupFromVlan(vlanId, ports);
        });
    }

    /**
     * Creates a single broadcast group from a given vlan id and list of ports.
     *
     * @param vlanId vlan id
     * @param ports list of ports in the subnet
     */
    public void createBcastGroupFromVlan(VlanId vlanId, Collection<PortNumber> ports) {
        VlanNextObjectiveStoreKey key = new VlanNextObjectiveStoreKey(deviceId, vlanId);

        if (vlanNextObjStore.containsKey(key)) {
            log.debug("Broadcast group for device {} and subnet {} exists",
                      deviceId, vlanId);
            return;
        }

        TrafficSelector metadata =
                DefaultTrafficSelector.builder().matchVlanId(vlanId).build();

        int nextId = flowObjectiveService.allocateNextId();

        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.BROADCAST).fromApp(appId)
                .withMeta(metadata);

        ports.forEach(port -> {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            if (toPopVlan(port, vlanId)) {
                tBuilder.popVlan();
            }
            tBuilder.setOutput(port);
            nextObjBuilder.addTreatment(tBuilder.build());
        });

        ObjectiveContext context = new DefaultObjectiveContext(
            (objective) ->
                log.debug("createBroadcastGroupFromVlan installed "
                        + "NextObj {} on {}", nextId, deviceId),
            (objective, error) ->
                log.warn("createBroadcastGroupFromVlan failed to install"
                        + " NextObj {} on {}: {}", nextId, deviceId, error)
            );
        NextObjective nextObj = nextObjBuilder.add(context);
        flowObjectiveService.next(deviceId, nextObj);
        log.debug("createBcastGroupFromVlan: Submitted next objective {} "
                + "for vlan: {} in device {}", nextId, vlanId, deviceId);

        vlanNextObjStore.put(key, nextId);
    }

    /**
     * Removes a single broadcast group from a given vlan id.
     * The group should be empty.
     * @param deviceId device Id to remove the group
     * @param portNum port number related to the group
     * @param vlanId vlan id of the broadcast group to remove
     * @param popVlan true if the TrafficTreatment involves pop vlan tag action
     */
    public void removeBcastGroupFromVlan(DeviceId deviceId, PortNumber portNum,
                                         VlanId vlanId, boolean popVlan) {
        VlanNextObjectiveStoreKey key = new VlanNextObjectiveStoreKey(deviceId, vlanId);

        if (!vlanNextObjStore.containsKey(key)) {
            log.debug("Broadcast group for device {} and subnet {} does not exist",
                      deviceId, vlanId);
            return;
        }

        TrafficSelector metadata =
                DefaultTrafficSelector.builder().matchVlanId(vlanId).build();

        int nextId = vlanNextObjStore.get(key);

        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.BROADCAST).fromApp(appId)
                .withMeta(metadata);

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        if (popVlan) {
            tBuilder.popVlan();
        }
        tBuilder.setOutput(portNum);
        nextObjBuilder.addTreatment(tBuilder.build());

        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) ->
                        log.debug("removeBroadcastGroupFromVlan removed "
                                          + "NextObj {} on {}", nextId, deviceId),
                (objective, error) ->
                        log.warn("removeBroadcastGroupFromVlan failed to remove "
                                         + " NextObj {} on {}: {}", nextId, deviceId, error)
        );
        NextObjective nextObj = nextObjBuilder.remove(context);
        flowObjectiveService.next(deviceId, nextObj);
        log.debug("removeBcastGroupFromVlan: Submited next objective {} in device {}",
                  nextId, deviceId);

        vlanNextObjStore.remove(key, nextId);
    }

    /**
     * Determine if we should pop given vlan before sending packets to the given port.
     *
     * @param portNumber port number
     * @param vlanId vlan id
     * @return true if the vlan id is not contained in any vlanTagged config
     */
    private boolean toPopVlan(PortNumber portNumber, VlanId vlanId) {
        return srManager.interfaceService
                .getInterfacesByPort(new ConnectPoint(deviceId, portNumber))
                .stream().noneMatch(intf -> intf.vlanTagged().contains(vlanId));
    }

    /**
     * Create simple next objective for a single port. The treatments can include
     * all outgoing actions that need to happen on the packet.
     *
     * @param portNum  the outgoing port on the device
     * @param treatment the actions to apply on the packets (should include outport)
     * @param meta optional data to pass to the driver
     */
    public void createGroupFromPort(PortNumber portNum, TrafficTreatment treatment,
                                    TrafficSelector meta) {
        int nextId = flowObjectiveService.allocateNextId();
        PortNextObjectiveStoreKey key = new PortNextObjectiveStoreKey(
                                                deviceId, portNum, treatment, meta);

        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.SIMPLE)
                .addTreatment(treatment)
                .fromApp(appId)
                .withMeta(meta);

        ObjectiveContext context = new DefaultObjectiveContext(
            (objective) ->
                log.debug("createGroupFromPort installed "
                        + "NextObj {} on {}", nextId, deviceId),
            (objective, error) ->
                log.warn("createGroupFromPort failed to install"
                        + " NextObj {} on {}: {}", nextId, deviceId, error)
            );
        NextObjective nextObj = nextObjBuilder.add(context);
        flowObjectiveService.next(deviceId, nextObj);
        log.debug("createGroupFromPort: Submited next objective {} in device {} "
                + "for port {}", nextId, deviceId, portNum);

        portNextObjStore.put(key, nextId);
    }

    /**
     * Removes simple next objective for a single port.
     *
     * @param deviceId device id that has the port to deal with
     * @param portNum the outgoing port on the device
     * @param vlanId vlan id associated with the port
     * @param popVlan true if POP_VLAN action is applied on the packets, false otherwise
     */
    public void removePortNextObjective(DeviceId deviceId, PortNumber portNum, VlanId vlanId, boolean popVlan) {
        TrafficSelector.Builder mbuilder = DefaultTrafficSelector.builder();
        mbuilder.matchVlanId(vlanId);

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        tbuilder.immediate().setOutput(portNum);
        if (popVlan) {
            tbuilder.immediate().popVlan();
        }

        int portNextObjId = srManager.getPortNextObjectiveId(deviceId, portNum,
                                                             tbuilder.build(), mbuilder.build(), false);

        PortNextObjectiveStoreKey key = new PortNextObjectiveStoreKey(
                deviceId, portNum, tbuilder.build(), mbuilder.build());
        if (portNextObjId != -1 && portNextObjStore.containsKey(key)) {
            NextObjective.Builder nextObjBuilder = DefaultNextObjective
                    .builder().withId(portNextObjId)
                    .withType(NextObjective.Type.SIMPLE).fromApp(appId);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("removePortNextObjective removes NextObj {} on {}",
                                             portNextObjId, deviceId),
                    (objective, error) ->
                            log.warn("removePortNextObjective failed to remove NextObj {} on {}: {}",
                                     portNextObjId, deviceId, error));
            NextObjective nextObjective = nextObjBuilder.remove(context);
            log.info("**removePortNextObjective: Submitted "
                             + "next objective {} in device {}",
                     portNextObjId, deviceId);
            flowObjectiveService.next(deviceId, nextObjective);

            portNextObjStore.remove(key);
        }
    }
    /**
     * Removes groups for the next objective ID given.
     *
     * @param objectiveId next objective ID to remove
     * @return true if succeeds, false otherwise
     */
    public boolean removeGroup(int objectiveId) {
        for (Map.Entry<DestinationSetNextObjectiveStoreKey, NextNeighbors> e :
                dsNextObjStore.entrySet()) {
            if (e.getValue().nextId() != objectiveId) {
                continue;
            }
            // Right now it is just used in TunnelHandler
            // remember in future that PW transit groups could
            // be Indirect groups
            NextObjective.Builder nextObjBuilder = DefaultNextObjective
                    .builder().withId(objectiveId)
                    .withType(NextObjective.Type.HASHED).fromApp(appId);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("RemoveGroup removes NextObj {} on {}",
                            objectiveId, deviceId),
                    (objective, error) ->
                            log.warn("RemoveGroup failed to remove NextObj {} on {}: {}",
                                    objectiveId, deviceId, error));
            NextObjective nextObjective = nextObjBuilder.remove(context);
            log.info("**removeGroup: Submited "
                    + "next objective {} in device {}",
                    objectiveId, deviceId);
            flowObjectiveService.next(deviceId, nextObjective);

            dsNextObjStore.remove(e.getKey());
            return true;
        }

        return false;
    }
    /**
     * Remove simple next objective for a single port. The treatments can include
     * all outgoing actions that need to happen on the packet.
     *
     * @param portNum  the outgoing port on the device
     * @param treatment the actions applied on the packets (should include outport)
     * @param meta optional data to pass to the driver
     */
    public void removeGroupFromPort(PortNumber portNum, TrafficTreatment treatment,
                                    TrafficSelector meta) {
        PortNextObjectiveStoreKey key = new PortNextObjectiveStoreKey(
                deviceId, portNum, treatment, meta);
        Integer nextId = portNextObjStore.get(key);

        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.SIMPLE)
                .addTreatment(treatment)
                .fromApp(appId)
                .withMeta(meta);

        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) ->
                        log.info("removeGroupFromPort installed "
                                          + "NextObj {} on {}", nextId, deviceId),
                (objective, error) ->
                        log.warn("removeGroupFromPort failed to install"
                                         + " NextObj {} on {}: {}", nextId, deviceId, error)
        );
        NextObjective nextObj = nextObjBuilder.remove(context);
        flowObjectiveService.next(deviceId, nextObj);
        log.info("removeGroupFromPort: Submitted next objective {} in device {} "
                          + "for port {}", nextId, deviceId, portNum);

        portNextObjStore.remove(key);
    }

    /**
     * Removes all groups from all next objective stores.
     */
    /*public void removeAllGroups() {
        for (Map.Entry<NeighborSetNextObjectiveStoreKey, NextNeighbors> entry:
                nsNextObjStore.entrySet()) {
            removeGroup(entry.getValue().nextId());
        }
        for (Map.Entry<PortNextObjectiveStoreKey, Integer> entry:
                portNextObjStore.entrySet()) {
            removeGroup(entry.getValue());
        }
        for (Map.Entry<VlanNextObjectiveStoreKey, Integer> entry:
                vlanNextObjStore.entrySet()) {
            removeGroup(entry.getValue());
        }
    }*/ //XXX revisit

    /**
     * Triggers a one time bucket verification operation on all hash groups
     * on this device.
     */
    public void triggerBucketCorrector() {
        BucketCorrector bc = new BucketCorrector();
        bc.run();
    }

    /**
     * Modifies L2IG bucket when the interface configuration is updated, especially
     * when the interface has same VLAN ID but the VLAN type is changed (e.g., from
     * vlan-tagged [10] to vlan-untagged 10), which requires changes on
     * TrafficTreatment in turn.
     *
     * @param portNumber the port on this device that needs to be updated
     * @param vlanId the vlan id corresponding to this port
     * @param pushVlan indicates if packets should be sent out untagged or not out
     *                 from the port. If true, updated TrafficTreatment involves
     *                 pop vlan tag action. If false, updated TrafficTreatment
     *                 does not involve pop vlan tag action.
     */
    public void updateL2InterfaceGroupBucket(PortNumber portNumber, VlanId vlanId, boolean pushVlan) {
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        if (pushVlan) {
            tBuilder.popVlan();
        }
        tBuilder.setOutput(portNumber);

        TrafficSelector metadata =
                DefaultTrafficSelector.builder().matchVlanId(vlanId).build();

        int nextId = getVlanNextObjectiveId(vlanId);

        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.SIMPLE).fromApp(appId)
                .addTreatment(tBuilder.build())
                .withMeta(metadata);

        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("port {} successfully updated NextObj {} on {}",
                                         portNumber, nextId, deviceId),
                (objective, error) ->
                        log.warn("port {} failed to updated NextObj {} on {}: {}",
                                 portNumber, nextId, deviceId, error));

        flowObjectiveService.next(deviceId, nextObjBuilder.modify(context));
    }

    /**
     * Adds a single port to the L2FG or removes it from the L2FG.
     *
     * @param vlanId the vlan id corresponding to this port
     * @param portNum the port on this device to be updated
     * @param nextId the next objective ID for the given vlan id
     * @param install if true, adds the port to L2FG. If false, removes it from L2FG.
     */
    public void updateGroupFromVlanConfiguration(VlanId vlanId, PortNumber portNum, int nextId, boolean install) {
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        if (toPopVlan(portNum, vlanId)) {
            tBuilder.popVlan();
        }
        tBuilder.setOutput(portNum);

        TrafficSelector metadata =
                DefaultTrafficSelector.builder().matchVlanId(vlanId).build();

        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.BROADCAST).fromApp(appId)
                .addTreatment(tBuilder.build())
                .withMeta(metadata);

        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("port {} successfully removedFrom NextObj {} on {}",
                                         portNum, nextId, deviceId),
                (objective, error) ->
                        log.warn("port {} failed to removedFrom NextObj {} on {}: {}",
                                 portNum, nextId, deviceId, error));

        if (install) {
            flowObjectiveService.next(deviceId, nextObjBuilder.addToExisting(context));
        } else {
            flowObjectiveService.next(deviceId, nextObjBuilder.removeFromExisting(context));
        }
    }

    /**
     * Performs bucket verification operation for all hash groups in this device.
     * Checks RouteHandler to ensure that routing is stable before attempting
     * verification. Verification involves creating a nextObjective with
     * operation VERIFY for existing next objectives in the store, and passing
     * it to the driver. It is the driver that actually performs the verification
     * by adding or removing buckets to match the verification next objective
     * created here.
     */
    protected final class BucketCorrector implements Runnable {
        Integer nextId;

        BucketCorrector() {
            this.nextId = null;
        }

        BucketCorrector(Integer nextId) {
            this.nextId = nextId;
        }

        @Override
        public void run() {
            if (!srManager.mastershipService.isLocalMaster(deviceId)) {
                return;
            }
            DefaultRoutingHandler rh = srManager.getRoutingHandler();
            if (rh == null) {
                return;
            }
            if (!rh.isRoutingStable()) {
                return;
            }
            rh.acquireRoutingLock();
            try {
                log.trace("running bucket corrector for dev: {}", deviceId);
                Set<DestinationSetNextObjectiveStoreKey> dsKeySet = dsNextObjStore.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                        // Filter out PW transit groups or include them if MPLS ECMP is supported
                        .filter(entry -> !entry.getKey().destinationSet().notBos() ||
                                (entry.getKey().destinationSet().notBos() && srManager.getMplsEcmp()))
                        // Filter out simple SWAP groups or include them if MPLS ECMP is supported
                        .filter(entry -> !entry.getKey().destinationSet().swap() ||
                                (entry.getKey().destinationSet().swap() && srManager.getMplsEcmp()))
                        .map(entry -> entry.getKey())
                        .collect(Collectors.toSet());
                for (DestinationSetNextObjectiveStoreKey dsKey : dsKeySet) {
                    NextNeighbors next = dsNextObjStore.get(dsKey);
                    if (next == null) {
                        continue;
                    }
                    int nid = next.nextId();
                    if (nextId != null && nextId != nid) {
                        continue;
                    }
                    log.trace("bkt-corr: dsNextObjStore for device {}: {}",
                              deviceId, dsKey, next);
                    TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder();
                    metabuilder.matchVlanId(INTERNAL_VLAN);
                    NextObjective.Builder nextObjBuilder = DefaultNextObjective.builder()
                            .withId(nid)
                            .withType(NextObjective.Type.HASHED)
                            .withMeta(metabuilder.build())
                            .fromApp(appId);

                    next.dstNextHops().forEach((dstDev, nextHops) -> {
                        int edgeLabel = dsKey.destinationSet().getEdgeLabel(dstDev);
                        nextHops.forEach(neighbor -> {
                            MacAddress neighborMac;
                            try {
                                neighborMac = deviceConfig.getDeviceMac(neighbor);
                            } catch (DeviceConfigNotFoundException e) {
                                log.warn(e.getMessage() + " Aborting neighbor"
                                        + neighbor);
                                return;
                            }
                            devicePortMap.get(neighbor).forEach(port -> {
                                log.trace("verify in device {} nextId {}: bucket with"
                                        + " port/label {}/{} to dst {} via {}",
                                        deviceId, nid, port, edgeLabel,
                                        dstDev, neighbor);
                                nextObjBuilder
                                    .addTreatment(treatmentBuilder(port,
                                                                   neighborMac,
                                                                   dsKey.destinationSet().swap(),
                                                                   edgeLabel));
                            });
                        });
                    });

                    NextObjective nextObjective = nextObjBuilder.verify();
                    flowObjectiveService.next(deviceId, nextObjective);
                }
            } finally {
                rh.releaseRoutingLock();
            }

        }

        TrafficTreatment treatmentBuilder(PortNumber outport, MacAddress dstMac,
                                          boolean swap, int edgeLabel) {
            TrafficTreatment.Builder tBuilder =
                    DefaultTrafficTreatment.builder();
            tBuilder.setOutput(outport)
                .setEthDst(dstMac)
                .setEthSrc(nodeMacAddr);
            if (edgeLabel != DestinationSet.NO_EDGE_LABEL) {
                if (swap) {
                    // swap label case
                    tBuilder.setMpls(MplsLabel.mplsLabel(edgeLabel));
                } else {
                    // ecmp with label push case
                    tBuilder.pushMpls()
                        .copyTtlOut()
                        .setMpls(MplsLabel.mplsLabel(edgeLabel));
                }
            }
            return tBuilder.build();
        }
    }

}
