/*
 *  Copyright 2016-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.model.topo;

import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.model.ServiceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.ui.model.topo.UiLinkId.uiLinkId;

/**
 * Represents the overall network topology.
 */
public class UiTopology extends UiElement {

    private static final String INDENT_1 = "  ";
    private static final String INDENT_2 = "    ";
    private static final String EOL = String.format("%n");

    private static final String E_UNMAPPED =
            "Attempting to retrieve unmapped {}: {}";

    private static final String DEFAULT_TOPOLOGY_ID = "TOPOLOGY-0";

    private static final Logger log = LoggerFactory.getLogger(UiTopology.class);

    private static final Comparator<UiClusterMember> CLUSTER_MEMBER_COMPARATOR =
            Comparator.comparing(UiClusterMember::idAsString);


    // top level mappings of topology elements by ID
    private final Map<NodeId, UiClusterMember> cnodeLookup = new HashMap<>();
    private final Map<RegionId, UiRegion> regionLookup = new HashMap<>();
    private final Map<DeviceId, UiDevice> deviceLookup = new HashMap<>();
    private final Map<HostId, UiHost> hostLookup = new HashMap<>();
    private final Map<UiLinkId, UiDeviceLink> devLinkLookup = new HashMap<>();
    private final Map<UiLinkId, UiEdgeLink> edgeLinkLookup = new HashMap<>();

    // a cache of the computed synthetic links, keyed by ID of original UiLink
    private final Map<UiLinkId, UiSynthLink> synthMap = new HashMap<>();

    // a container for devices, hosts, etc. belonging to no region
    private final UiRegion nullRegion = new UiRegion(this, null);

    final ServiceBundle services;

    /**
     * Creates a new UI topology backed by the specified service bundle.
     *
     * @param services service bundle
     */
    public UiTopology(ServiceBundle services) {
        this.services = services;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("#cnodes", clusterMemberCount())
                .add("#regions", regionCount())
                .add("#devices", deviceLookup.size())
                .add("#hosts", hostLookup.size())
                .add("#dev-links", devLinkLookup.size())
                .add("#edge-links", edgeLinkLookup.size())
                .add("#synth-links", synthMap.size())
                .toString();
    }

    @Override
    public String idAsString() {
        return DEFAULT_TOPOLOGY_ID;
    }

    /**
     * Clears the topology state; that is, drops all regions, devices, hosts,
     * links, and cluster members.
     */
    public void clear() {
        log.debug("clearing topology model");
        cnodeLookup.clear();
        regionLookup.clear();
        deviceLookup.clear();
        hostLookup.clear();
        devLinkLookup.clear();
        edgeLinkLookup.clear();

        synthMap.clear();

        nullRegion.destroy();
    }


    /**
     * Returns all the cluster members, sorted by their ID.
     *
     * @return all cluster members
     */
    public List<UiClusterMember> allClusterMembers() {
        List<UiClusterMember> members = new ArrayList<>(cnodeLookup.values());
        Collections.sort(members, CLUSTER_MEMBER_COMPARATOR);
        return members;
    }

    /**
     * Returns the cluster member with the given identifier, or null if no
     * such member exists.
     *
     * @param id cluster node identifier
     * @return corresponding UI cluster member
     */
    public UiClusterMember findClusterMember(NodeId id) {
        return cnodeLookup.get(id);
    }

    /**
     * Adds the given cluster member to the topology model.
     *
     * @param member cluster member to add
     */
    public void add(UiClusterMember member) {
        cnodeLookup.put(member.id(), member);
    }

    /**
     * Removes the given cluster member from the topology model.
     *
     * @param member cluster member to remove
     */
    public void remove(UiClusterMember member) {
        UiClusterMember m = cnodeLookup.remove(member.id());
        if (m != null) {
            m.destroy();
        }
    }

    /**
     * Returns the number of members in the cluster.
     *
     * @return number of cluster members
     */
    public int clusterMemberCount() {
        return cnodeLookup.size();
    }


    /**
     * Returns all regions in the model (except the
     * {@link #nullRegion() null region}).
     *
     * @return all regions
     */
    public Set<UiRegion> allRegions() {
        return new HashSet<>(regionLookup.values());
    }

    /**
     * Returns a reference to the null-region. That is, the container for
     * devices, hosts, and links that belong to no region.
     *
     * @return the null-region
     */
    public UiRegion nullRegion() {
        return nullRegion;
    }

    /**
     * Returns the region with the specified identifier, or null if
     * no such region exists.
     *
     * @param id region identifier
     * @return corresponding UI region
     */
    public UiRegion findRegion(RegionId id) {
        return UiRegion.NULL_ID.equals(id) ? nullRegion() : regionLookup.get(id);
    }

    /**
     * Adds the given region to the topology model.
     *
     * @param uiRegion region to add
     */
    public void add(UiRegion uiRegion) {
        regionLookup.put(uiRegion.id(), uiRegion);
    }

    /**
     * Removes the given region from the topology model.
     *
     * @param uiRegion region to remove
     */
    public void remove(UiRegion uiRegion) {
        UiRegion r = regionLookup.remove(uiRegion.id());
        if (r != null) {
            r.destroy();
        }
    }

    /**
     * Returns the number of regions configured in the topology.
     *
     * @return number of regions
     */
    public int regionCount() {
        return regionLookup.size();
    }

    /**
     * Returns all devices in the model.
     *
     * @return all devices
     */
    public Set<UiDevice> allDevices() {
        return new HashSet<>(deviceLookup.values());
    }

    /**
     * Returns the device with the specified identifier, or null if
     * no such device exists.
     *
     * @param id device identifier
     * @return corresponding UI device
     */
    public UiDevice findDevice(DeviceId id) {
        return deviceLookup.get(id);
    }

    /**
     * Adds the given device to the topology model.
     *
     * @param uiDevice device to add
     */
    public void add(UiDevice uiDevice) {
        deviceLookup.put(uiDevice.id(), uiDevice);
    }

    /**
     * Removes the given device from the topology model.
     *
     * @param uiDevice device to remove
     */
    public void remove(UiDevice uiDevice) {
        UiDevice d = deviceLookup.remove(uiDevice.id());
        // TODO: Update the containing region
        if (d != null) {
            d.destroy();
        }
    }

    /**
     * Returns the number of devices configured in the topology.
     *
     * @return number of devices
     */
    public int deviceCount() {
        return deviceLookup.size();
    }


    /**
     * Returns all device links in the model.
     *
     * @return all device links
     */
    public Set<UiDeviceLink> allDeviceLinks() {
        return new HashSet<>(devLinkLookup.values());
    }

    /**
     * Returns the device link with the specified identifier, or null if no
     * such link exists.
     *
     * @param id the canonicalized link identifier
     * @return corresponding UI device link
     */
    public UiDeviceLink findDeviceLink(UiLinkId id) {
        return devLinkLookup.get(id);
    }

    /**
     * Returns the edge link with the specified identifier, or null if no
     * such link exists.
     *
     * @param id the canonicalized link identifier
     * @return corresponding UI edge link
     */
    public UiEdgeLink findEdgeLink(UiLinkId id) {
        return edgeLinkLookup.get(id);
    }

    /**
     * Adds the given UI device link to the topology model.
     *
     * @param uiDeviceLink link to add
     */
    public void add(UiDeviceLink uiDeviceLink) {
        devLinkLookup.put(uiDeviceLink.id(), uiDeviceLink);
    }

    /**
     * Adds the given UI edge link to the topology model.
     *
     * @param uiEdgeLink link to add
     */
    public void add(UiEdgeLink uiEdgeLink) {
        edgeLinkLookup.put(uiEdgeLink.id(), uiEdgeLink);
    }

    /**
     * Removes the given UI device link from the model.
     *
     * @param uiDeviceLink link to remove
     */
    public void remove(UiDeviceLink uiDeviceLink) {
        UiDeviceLink link = devLinkLookup.remove(uiDeviceLink.id());
        if (link != null) {
            link.destroy();
        }
    }

    /**
     * Removes the given UI edge link from the model.
     *
     * @param uiEdgeLink link to remove
     */
    public void remove(UiEdgeLink uiEdgeLink) {
        UiEdgeLink link = edgeLinkLookup.remove(uiEdgeLink.id());
        if (link != null) {
            link.destroy();
        }
    }

    /**
     * Returns the number of device links configured in the topology.
     *
     * @return number of device links
     */
    public int deviceLinkCount() {
        return devLinkLookup.size();
    }

    /**
     * Returns the number of edge links configured in the topology.
     *
     * @return number of edge links
     */
    public int edgeLinkCount() {
        return edgeLinkLookup.size();
    }

    /**
     * Returns all hosts in the model.
     *
     * @return all hosts
     */
    public Set<UiHost> allHosts() {
        return new HashSet<>(hostLookup.values());
    }

    /**
     * Returns the host with the specified identifier, or null if no such
     * host exists.
     *
     * @param id host identifier
     * @return corresponding UI host
     */
    public UiHost findHost(HostId id) {
        return hostLookup.get(id);
    }

    /**
     * Adds the given host to the topology model.
     *
     * @param uiHost host to add
     */
    public void add(UiHost uiHost) {
        hostLookup.put(uiHost.id(), uiHost);
    }

    /**
     * Removes the given host from the topology model.
     *
     * @param uiHost host to remove
     */
    public void remove(UiHost uiHost) {
        UiHost h = hostLookup.remove(uiHost.id());
        if (h != null) {
            h.destroy();
        }
    }

    /**
     * Returns the number of hosts configured in the topology.
     *
     * @return number of hosts
     */
    public int hostCount() {
        return hostLookup.size();
    }


    // ==
    // package private methods for supporting linkage amongst topology entities
    // ==

    /**
     * Returns the set of UI devices with the given identifiers.
     *
     * @param deviceIds device identifiers
     * @return set of matching UI device instances
     */
    Set<UiDevice> deviceSet(Set<DeviceId> deviceIds) {
        Set<UiDevice> uiDevices = new HashSet<>();
        for (DeviceId id : deviceIds) {
            UiDevice d = deviceLookup.get(id);
            if (d != null) {
                uiDevices.add(d);
            } else {
                log.warn(E_UNMAPPED, "device", id);
            }
        }
        return uiDevices;
    }

    /**
     * Returns the set of UI hosts with the given identifiers.
     *
     * @param hostIds host identifiers
     * @return set of matching UI host instances
     */
    Set<UiHost> hostSet(Set<HostId> hostIds) {
        Set<UiHost> uiHosts = new HashSet<>();
        for (HostId id : hostIds) {
            UiHost h = hostLookup.get(id);
            if (h != null) {
                uiHosts.add(h);
            } else {
                log.warn(E_UNMAPPED, "host", id);
            }
        }
        return uiHosts;
    }

    /**
     * Returns the set of UI device links with the given identifiers.
     *
     * @param uiLinkIds link identifiers
     * @return set of matching UI device link instances
     */
    Set<UiDeviceLink> linkSet(Set<UiLinkId> uiLinkIds) {
        Set<UiDeviceLink> result = new HashSet<>();
        for (UiLinkId id : uiLinkIds) {
            UiDeviceLink link = devLinkLookup.get(id);
            if (link != null) {
                result.add(link);
            } else {
                log.warn(E_UNMAPPED, "device link", id);
            }
        }
        return result;
    }

    /**
     * Uses the device-device links and data about the regions to compute the
     * set of synthetic links that are required per region.
     */
    public void computeSynthLinks() {
        List<UiSynthLink> slinks = new ArrayList<>();
        allDeviceLinks().forEach((link) -> {
            UiSynthLink synthetic = inferSyntheticLink(link);
            slinks.add(synthetic);
            log.debug("Synthetic link: {}", synthetic);
        });

        slinks.addAll(wrapHostLinks(nullRegion()));
        for (UiRegion r: allRegions()) {
            slinks.addAll(wrapHostLinks(r));
        }

        synthMap.clear();
        for (UiSynthLink sl : slinks) {
            synthMap.put(sl.original().id(), sl);
        }
    }

    private Set<UiSynthLink> wrapHostLinks(UiRegion region) {
        RegionId regionId = region.id();
        return region.hosts().stream().map(h -> wrapHostLink(regionId, h))
                .collect(Collectors.toSet());
    }

    private UiSynthLink wrapHostLink(RegionId regionId, UiHost host) {
        UiEdgeLink elink = new UiEdgeLink(this, host.edgeLinkId());
        return new UiSynthLink(regionId, elink, elink);
    }

    private UiSynthLink inferSyntheticLink(UiDeviceLink link) {
        /*
          Look at the containment hierarchy of each end of the link. Find the
          common ancestor region R. A synthetic link will be added to R, based
          on the "next" node back down the branch...

                S1 --- S2       * in the same region ...
                :      :
                R      R          return S1 --- S2 (same link instance)


                S1 --- S2       * in different regions (R1, R2) at same level
                :      :
                R1     R2         return R1 --- R2
                :      :
                R      R

                S1 --- S2       * in different regions at different levels
                :      :
                R1     R2         return R1 --- R3
                :      :
                R      R3
                       :
                       R

                S1 --- S2       * in different regions at different levels
                :      :
                R      R2         return S1 --- R2
                       :
                       R

         */
        DeviceId a = link.deviceA();
        DeviceId b = link.deviceB();
        List<RegionId> aBranch = ancestors(a);
        List<RegionId> bBranch = ancestors(b);
        if (aBranch == null || bBranch == null) {
            return null;
        }

        return makeSynthLink(link, aBranch, bBranch);
    }

    // package private for unit testing
    UiSynthLink makeSynthLink(UiDeviceLink orig,
                              List<RegionId> aBranch,
                              List<RegionId> bBranch) {

        final int aSize = aBranch.size();
        final int bSize = bBranch.size();
        final int min = Math.min(aSize, bSize);

        int index = 0;
        RegionId commonRegion = aBranch.get(index);

        while (true) {
            int next = index + 1;
            if (next == min) {
                // no more pairs of regions left to test
                break;
            }
            RegionId rA = aBranch.get(next);
            RegionId rB = bBranch.get(next);
            if (rA.equals(rB)) {
                commonRegion = rA;
                index++;
            } else {
                break;
            }
        }


        int endPointIndex = index + 1;
        UiLinkId linkId;
        UiLink link;

        if (endPointIndex < aSize) {
            // the A endpoint is a subregion
            RegionId aRegion = aBranch.get(endPointIndex);

            if (endPointIndex < bSize) {
                // the B endpoint is a subregion
                RegionId bRegion = bBranch.get(endPointIndex);

                linkId = uiLinkId(aRegion, bRegion);
                link = new UiRegionLink(this, linkId);

            } else {
                // the B endpoint is the device
                DeviceId dB = orig.deviceB();
                PortNumber pB = orig.portB();

                linkId = uiLinkId(aRegion, dB, pB);
                link = new UiRegionDeviceLink(this, linkId);
            }

        } else {
            // the A endpoint is the device
            DeviceId dA = orig.deviceA();
            PortNumber pA = orig.portA();

            if (endPointIndex < bSize) {
                // the B endpoint is a subregion
                RegionId bRegion = bBranch.get(endPointIndex);

                linkId = uiLinkId(bRegion, dA, pA);
                link = new UiRegionDeviceLink(this, linkId);

            } else {
                // the B endpoint is the device
                // (so, we can just use the original device-device link...)

                link = orig;
            }
        }
        return new UiSynthLink(commonRegion, link, orig);
    }

    private List<RegionId> ancestors(DeviceId id) {
        // return the ancestor chain from this device to root region
        UiDevice dev = findDevice(id);
        if (dev == null) {
            log.warn("Unable to find cached device with ID %s", id);
            return null;
        }

        UiRegion r = dev.uiRegion();
        List<RegionId> result = new ArrayList<>();
        while (r != null && !r.isRoot()) {
            result.add(0, r.id());
            r = r.parentRegion();
        }
        // finally add root region, since this is the grand-daddy of them all
        result.add(0, UiRegion.NULL_ID);
        return result;
    }


    /**
     * Returns the synthetic links associated with the specified region.
     *
     * @param regionId the region ID
     * @return synthetic links for this region
     */
    public List<UiSynthLink> findSynthLinks(RegionId regionId) {
        return synthMap.values().stream()
                .filter(s -> Objects.equals(regionId, s.regionId()))
                .collect(Collectors.toList());
    }


    /**
     * Returns the number of synthetic links in the topology.
     *
     * @return the synthetic link count
     */
    public int synthLinkCount() {
        return synthMap.size();
    }

    /**
     * Returns a detailed (multi-line) string showing the contents of the
     * topology.
     *
     * @return detailed string
     */
    public String dumpString() {
        StringBuilder sb = new StringBuilder("Topology:").append(EOL);

        sb.append(INDENT_1).append("Cluster Members").append(EOL);
        for (UiClusterMember m : cnodeLookup.values()) {
            sb.append(INDENT_2).append(m).append(EOL);
        }

        sb.append(INDENT_1).append("Regions").append(EOL);
        for (UiRegion r : regionLookup.values()) {
            sb.append(INDENT_2).append(r).append(EOL);
        }

        sb.append(INDENT_1).append("Devices").append(EOL);
        for (UiDevice d : deviceLookup.values()) {
            sb.append(INDENT_2).append(d).append(EOL);
        }

        sb.append(INDENT_1).append("Hosts").append(EOL);
        for (UiHost h : hostLookup.values()) {
            sb.append(INDENT_2).append(h).append(EOL);
        }

        sb.append(INDENT_1).append("Device Links").append(EOL);
        for (UiLink link : devLinkLookup.values()) {
            sb.append(INDENT_2).append(link).append(EOL);
        }

        sb.append(INDENT_1).append("Edge Links").append(EOL);
        for (UiLink link : edgeLinkLookup.values()) {
            sb.append(INDENT_2).append(link).append(EOL);
        }

        sb.append(INDENT_1).append("Synth Links").append(EOL);
        for (UiSynthLink link : synthMap.values()) {
            sb.append(INDENT_2).append(link).append(EOL);
        }
        sb.append("------").append(EOL);

        return sb.toString();
    }
}
