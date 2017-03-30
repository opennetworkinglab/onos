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
package org.onosproject.segmentrouting.grouphandler;


import com.google.common.collect.Iterables;
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
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceProperties;
import org.onosproject.segmentrouting.storekey.NeighborSetNextObjectiveStoreKey;
import org.onosproject.segmentrouting.storekey.PortNextObjectiveStoreKey;
import org.onosproject.segmentrouting.storekey.VlanNextObjectiveStoreKey;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.slf4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.segmentrouting.SegmentRoutingManager.INTERNAL_VLAN;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default ECMP group handler creation module. This component creates a set of
 * ECMP groups for every neighbor that this device is connected to based on
 * whether the current device is an edge device or a transit device.
 */
public class DefaultGroupHandler {
    protected static final Logger log = getLogger(DefaultGroupHandler.class);

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
    // local store for neighbor-device-ids and the set of ports on this device
    // that connect to the same neighbor
    protected ConcurrentHashMap<DeviceId, Set<PortNumber>> devicePortMap =
            new ConcurrentHashMap<>();
    // local store for ports on this device connected to neighbor-device-id
    protected ConcurrentHashMap<PortNumber, DeviceId> portDeviceMap =
            new ConcurrentHashMap<>();
    // distributed store for (device+neighborset) mapped to next-id
    protected EventuallyConsistentMap<NeighborSetNextObjectiveStoreKey, Integer>
            nsNextObjStore = null;
    // distributed store for (device+subnet-ip-prefix) mapped to next-id
    protected EventuallyConsistentMap<VlanNextObjectiveStoreKey, Integer>
            vlanNextObjStore = null;
    // distributed store for (device+port+treatment) mapped to next-id
    protected EventuallyConsistentMap<PortNextObjectiveStoreKey, Integer>
            portNextObjStore = null;
    private SegmentRoutingManager srManager;

    protected KryoNamespace.Builder kryo = new KryoNamespace.Builder()
            .register(URI.class).register(HashSet.class)
            .register(DeviceId.class).register(PortNumber.class)
            .register(NeighborSet.class).register(PolicyGroupIdentifier.class)
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
        this.nsNextObjStore = srManager.nsNextObjStore();
        this.vlanNextObjStore = srManager.vlanNextObjStore();
        this.portNextObjStore = srManager.portNextObjStore();
        this.srManager = srManager;

        populateNeighborMaps();
    }

    /**
     * Creates a group handler object based on the type of device. If device is
     * of edge type it returns edge group handler, else it returns transit group
     * handler.
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
        // handle possible exception in the caller
        if (config.isEdgeDevice(deviceId)) {
            return new DefaultEdgeGroupHandler(deviceId, appId, config,
                                               linkService,
                                               flowObjService,
                                               srManager
                                               );
        } else {
            return new DefaultTransitGroupHandler(deviceId, appId, config,
                                                  linkService,
                                                  flowObjService,
                                                  srManager);
        }
    }

    /**
     * Creates the auto created groups for this device based on the current
     * snapshot of the topology.
     */
    // Empty implementations to be overridden by derived classes
    public void createGroups() {
    }

    /**
     * Performs group creation or update procedures when a new link is
     * discovered on this device.
     *
     * @param newLink new neighbor link
     * @param isMaster true if local instance is the master
     *
     */
    public void linkUp(Link newLink, boolean isMaster) {

        if (newLink.type() != Link.Type.DIRECT) {
            // NOTE: A DIRECT link might be transiently marked as INDIRECT
            //       if BDDP is received before LLDP. We can safely ignore that
            //       until the LLDP is received and the link is marked as DIRECT.
            log.info("Ignore link {}->{}. Link type is {} instead of DIRECT.",
                    newLink.src(), newLink.dst(), newLink.type());
            return;
        }

        if (!newLink.src().deviceId().equals(deviceId)) {
            log.warn("linkUp: deviceId{} doesn't match with link src {}",
                     deviceId, newLink.src().deviceId());
            return;
        }

        log.info("* LinkUP: Device {} linkUp at local port {} to neighbor {}", deviceId,
                 newLink.src().port(), newLink.dst().deviceId());
        // ensure local state is updated even if linkup is aborted later on
        addNeighborAtPort(newLink.dst().deviceId(),
                          newLink.src().port());

        MacAddress dstMac;
        try {
            dstMac = deviceConfig.getDeviceMac(newLink.dst().deviceId());
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting linkUp.");
            return;
        }

        /*if (devicePortMap.get(newLink.dst().deviceId()) == null) {
            // New Neighbor
            newNeighbor(newLink);
        } else {
            // Old Neighbor
            newPortToExistingNeighbor(newLink);
        }*/
        Set<NeighborSet> nsSet = nsNextObjStore.keySet()
                .stream()
                .filter((nsStoreEntry) -> (nsStoreEntry.deviceId().equals(deviceId)))
                .map((nsStoreEntry) -> (nsStoreEntry.neighborSet()))
                .filter((ns) -> (ns.getDeviceIds()
                        .contains(newLink.dst().deviceId())))
                .collect(Collectors.toSet());
        log.trace("linkUp: nsNextObjStore contents for device {}:",
                deviceId,
                nsSet);
        for (NeighborSet ns : nsSet) {
            Integer nextId = nsNextObjStore.
                    get(new NeighborSetNextObjectiveStoreKey(deviceId, ns));
            if (nextId != null && isMaster) {
                // Create the new bucket to be updated
                TrafficTreatment.Builder tBuilder =
                        DefaultTrafficTreatment.builder();
                tBuilder.setOutput(newLink.src().port())
                    .setEthDst(dstMac)
                    .setEthSrc(nodeMacAddr);
                if (ns.getEdgeLabel() != NeighborSet.NO_EDGE_LABEL) {
                    tBuilder.pushMpls()
                        .copyTtlOut()
                        .setMpls(MplsLabel.mplsLabel(ns.getEdgeLabel()));
                }
                // setup metadata to pass to nextObjective - indicate the vlan on egress
                // if needed by the switch pipeline. Since hashed next-hops are always to
                // other neighboring routers, there is no subnet assigned on those ports.
                TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder();
                metabuilder.matchVlanId(INTERNAL_VLAN);

                NextObjective.Builder nextObjBuilder = DefaultNextObjective.builder()
                        .withId(nextId)
                        .withType(NextObjective.Type.HASHED)
                        .addTreatment(tBuilder.build())
                        .withMeta(metabuilder.build())
                        .fromApp(appId);
                log.info("**linkUp in device {}: Adding Bucket "
                        + "with Port {} to next object id {}",
                        deviceId,
                        newLink.src().port(),
                        nextId);

                ObjectiveContext context = new DefaultObjectiveContext(
                        (objective) -> log.debug("LinkUp addedTo NextObj {} on {}",
                                nextId, deviceId),
                        (objective, error) ->
                                log.warn("LinkUp failed to addTo NextObj {} on {}: {}",
                                        nextId, deviceId, error));
                NextObjective nextObjective = nextObjBuilder.addToExisting(context);
                flowObjectiveService.next(deviceId, nextObjective);
            } else if (isMaster) {
                log.warn("linkUp in device {}, but global store has no record "
                        + "for neighbor-set {}", deviceId, ns);
            }
        }
    }

    /**
     * Performs hash group recovery procedures when a switch-to-switch
     * port goes down on this device.
     *
     * @param port port number that has gone down
     * @param isMaster true if local instance is the master
     */
    public void portDown(PortNumber port, boolean isMaster) {
        if (portDeviceMap.get(port) == null) {
            log.warn("portDown: unknown port");
            return;
        }

        MacAddress dstMac;
        try {
            dstMac = deviceConfig.getDeviceMac(portDeviceMap.get(port));
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting portDown.");
            return;
        }

        log.debug("Device {} portDown {} to neighbor {}", deviceId, port,
                  portDeviceMap.get(port));
        /*Set<NeighborSet> nsSet = computeImpactedNeighborsetForPortEvent(portDeviceMap
                                                                                .get(port),
                                                                        devicePortMap
                                                                                .keySet());*/
        Set<NeighborSet> nsSet = nsNextObjStore.keySet()
                .stream()
                .filter((nsStoreEntry) -> (nsStoreEntry.deviceId().equals(deviceId)))
                .map((nsStoreEntry) -> (nsStoreEntry.neighborSet()))
                .filter((ns) -> (ns.getDeviceIds()
                        .contains(portDeviceMap.get(port))))
                .collect(Collectors.toSet());
        log.debug("portDown: nsNextObjStore contents for device {}:{}",
                  deviceId, nsSet);
        for (NeighborSet ns : nsSet) {
            NeighborSetNextObjectiveStoreKey nsStoreKey =
                    new NeighborSetNextObjectiveStoreKey(deviceId, ns);
            Integer nextId = nsNextObjStore.get(nsStoreKey);
            if (nextId != null && isMaster) {
                log.info("**portDown in device {}: Removing Bucket "
                        + "with Port {} to next object id {}",
                        deviceId,
                        port,
                        nextId);
                // Create the bucket to be removed
                TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment
                        .builder();
                tBuilder.setOutput(port)
                    .setEthDst(dstMac)
                    .setEthSrc(nodeMacAddr);
                if (ns.getEdgeLabel() != NeighborSet.NO_EDGE_LABEL) {
                    tBuilder.pushMpls()
                        .copyTtlOut()
                        .setMpls(MplsLabel.mplsLabel(ns.getEdgeLabel()));
                }
                NextObjective.Builder nextObjBuilder = DefaultNextObjective
                        .builder()
                        .withType(NextObjective.Type.HASHED) //same as original
                        .withId(nextId)
                        .fromApp(appId)
                        .addTreatment(tBuilder.build());
                ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("portDown removedFrom NextObj {} on {}",
                                             nextId, deviceId),
                    (objective, error) ->
                    log.warn("portDown failed to removeFrom NextObj {} on {}: {}",
                             nextId, deviceId, error));
                NextObjective nextObjective = nextObjBuilder.
                        removeFromExisting(context);

                flowObjectiveService.next(deviceId, nextObjective);
            }
        }

        devicePortMap.get(portDeviceMap.get(port)).remove(port);
        portDeviceMap.remove(port);
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
                        + " subnet {} on dev {} port {}", vlanId, deviceId, port);
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
     * Returns the next objective of type hashed associated with the neighborset.
     * If there is no next objective for this neighborset, this method
     * would create a next objective and return. Optionally metadata can be
     * passed in for the creation of the next objective.
     *
     * @param ns neighborset
     * @param meta metadata passed into the creation of a Next Objective
     * @param isBos if Bos is set
     * @return int if found or -1 if there are errors in the creation of the
     *          neighbor set.
     */
    public int getNextObjectiveId(NeighborSet ns, TrafficSelector meta, boolean isBos) {
        Integer nextId = nsNextObjStore.
                get(new NeighborSetNextObjectiveStoreKey(deviceId, ns));
        if (nextId == null) {
            log.trace("getNextObjectiveId in device{}: Next objective id "
                    + "not found for {} and creating", deviceId, ns);
            log.trace("getNextObjectiveId: nsNextObjStore contents for device {}: {}",
                      deviceId,
                      nsNextObjStore.entrySet()
                      .stream()
                      .filter((nsStoreEntry) ->
                      (nsStoreEntry.getKey().deviceId().equals(deviceId)))
                      .collect(Collectors.toList()));
            createGroupsFromNeighborsets(Collections.singleton(ns), meta, isBos);
            nextId = nsNextObjStore.
                    get(new NeighborSetNextObjectiveStoreKey(deviceId, ns));
            if (nextId == null) {
                log.warn("getNextObjectiveId: unable to create next objective");
                return -1;
            } else {
                log.debug("getNextObjectiveId in device{}: Next objective id {} "
                    + "created for {}", deviceId, nextId, ns);
            }
        } else {
            log.trace("getNextObjectiveId in device{}: Next objective id {} "
                    + "found for {}", deviceId, nextId, ns);
        }
        return nextId;
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
    public boolean hasNextObjectiveId(NeighborSet ns) {
        Integer nextId = nsNextObjStore.
                get(new NeighborSetNextObjectiveStoreKey(deviceId, ns));
        if (nextId == null) {
            return false;
        }

        return true;
    }

    // Empty implementation
    protected void newNeighbor(Link newLink) {
    }

    // Empty implementation
    protected void newPortToExistingNeighbor(Link newLink) {
    }

    // Empty implementation
    protected Set<NeighborSet>
        computeImpactedNeighborsetForPortEvent(DeviceId impactedNeighbor,
                                               Set<DeviceId> updatedNeighbors) {
        return null;
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
        DeviceId prev = portDeviceMap.putIfAbsent(portToNeighbor, neighborId);
        if (prev != null) {
            log.warn("Device: {} port: {} has neighbor: {}. NOT updating "
                    + "to neighbor: {}", deviceId, portToNeighbor, prev, neighborId);
        }
    }

    protected Set<Set<DeviceId>> getPowerSetOfNeighbors(Set<DeviceId> neighbors) {
        List<DeviceId> list = new ArrayList<>(neighbors);
        Set<Set<DeviceId>> sets = new HashSet<>();
        // get the number of elements in the neighbors
        int elements = list.size();
        // the number of members of a power set is 2^n
        // including the empty set
        int powerElements = (1 << elements);

        // run a binary counter for the number of power elements
        // NOTE: Exclude empty set
        for (long i = 1; i < powerElements; i++) {
            Set<DeviceId> neighborSubSet = new HashSet<>();
            for (int j = 0; j < elements; j++) {
                if ((i >> j) % 2 == 1) {
                    neighborSubSet.add(list.get(j));
                }
            }
            sets.add(neighborSubSet);
        }
        return sets;
    }

    private boolean isSegmentIdSameAsNodeSegmentId(DeviceId deviceId, int sId) {
        int segmentId;
        try {
            // IPv6 sid is not inserted. this part of the code is not used for now.
            segmentId = deviceConfig.getIPv4SegmentId(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting isSegmentIdSameAsNodeSegmentId.");
            return false;
        }

        return segmentId == sId;
    }

    protected List<Integer> getSegmentIdsTobePairedWithNeighborSet(Set<DeviceId> neighbors) {

        List<Integer> nsSegmentIds = new ArrayList<>();

        // Always pair up with no edge label
        // If (neighbors.size() == 1) {
        nsSegmentIds.add(-1);
        // }

        // Filter out SegmentIds matching with the
        // nodes in the combo
        for (Integer sId : allSegmentIds) {
            if (sId.equals(this.ipv4NodeSegmentId)) {
                continue;
            }
            boolean filterOut = false;
            // Check if the edge label being set is of
            // any node in the Neighbor set
            for (DeviceId deviceId : neighbors) {
                if (isSegmentIdSameAsNodeSegmentId(deviceId, sId)) {
                    filterOut = true;
                    break;
                }
            }
            if (!filterOut) {
                nsSegmentIds.add(sId);
            }
        }
        return nsSegmentIds;
    }

    /**
     * Creates hash groups from a set of NeighborSet given.
     *
     * @param nsSet a set of NeighborSet
     * @param meta metadata passed into the creation of a Next Objective
     * @param isBos if BoS is set
     */
    public void createGroupsFromNeighborsets(Set<NeighborSet> nsSet,
                                             TrafficSelector meta,
                                             boolean isBos) {
        for (NeighborSet ns : nsSet) {
            int nextId = flowObjectiveService.allocateNextId();
            NextObjective.Type type = NextObjective.Type.HASHED;
            Set<DeviceId> neighbors = ns.getDeviceIds();
            // If Bos == False and MPLS-ECMP == false, we have
            // to use simple group and we will pick a single neighbor.
            if (!isBos && !srManager.getMplsEcmp()) {
                type = NextObjective.Type.SIMPLE;
                neighbors = Collections.singleton(ns.getFirstNeighbor());
            }
            NextObjective.Builder nextObjBuilder = DefaultNextObjective
                    .builder()
                    .withId(nextId)
                    .withType(type)
                    .fromApp(appId);
            // For each neighbor, we have to update the sent actions
            for (DeviceId neighborId : neighbors) {
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
                    log.warn(e.getMessage() + " Aborting createGroupsFromNeighborsets.");
                    return;
                }
                // For each port, we have to create a new treatment
                Set<PortNumber> neighborPorts = devicePortMap.get(neighborId);
                // In this case we are using a SIMPLE group. We randomly pick a port
                if (!isBos && !srManager.getMplsEcmp()) {
                    int size = devicePortMap.get(neighborId).size();
                    int index = RandomUtils.nextInt(0, size);
                    neighborPorts = Collections.singleton(
                            Iterables.get(devicePortMap.get(neighborId), index)
                    );
                }
                for (PortNumber sp : neighborPorts) {
                    TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment
                            .builder();
                    tBuilder.setEthDst(neighborMac)
                            .setEthSrc(nodeMacAddr);
                    if (ns.getEdgeLabel() != NeighborSet.NO_EDGE_LABEL) {
                        tBuilder.pushMpls()
                                .copyTtlOut()
                                .setMpls(MplsLabel.mplsLabel(ns.getEdgeLabel()));
                    }
                    tBuilder.setOutput(sp);
                    nextObjBuilder.addTreatment(tBuilder.build());
                }
            }
            if (meta != null) {
                nextObjBuilder.withMeta(meta);
            }

            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) ->
                            log.debug("createGroupsFromNeighborsets installed "
                                    + "NextObj {} on {}", nextId, deviceId),
                    (objective, error) ->
                            log.warn("createGroupsFromNeighborsets failed to install"
                                    + " NextObj {} on {}: {}", nextId, deviceId, error)
                    );
            NextObjective nextObj = nextObjBuilder.add(context);
            log.debug("**createGroupsFromNeighborsets: Submited "
                    + "next objective {} in device {}",
                    nextId, deviceId);
            flowObjectiveService.next(deviceId, nextObj);
            nsNextObjStore.put(new NeighborSetNextObjectiveStoreKey(deviceId, ns),
                               nextId);
        }
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
        log.debug("createBcastGroupFromVlan: Submited next objective {} in device {}",
                  nextId, deviceId);

        vlanNextObjStore.put(key, nextId);
    }

    /**
     * Determine if we should pop given vlan before sending packets to the given port.
     *
     * @param portNumber port number
     * @param vlanId vlan id
     * @return true if the vlan id is not contained in any vlanTagged config
     */
    private boolean toPopVlan(PortNumber portNumber, VlanId vlanId) {
        return srManager.interfaceService.getInterfacesByPort(new ConnectPoint(deviceId, portNumber))
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
     * Removes groups for the next objective ID given.
     *
     * @param objectiveId next objective ID to remove
     * @return true if succeeds, false otherwise
     */
    public boolean removeGroup(int objectiveId) {

        if (nsNextObjStore.containsValue(objectiveId)) {
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

            for (Map.Entry<NeighborSetNextObjectiveStoreKey, Integer> entry: nsNextObjStore.entrySet()) {
                if (entry.getValue().equals(objectiveId)) {
                    nsNextObjStore.remove(entry.getKey());
                    break;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Removes all groups from all next objective stores.
     */
    public void removeAllGroups() {
        for (Map.Entry<NeighborSetNextObjectiveStoreKey, Integer> entry:
                nsNextObjStore.entrySet()) {
            removeGroup(entry.getValue());
        }
        for (Map.Entry<PortNextObjectiveStoreKey, Integer> entry:
                portNextObjStore.entrySet()) {
            removeGroup(entry.getValue());
        }
        for (Map.Entry<VlanNextObjectiveStoreKey, Integer> entry:
                vlanNextObjStore.entrySet()) {
            removeGroup(entry.getValue());
        }
        // should probably clean local stores port-neighbor
    }
}
