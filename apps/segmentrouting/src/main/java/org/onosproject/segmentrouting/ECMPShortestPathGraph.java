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
package org.onosproject.segmentrouting;

import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;

/**
 * This class creates bandwidth constrained breadth first tree and returns paths
 * from root Device to leaf Devicees which satisfies the bandwidth condition. If
 * bandwidth parameter is not specified, the normal breadth first tree will be
 * calculated. The paths are snapshot paths at the point of the class
 * instantiation.
 */
public class ECMPShortestPathGraph {

    static class DeviceDistance {

        private DeviceId deviceId;
        private int distance;

        public DeviceDistance(DeviceId id, int dist) {
            this.deviceId = id;
            this.distance = dist;
        }

        public DeviceId deviceId() {
            return deviceId;
        }

        public int distance() {
            return distance;
        }
    }

    static class PQsort implements Comparator<DeviceDistance> {

        public int compare(DeviceDistance devDist1, DeviceDistance devDist2) {
            return devDist1.distance() - devDist2.distance();
        }
    }

    PQsort pqs = new PQsort();

    LinkedList<DeviceId> deviceQueue = new LinkedList<>();
    PriorityQueue<DeviceDistance> devicePriorityQueue = new PriorityQueue<DeviceDistance>(1, pqs);
    LinkedList<Integer> distanceQueue = new LinkedList<>();
    HashMap<DeviceId, Integer> deviceSearched = new HashMap<>();
    HashMap<DeviceId, ArrayList<Link>> upstreamLinks = new HashMap<>();
    HashMap<DeviceId, ArrayList<Path>> paths = new HashMap<>();
    HashMap<Integer, ArrayList<DeviceId>> distanceDeviceMap = new HashMap<>();
    DeviceId rootDevice;
    private SegmentRoutingManager srManager;
    private static final Logger log = LoggerFactory
            .getLogger(ECMPShortestPathGraph.class);

    /**
     * Constructor.
     *
     * @param rootDevice root of the BFS tree
     * @param linkListToAvoid link list to avoid
     * @param deviceIdListToAvoid device list to avoid
     */
    public ECMPShortestPathGraph(DeviceId rootDevice, List<String> deviceIdListToAvoid,
                                 List<Link> linkListToAvoid) {
        this.rootDevice = rootDevice;
        calcECMPShortestPathGraph(deviceIdListToAvoid, linkListToAvoid);
    }

    /**
     * Constructor.
     *
     * @param rootDevice root of the BFS tree
     * @param srManager SegmentRoutingManager object
     */
    public ECMPShortestPathGraph(DeviceId rootDevice, SegmentRoutingManager srManager,
                                 boolean linkHasWeight) {
        this.rootDevice = rootDevice;
        this.srManager = srManager;
        if (!linkHasWeight) {
            calcECMPShortestPathGraph();
        } else {
            calcECMPShortestPathGraphWithWeights();
        }
    }

    /**
     * Calculates the shortest path using Dijkstra's algorithm.
     */
    private void calcECMPShortestPathGraphWithWeights() {
        DeviceDistance rootDeviceObj = new DeviceDistance(rootDevice, 0);
        devicePriorityQueue.add(rootDeviceObj);
        int currDistance = 0;
        deviceSearched.put(rootDeviceObj.deviceId(), rootDeviceObj.distance());

        while (devicePriorityQueue.peek() != null) {
            DeviceDistance sw = devicePriorityQueue.poll();
            DeviceId prevSw = null;
            currDistance = sw.distance();

            for (Link link : srManager.linkService.getDeviceEgressLinks(sw.deviceId())) {
                DeviceId reachedDevice = link.dst().deviceId();
                if ((prevSw != null)
                        && (prevSw.equals(reachedDevice))) {
                    /* Ignore LAG links between the same set of Devicees */
                    continue;
                }

                Integer distance = deviceSearched.get(reachedDevice);
                if ((distance != null) && (distance.intValue() < (currDistance + link.getWeight()))) {
                    continue;
                }

                /* First time visiting this Device node or update the distance */
                if (distance == null || distance.intValue() >= (currDistance + link.getWeight())) {
                    DeviceDistance reachedDeviceObj = new DeviceDistance(reachedDevice,
                                                                         currDistance + link.getWeight());
                    devicePriorityQueue.add(reachedDeviceObj);
                    deviceSearched.put(reachedDeviceObj.deviceId(), reachedDeviceObj.distance());

                    if (distance != null) {
                        ArrayList<DeviceId> distanceSwArray = distanceDeviceMap.get(distance);
                        for (int i = 0; i < distanceSwArray.size(); i++) {
                            if (distanceSwArray.get(i).equals(reachedDevice)) {
                                distanceSwArray.remove(i);
                            }
                        }
                    }

                    ArrayList<DeviceId> distanceSwArray = distanceDeviceMap
                            .get(currDistance + link.getWeight());
                    if (distanceSwArray == null) {
                        distanceSwArray = new ArrayList<DeviceId>();
                        distanceSwArray.add(reachedDevice);
                        distanceDeviceMap.put(currDistance + link.getWeight(), distanceSwArray);
                    } else {
                        distanceSwArray.add(reachedDevice);
                    }
                }

                ArrayList<Link> upstreamLinkArray =
                        upstreamLinks.get(reachedDevice);
                if (upstreamLinkArray == null) {
                    upstreamLinkArray = new ArrayList<Link>();
                    upstreamLinkArray.add(copyDefaultLink(link));
                    upstreamLinks.put(reachedDevice, upstreamLinkArray);
                } else {
                    /* ECMP links */
                    if (distance.intValue() > (currDistance + link.getWeight())) {
                        upstreamLinkArray.clear();
                    }
                    upstreamLinkArray.add(copyDefaultLink(link));
                }
            }
        }
    }

    /**
     * Calculates the BFS tree using any provided constraints and Intents.
     */
    private void calcECMPShortestPathGraph() {
        deviceQueue.add(rootDevice);
        int currDistance = 0;
        distanceQueue.add(currDistance);
        deviceSearched.put(rootDevice, currDistance);
        while (!deviceQueue.isEmpty()) {
            DeviceId sw = deviceQueue.poll();
            DeviceId prevSw = null;
            currDistance = distanceQueue.poll();

            for (Link link : srManager.linkService.getDeviceEgressLinks(sw)) {
                DeviceId reachedDevice = link.dst().deviceId();
                if ((prevSw != null)
                        && (prevSw.equals(reachedDevice))) {
                    /* Ignore LAG links between the same set of Devicees */
                    continue;
                } else  {
                    prevSw = reachedDevice;
                }

                Integer distance = deviceSearched.get(reachedDevice);
                if ((distance != null) && (distance < (currDistance + 1))) {
                    continue;
                }
                if (distance == null) {
                    /* First time visiting this Device node */
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
                    /* ECMP links */
                    upstreamLinkArray.add(copyDefaultLink(link));
                }
            }
        }
    }

    /**
     * Calculates the BFS tree using any provided constraints and Intents.
     */
    private void calcECMPShortestPathGraph(List<String> deviceIdListToAvoid, List<Link> linksToAvoid) {
        deviceQueue.add(rootDevice);
        int currDistance = 0;
        distanceQueue.add(currDistance);
        deviceSearched.put(rootDevice, currDistance);
        boolean foundLinkToAvoid = false;
        while (!deviceQueue.isEmpty()) {
            DeviceId sw = deviceQueue.poll();
            DeviceId prevSw = null;
            currDistance = distanceQueue.poll();
            for (Link link : srManager.linkService.getDeviceEgressLinks(sw)) {
                for (Link linkToAvoid: linksToAvoid) {
                    // TODO: equls should work
                    //if (link.equals(linkToAvoid)) {
                    if (linkContains(link, linksToAvoid)) {
                        foundLinkToAvoid = true;
                        break;
                    }
                }
                if (foundLinkToAvoid) {
                    foundLinkToAvoid = false;
                    continue;
                }
                DeviceId reachedDevice = link.dst().deviceId();
                if (deviceIdListToAvoid.contains(reachedDevice.toString())) {
                    continue;
                }
                if ((prevSw != null)
                        && (prevSw.equals(reachedDevice))) {
                    /* Ignore LAG links between the same set of Devicees */
                    continue;
                } else {
                    prevSw = reachedDevice;
                }

                Integer distance = deviceSearched.get(reachedDevice);
                if ((distance != null) && (distance < (currDistance + 1))) {
                    continue;
                }
                if (distance == null) {
                    /* First time visiting this Device node */
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
                    upstreamLinks.put(reachedDevice, upstreamLinkArray);
                } else {
                    /* ECMP links */
                    upstreamLinkArray.add(copyDefaultLink(link));
                }
            }
        }
    }


    private boolean linkContains(Link link, List<Link> links) {

        DeviceId srcDevice1 = link.src().deviceId();
        DeviceId dstDevice1 = link.dst().deviceId();
        long srcPort1 = link.src().port().toLong();
        long dstPort1 = link.dst().port().toLong();

        for (Link link2: links) {
            DeviceId srcDevice2 = link2.src().deviceId();
            DeviceId dstDevice2 = link2.dst().deviceId();
            long srcPort2 = link2.src().port().toLong();
            long dstPort2 = link2.dst().port().toLong();

            if (srcDevice1.toString().equals(srcDevice2.toString())
                    && dstDevice1.toString().equals(dstDevice2.toString())
                    && srcPort1 == srcPort2 && dstPort1 == dstPort2) {
                return true;
            }
        }

        return false;
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
            sofarPath = new DefaultPath(ProviderId.NONE, sofarLinks, 0);
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
     * @return the hash table of Devicees learned in multiple Dijkstra
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
     * Return the complete info of the computed ECMP paths for each Device
     * learned in multiple iterations from the root Device.
     *
     * @return the hash table of Devicees learned in multiple Dijkstra
     *         iterations and corresponding ECMP paths in terms of Devicees to
     *         be traversed to it from the root Device
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
        DefaultLink defaultLink = new DefaultLink(src.providerId(), src.src(),
                src.dst(), src.type(), src.annotations());

        return defaultLink;
    }

    @Override
    public String toString() {
        StringBuilder sBuilder = new StringBuilder();
        for (Device device: srManager.deviceService.getDevices()) {
            if (device.id() != rootDevice) {
                sBuilder.append("Paths from" + rootDevice + " to " + device.id() + "\r\n");
                ArrayList<Path> paths = getECMPPaths(device.id());
                if (paths != null) {
                    for (Path path : paths) {
                        for (Link link : path.links()) {
                            sBuilder.append(" : " + link.src() + " -> " + link.dst());
                        }
                    }
                }
            }
        }
        return sBuilder.toString();
    }
}

