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
package org.onosproject.segmentrouting;

import org.onlab.graph.ScalarWeight;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * This class creates breadth-first-search (BFS) tree for a given root device
 * and returns paths from the root Device to leaf Devices (target devices).
 * The paths are snapshot paths at the point of the class instantiation.
 */
public class EcmpShortestPathGraph {
    LinkedList<DeviceId> deviceQueue = new LinkedList<>();
    LinkedList<Integer> distanceQueue = new LinkedList<>();
    HashMap<DeviceId, Integer> deviceSearched = new HashMap<>();
    HashMap<DeviceId, ArrayList<Link>> upstreamLinks = new HashMap<>();
    HashMap<DeviceId, ArrayList<Path>> paths = new HashMap<>();
    HashMap<Integer, ArrayList<DeviceId>> distanceDeviceMap = new HashMap<>();
    DeviceId rootDevice;
    private SegmentRoutingManager srManager;
    private static final Logger log = LoggerFactory.getLogger(EcmpShortestPathGraph.class);

    /**
     * Constructor.
     *
     * @param rootDevice root of the BFS tree
     * @param srManager SegmentRoutingManager object
     */
    public EcmpShortestPathGraph(DeviceId rootDevice, SegmentRoutingManager srManager) {
        this.rootDevice = rootDevice;
        this.srManager = srManager;
        calcECMPShortestPathGraph();
    }

    /**
     * Calculates the BFS tree.
     */
   private void calcECMPShortestPathGraph() {
        deviceQueue.add(rootDevice);
        int currDistance = 0;
        distanceQueue.add(currDistance);
        deviceSearched.put(rootDevice, currDistance);
        while (!deviceQueue.isEmpty()) {
            DeviceId sw = deviceQueue.poll();
            Set<DeviceId> prevSw = Sets.newHashSet();
            currDistance = distanceQueue.poll();

            for (Link link : srManager.linkHandler.getDeviceEgressLinks(sw)) {
                if (srManager.linkHandler.avoidLink(link)) {
                    continue;
                }
                DeviceId reachedDevice = link.dst().deviceId();
                if (prevSw.contains(reachedDevice)) {
                    // Ignore LAG links between the same set of Devices
                    continue;
                } else  {
                    prevSw.add(reachedDevice);
                }

                Integer distance = deviceSearched.get(reachedDevice);
                if ((distance != null) && (distance < (currDistance + 1))) {
                    continue;
                }
                if (distance == null) {
                    // First time visiting this Device node
                    deviceQueue.add(reachedDevice);
                    distanceQueue.add(currDistance + 1);
                    deviceSearched.put(reachedDevice, currDistance + 1);

                    ArrayList<DeviceId> distanceSwArray = distanceDeviceMap
                            .get(currDistance + 1);
                    if (distanceSwArray == null) {
                        distanceSwArray = new ArrayList<>();
                        distanceSwArray.add(reachedDevice);
                        distanceDeviceMap.put(currDistance + 1, distanceSwArray);
                    } else {
                        distanceSwArray.add(reachedDevice);
                    }
                }

                ArrayList<Link> upstreamLinkArray =
                        upstreamLinks.get(reachedDevice);
                if (upstreamLinkArray == null) {
                    upstreamLinkArray = new ArrayList<>();
                    upstreamLinkArray.add(copyDefaultLink(link));
                    //upstreamLinkArray.add(link);
                    upstreamLinks.put(reachedDevice, upstreamLinkArray);
                } else {
                    // ECMP links
                    upstreamLinkArray.add(copyDefaultLink(link));
                }
            }
        }
    }

    private void getDFSPaths(DeviceId dstDeviceDeviceId, Path path, ArrayList<Path> paths) {
        DeviceId rootDeviceDeviceId = rootDevice;
        for (Link upstreamLink : upstreamLinks.get(dstDeviceDeviceId)) {
            /* Deep clone the path object */
            Path sofarPath;
            ArrayList<Link> sofarLinks = new ArrayList<>();
            if (path != null && !path.links().isEmpty()) {
                sofarLinks.addAll(path.links());
            }
            sofarLinks.add(upstreamLink);
            sofarPath = new DefaultPath(ProviderId.NONE, sofarLinks, ScalarWeight.toWeight(0));
            if (upstreamLink.src().deviceId().equals(rootDeviceDeviceId)) {
                paths.add(sofarPath);
                return;
            } else {
                getDFSPaths(upstreamLink.src().deviceId(), sofarPath, paths);
            }
        }
    }

    /**
     * Return root Device for the graph.
     *
     * @return root Device
     */
    public DeviceId getRootDevice() {
        return rootDevice;
    }

    /**
     * Return the computed ECMP paths from the root Device to a given Device in
     * the network.
     *
     * @param targetDevice the target Device
     * @return the list of ECMP Paths from the root Device to the target Device
     */
    public ArrayList<Path> getECMPPaths(DeviceId targetDevice) {
        ArrayList<Path> pathArray = paths.get(targetDevice);
        if (pathArray == null && deviceSearched.containsKey(
                targetDevice)) {
            pathArray = new ArrayList<>();
            DeviceId sw = targetDevice;
            getDFSPaths(sw, null, pathArray);
            paths.put(targetDevice, pathArray);
        }
        return pathArray;
    }

    /**
     * Return the complete info of the computed ECMP paths for each Device
     * learned in multiple iterations from the root Device.
     *
     * @return the hash table of Devices learned in multiple Dijkstra
     *         iterations and corresponding ECMP paths to it from the root
     *         Device
     */
    public HashMap<Integer, HashMap<DeviceId,
            ArrayList<Path>>> getCompleteLearnedDeviceesAndPaths() {

        HashMap<Integer, HashMap<DeviceId, ArrayList<Path>>> pathGraph = new HashMap<>();

        for (Integer itrIndx : distanceDeviceMap.keySet()) {
            HashMap<DeviceId, ArrayList<Path>> swMap = new HashMap<>();
            for (DeviceId sw : distanceDeviceMap.get(itrIndx)) {
                swMap.put(sw, getECMPPaths(sw));
            }
            pathGraph.put(itrIndx, swMap);
        }

        return pathGraph;
    }

    /**
     * Returns the complete info of the computed ECMP paths for each target device
     * learned in multiple iterations from the root Device. The computed info
     * returned is per iteration (Integer key of outer HashMap). In each
     * iteration, for the target devices reached (DeviceId key of inner HashMap),
     * the ECMP paths are detailed (2D array).
     *
     * @return the hash table of target Devices learned in multiple Dijkstra
     *         iterations and corresponding ECMP paths in terms of Devices to
     *         be traversed (via) from the root Device to the target Device
     */
    public HashMap<Integer, HashMap<DeviceId,
            ArrayList<ArrayList<DeviceId>>>> getAllLearnedSwitchesAndVia() {

        HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> deviceViaMap = new HashMap<>();

        for (Integer itrIndx : distanceDeviceMap.keySet()) {
            HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swMap = new HashMap<>();

            for (DeviceId sw : distanceDeviceMap.get(itrIndx)) {
                ArrayList<ArrayList<DeviceId>> swViaArray = new ArrayList<>();
                for (Path path : getECMPPaths(sw)) {
                    ArrayList<DeviceId> swVia = new ArrayList<>();
                    for (Link link : path.links()) {
                        if (link.src().deviceId().equals(rootDevice)) {
                            /* No need to add the root Device again in
                             * the Via list
                             */
                            continue;
                        }
                        swVia.add(link.src().deviceId());
                    }
                    swViaArray.add(swVia);
                }
                swMap.put(sw, swViaArray);
            }
            deviceViaMap.put(itrIndx, swMap);
        }
        return deviceViaMap;
    }


    private Link copyDefaultLink(Link link) {
        DefaultLink src = (DefaultLink) link;
        DefaultLink defaultLink = DefaultLink.builder()
                .providerId(src.providerId())
                .src(src.src())
                .dst(src.dst())
                .type(src.type())
                .annotations(src.annotations())
                .build();

        return defaultLink;
    }

    @Override
    public String toString() {
        StringBuilder sBuilder = new StringBuilder();
        for (Device device: srManager.deviceService.getDevices()) {
            if (!device.id().equals(rootDevice)) {
                sBuilder.append("\r\n  Paths from " + rootDevice + " to "
                                + device.id());
                ArrayList<Path> paths = getECMPPaths(device.id());
                if (paths != null) {
                    for (Path path : paths) {
                        sBuilder.append("\r\n       == "); // equal cost paths delimiter
                        for (int i = path.links().size() - 1; i >= 0; i--) {
                            Link link = path.links().get(i);
                            sBuilder.append(" : " + link.src() + " -> " + link.dst());
                        }
                    }
                } else {
                    sBuilder.append("\r\n       == no paths");
                }
            }
        }
        return sBuilder.toString();
    }
}

