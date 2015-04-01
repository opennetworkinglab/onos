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
package org.onosproject.grouphandler;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;

/**
 * Default ECMP group handler creation module. This
 * component creates a set of ECMP groups for every neighbor
 * that this device is connected to based on whether the
 * current device is an edge device or a transit device.
 */
public class DefaultGroupHandler {
    protected final Logger log = getLogger(getClass());

    protected final DeviceId deviceId;
    protected final ApplicationId appId;
    protected final DeviceProperties deviceConfig;
    protected final List<Integer> allSegmentIds;
    protected final int nodeSegmentId;
    protected final boolean isEdgeRouter;
    protected final MacAddress nodeMacAddr;
    protected LinkService linkService;
    protected GroupService groupService;

    protected HashMap<DeviceId, Set<PortNumber>> devicePortMap =
            new HashMap<DeviceId, Set<PortNumber>>();
    protected HashMap<PortNumber, DeviceId> portDeviceMap =
            new HashMap<PortNumber, DeviceId>();

    private GroupListener listener = new InternalGroupListener();
    protected KryoNamespace.Builder kryo = new KryoNamespace.Builder()
    .register(URI.class)
    .register(HashSet.class)
    .register(DeviceId.class)
    .register(PortNumber.class)
    .register(NeighborSet.class)
    .register(PolicyGroupIdentifier.class)
    .register(PolicyGroupParams.class)
    .register(GroupBucketIdentifier.class)
    .register(GroupBucketIdentifier.BucketOutputType.class);

    protected DefaultGroupHandler(DeviceId deviceId,
                               ApplicationId appId,
                               DeviceProperties config,
                               LinkService linkService,
                               GroupService groupService) {
        this.deviceId = checkNotNull(deviceId);
        this.appId = checkNotNull(appId);
        this.deviceConfig = checkNotNull(config);
        this.linkService = checkNotNull(linkService);
        this.groupService = checkNotNull(groupService);
        allSegmentIds = checkNotNull(config.getAllDeviceSegmentIds());
        nodeSegmentId = config.getSegmentId(deviceId);
        isEdgeRouter = config.isEdgeDevice(deviceId);
        nodeMacAddr = checkNotNull(config.getDeviceMac(deviceId));

        this.groupService.addListener(listener);

        populateNeighborMaps();
    }

    /**
     * Creates a group handler object based on the type of device. If
     * device is of edge type it returns edge group handler, else it
     * returns transit group handler.
     *
     * @param deviceId device identifier
     * @param appId application identifier
     * @param config interface to retrieve the device properties
     * @param linkService link service object
     * @param groupService group service object
     * @return default group handler type
     */
    public static DefaultGroupHandler createGroupHandler(DeviceId deviceId,
                                                  ApplicationId appId,
                                                  DeviceProperties config,
                                                  LinkService linkService,
                                                  GroupService groupService) {
        if (config.isEdgeDevice(deviceId)) {
            return new DefaultEdgeGroupHandler(deviceId,
                                               appId,
                                               config,
                                               linkService,
                                               groupService);
        } else {
            return new DefaultTransitGroupHandler(deviceId,
                                                  appId,
                                                  config,
                                                  linkService,
                                                  groupService);
        }
    }

    /**
     * Creates the auto created groups for this device based on the
     * current snapshot of the topology.
     */
    //Empty implementations to be overridden by derived classes
    public void createGroups() {
    }

    /**
     * Performs group creation or update procedures when a new link
     * is discovered on this device.
     *
     * @param newLink new neighbor link
     */
    public void linkUp(Link newLink) {

        if (newLink.type() != Link.Type.DIRECT) {
            log.warn("linkUp: unknown link type");
            return;
        }


        if (!newLink.src().deviceId().equals(deviceId)) {
            log.warn("linkUp: deviceId{} doesn't match with link src{}",
                     deviceId,
                     newLink.src().deviceId());
            return;
        }

        log.debug("Device {} linkUp at local port {} to neighbor {}",
                  deviceId, newLink.src().port(), newLink.dst().deviceId());
        if (devicePortMap.get(newLink.dst().deviceId()) == null) {
            // New Neighbor
            newNeighbor(newLink);
        } else {
            // Old Neighbor
            newPortToExistingNeighbor(newLink);
        }
    }

    /**
     * Performs group recovery procedures when a port goes down
     * on this device.
     *
     * @param port port number that has gone down
     */
    public void portDown(PortNumber port) {
        if (portDeviceMap.get(port) == null) {
            log.warn("portDown: unknown port");
            return;
        }
        log.debug("Device {} portDown {} to neighbor {}",
                  deviceId, port, portDeviceMap.get(port));
        Set<NeighborSet> nsSet = computeImpactedNeighborsetForPortEvent(
                                                     portDeviceMap.get(port),
                                                     devicePortMap.keySet());
        for (NeighborSet ns : nsSet) {
            // Create the bucket to be removed
            TrafficTreatment.Builder tBuilder =
                    DefaultTrafficTreatment.builder();
            tBuilder.setOutput(port)
                    .setEthDst(deviceConfig.getDeviceMac(
                         portDeviceMap.get(port)))
                    .setEthSrc(nodeMacAddr);
            if (ns.getEdgeLabel() != NeighborSet.NO_EDGE_LABEL) {
                tBuilder.pushMpls()
                    .setMpls(MplsLabel.mplsLabel(ns.getEdgeLabel()));
            }
            GroupBucket removeBucket = DefaultGroupBucket.
                    createSelectGroupBucket(tBuilder.build());
            GroupBuckets removeBuckets = new GroupBuckets(
                                               Arrays.asList(removeBucket));
            log.debug("portDown in device{}: "
                    + "groupService.removeBucketsFromGroup "
                    + "for neighborset{}", deviceId, ns);
            groupService.removeBucketsFromGroup(deviceId,
                                                getGroupKey(ns),
                                                removeBuckets,
                                                getGroupKey(ns),
                                                appId);
        }

        devicePortMap.get(portDeviceMap.get(port)).remove(port);
        portDeviceMap.remove(port);
    }

    /**
     * Returns a group associated with the key.
     *
     * @param key cookie associated with the group
     * @return group if found or null
     */
    public Group getGroup(GroupKey key) {
        return groupService.getGroup(deviceId, key);
    }

    //Empty implementation
    protected void newNeighbor(Link newLink) {
    }

    //Empty implementation
    protected void newPortToExistingNeighbor(Link newLink) {
    }

    //Empty implementation
    protected Set<NeighborSet> computeImpactedNeighborsetForPortEvent(
                                    DeviceId impactedNeighbor,
                                    Set<DeviceId> updatedNeighbors) {
        return null;
    }

    private void populateNeighborMaps() {
        Set<Link> outgoingLinks = linkService.getDeviceEgressLinks(deviceId);
        for (Link link:outgoingLinks) {
            if (link.type() != Link.Type.DIRECT) {
                continue;
            }
            addNeighborAtPort(link.dst().deviceId(), link.src().port());
        }
    }

    protected void addNeighborAtPort(DeviceId neighborId, PortNumber portToNeighbor) {
        // Update DeviceToPort database
        log.debug("Device {} addNeighborAtPort: neighbor {} at port {}",
                  deviceId, neighborId, portToNeighbor);
        if (devicePortMap.get(neighborId) != null) {
            devicePortMap.get(neighborId).add(portToNeighbor);
        } else {
            Set<PortNumber> ports = new HashSet<PortNumber>();
            ports.add(portToNeighbor);
            devicePortMap.put(neighborId, ports);
        }

        // Update portToDevice database
        if (portDeviceMap.get(portToNeighbor) == null) {
            portDeviceMap.put(portToNeighbor, neighborId);
        }
    }

    protected Set<Set<DeviceId>>
            getPowerSetOfNeighbors(Set<DeviceId> neighbors) {
        List<DeviceId> list = new ArrayList<DeviceId>(neighbors);
        Set<Set<DeviceId>> sets = new HashSet<Set<DeviceId>>();
        // get the number of elements in the neighbors
        int elements = list.size();
        // the number of members of a power set is 2^n
        // including the empty set
        int powerElements = (1 << elements);

        // run a binary counter for the number of power elements
        // NOTE: Exclude empty set
        for (long i = 1; i < powerElements; i++) {
            Set<DeviceId> neighborSubSet = new HashSet<DeviceId>();
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
        return (deviceConfig.getSegmentId(deviceId) == sId);
    }

    protected List<Integer> getSegmentIdsTobePairedWithNeighborSet(
                                   Set<DeviceId> neighbors) {

        List<Integer> nsSegmentIds = new ArrayList<Integer>();

        // Always pair up with no edge label
        //If (neighbors.size() == 1) {
        nsSegmentIds.add(-1);
        //}

        // Filter out SegmentIds matching with the
        // nodes in the combo
        for (Integer sId : allSegmentIds) {
            if (sId.equals(nodeSegmentId)) {
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

    protected void createGroupsFromNeighborsets(Set<NeighborSet> nsSet) {
        for (NeighborSet ns : nsSet) {
            // Create the bucket array from the neighbor set
            List<GroupBucket> buckets = new ArrayList<GroupBucket>();
            for (DeviceId d : ns.getDeviceIds()) {
                for (PortNumber sp : devicePortMap.get(d)) {
                    TrafficTreatment.Builder tBuilder =
                            DefaultTrafficTreatment.builder();
                    tBuilder.setOutput(sp)
                            .setEthDst(deviceConfig.getDeviceMac(d))
                            .setEthSrc(nodeMacAddr);
                    if (ns.getEdgeLabel() != NeighborSet.NO_EDGE_LABEL) {
                        tBuilder.pushMpls()
                                .setMpls(MplsLabel.
                                         mplsLabel(ns.getEdgeLabel()));
                    }
                    buckets.add(DefaultGroupBucket.createSelectGroupBucket(
                                                                tBuilder.build()));
                }
            }
            GroupBuckets groupBuckets = new GroupBuckets(buckets);
            GroupDescription newGroupDesc = new DefaultGroupDescription(
                                      deviceId,
                                      Group.Type.SELECT,
                                      groupBuckets,
                                      getGroupKey(ns),
                                      appId);
            log.debug("createGroupsFromNeighborsets: "
                    + "groupService.addGroup for neighborset{}", ns);
            groupService.addGroup(newGroupDesc);
        }
    }

    protected void handleGroupEvent(GroupEvent event) {
        switch (event.type()) {
        case GROUP_ADDED:
            log.debug("Received GROUP_ADDED from group service "
                    + "for device {} with group key{} with id{}",
                      event.subject().deviceId(),
                      event.subject().appCookie(),
                      event.subject().id());
            break;
        case GROUP_UPDATED:
            log.trace("Received GROUP_UPDATED from group service "
                    + "for device {} with group key{} with id{}",
                      event.subject().deviceId(),
                      event.subject().appCookie(),
                      event.subject().id());
            break;
        case GROUP_REMOVED:
            log.debug("Received GROUP_REMOVED from group service "
                    + "for device {} with group key{} with id{}",
                      event.subject().deviceId(),
                      event.subject().appCookie(),
                      event.subject().id());
            break;
        default:
            break;
        }
    }

    private class InternalGroupListener implements GroupListener {

        @Override
        public void event(GroupEvent event) {
            handleGroupEvent(event);
        }
    }

    public GroupKey getGroupKey(Object obj) {
        return new DefaultGroupKey(kryo.build().serialize(obj));
    }

}
