/*
 *  Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.net.region.RegionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

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


    // top level mappings of topology elements by ID
    private final Map<NodeId, UiClusterMember> cnodeLookup = new HashMap<>();
    private final Map<RegionId, UiRegion> regionLookup = new HashMap<>();
    private final Map<DeviceId, UiDevice> deviceLookup = new HashMap<>();
    private final Map<HostId, UiHost> hostLookup = new HashMap<>();
    private final Map<UiLinkId, UiLink> linkLookup = new HashMap<>();


    @Override
    public String toString() {
        return toStringHelper(this)
                .add("#cnodes", clusterMemberCount())
                .add("#regions", regionCount())
                .add("#devices", deviceLookup.size())
                .add("#hosts", hostLookup.size())
                .add("#links", linkLookup.size())
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
        linkLookup.clear();
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
     * Returns the region with the specified identifier, or null if
     * no such region exists.
     *
     * @param id region identifier
     * @return corresponding UI region
     */
    public UiRegion findRegion(RegionId id) {
        return regionLookup.get(id);
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
     * Returns the link with the specified identifier, or null if no such
     * link exists.
     *
     * @param id the canonicalized link identifier
     * @return corresponding UI link
     */
    public UiLink findLink(UiLinkId id) {
        return linkLookup.get(id);
    }

    /**
     * Adds the given UI link to the topology model.
     *
     * @param uiLink link to add
     */
    public void add(UiLink uiLink) {
        linkLookup.put(uiLink.id(), uiLink);
    }

    /**
     * Removes the given UI link from the model.
     *
     * @param uiLink link to remove
     */
    public void remove(UiLink uiLink) {
        UiLink link = linkLookup.remove(uiLink.id());
        if (link != null) {
            link.destroy();
        }
    }

    /**
     * Returns the number of links configured in the topology.
     *
     * @return number of links
     */
    public int linkCount() {
        return linkLookup.size();
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
     * Returns the set of UI links with the given identifiers.
     *
     * @param uiLinkIds link identifiers
     * @return set of matching UI link instances
     */
    Set<UiLink> linkSet(Set<UiLinkId> uiLinkIds) {
        Set<UiLink> uiLinks = new HashSet<>();
        for (UiLinkId id : uiLinkIds) {
            UiLink link = linkLookup.get(id);
            if (link != null) {
                uiLinks.add(link);
            } else {
                log.warn(E_UNMAPPED, "link", id);
            }
        }
        return uiLinks;
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

        sb.append(INDENT_1).append("Links").append(EOL);
        for (UiLink link : linkLookup.values()) {
            sb.append(INDENT_2).append(link).append(EOL);
        }
        sb.append("------").append(EOL);

        return sb.toString();
    }

}
