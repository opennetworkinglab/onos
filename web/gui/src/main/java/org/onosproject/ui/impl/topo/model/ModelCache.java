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

package org.onosproject.ui.impl.topo.model;

import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.event.EventDispatcher;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.UiTopoLayoutService;
import org.onosproject.ui.model.ServiceBundle;
import org.onosproject.ui.model.topo.UiClusterMember;
import org.onosproject.ui.model.topo.UiDevice;
import org.onosproject.ui.model.topo.UiDeviceLink;
import org.onosproject.ui.model.topo.UiEdgeLink;
import org.onosproject.ui.model.topo.UiElement;
import org.onosproject.ui.model.topo.UiHost;
import org.onosproject.ui.model.topo.UiLinkId;
import org.onosproject.ui.model.topo.UiRegion;
import org.onosproject.ui.model.topo.UiSynthLink;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.model.topo.UiTopoLayoutId;
import org.onosproject.ui.model.topo.UiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.CLUSTER_MEMBER_ADDED_OR_UPDATED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.CLUSTER_MEMBER_REMOVED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.DEVICE_ADDED_OR_UPDATED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.DEVICE_REMOVED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.HOST_ADDED_OR_UPDATED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.HOST_MOVED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.HOST_REMOVED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.LINK_ADDED_OR_UPDATED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.LINK_REMOVED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.REGION_ADDED_OR_UPDATED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.REGION_REMOVED;
import static org.onosproject.ui.model.topo.UiLinkId.uiLinkId;

/**
 * UI Topology Model cache.
 */
class ModelCache {

    private static final String E_NO_ELEMENT = "Tried to remove non-member {}: {}";

    private static final Logger log = LoggerFactory.getLogger(ModelCache.class);

    private final ServiceBundle services;
    private final EventDispatcher dispatcher;
    private final UiTopology uiTopology = new UiTopology();

    ModelCache(ServiceBundle services, EventDispatcher eventDispatcher) {
        this.services = services;
        this.dispatcher = eventDispatcher;
    }

    @Override
    public String toString() {
        return "ModelCache{" + uiTopology + "}";
    }

    private void postEvent(UiModelEvent.Type type, UiElement subject) {
        dispatcher.post(new UiModelEvent(type, subject));
    }

    void clear() {
        uiTopology.clear();
    }

    /**
     * Create our internal model of the global topology. An assumption we are
     * making is that the topology is empty to start.
     */
    void load() {
        loadClusterMembers();
        loadRegions();
        loadDevices();
        loadDeviceLinks();
        loadHosts();
    }


    // === CLUSTER MEMBERS

    private UiClusterMember addNewClusterMember(ControllerNode n) {
        UiClusterMember member = new UiClusterMember(uiTopology, n);
        uiTopology.add(member);
        return member;
    }

    private void updateClusterMember(UiClusterMember member) {
        ControllerNode.State state = services.cluster().getState(member.id());
        member.setState(state);
        member.setMastership(services.mastership().getDevicesOf(member.id()));
        // NOTE: 'UI-attached' is session-based data, not global, so will
        //       be set elsewhere
    }

    private void loadClusterMembers() {
        for (ControllerNode n : services.cluster().getNodes()) {
            UiClusterMember member = addNewClusterMember(n);
            updateClusterMember(member);
        }
    }

    // invoked from UiSharedTopologyModel cluster event listener
    void addOrUpdateClusterMember(ControllerNode cnode) {
        NodeId id = cnode.id();
        UiClusterMember member = uiTopology.findClusterMember(id);
        if (member == null) {
            member = addNewClusterMember(cnode);
        }
        updateClusterMember(member);

        postEvent(CLUSTER_MEMBER_ADDED_OR_UPDATED, member);
    }

    // package private for unit test access
    UiClusterMember accessClusterMember(NodeId id) {
        return uiTopology.findClusterMember(id);
    }

    // invoked from UiSharedTopologyModel cluster event listener
    void removeClusterMember(ControllerNode cnode) {
        NodeId id = cnode.id();
        UiClusterMember member = uiTopology.findClusterMember(id);
        if (member != null) {
            uiTopology.remove(member);
            postEvent(CLUSTER_MEMBER_REMOVED, member);
        } else {
            log.warn(E_NO_ELEMENT, "cluster node", id);
        }
    }

    List<UiClusterMember> getAllClusterMembers() {
        return uiTopology.allClusterMembers();
    }


    // === MASTERSHIP CHANGES

    // invoked from UiSharedTopologyModel mastership listener
    void updateMasterships(DeviceId deviceId, RoleInfo roleInfo) {
        // To think about:: do we need to store mastership info?
        //  or can we rely on looking it up live?
        // TODO: store the updated mastership information
        // TODO: post event
    }

    // === THE NULL REGION

    UiRegion nullRegion() {
        return uiTopology.nullRegion();
    }

    // === REGIONS

    private UiRegion addNewRegion(Region r) {
        UiRegion region = new UiRegion(uiTopology, r);
        uiTopology.add(region);
        log.debug("Region {} added to topology", region);
        return region;
    }

    private void updateRegion(UiRegion region) {
        RegionId rid = region.id();
        Set<DeviceId> deviceIds = services.region().getRegionDevices(rid);

        // Make sure device objects refer to their region
        deviceIds.forEach(d -> {
            UiDevice dev = uiTopology.findDevice(d);
            if (dev != null) {
                dev.setRegionId(rid);
            } else {
                // if we don't have the UiDevice in the topology, what can we do?
                log.warn("Region device {}, but we don't have UiDevice in topology", d);
            }
        });

        // Make sure the region object refers to the devices
        region.reconcileDevices(deviceIds);

        fixupContainmentHierarchy(region);
    }

    private void fixupContainmentHierarchy(UiRegion region) {
        UiTopoLayoutService ls = services.layout();
        RegionId regionId = region.id();

        UiTopoLayout layout = ls.getLayout(regionId);
        if (layout == null) {
            // no layout backed by this region
            log.warn("No layout backed by region {}", regionId);
            return;
        }

        UiTopoLayoutId layoutId = layout.id();

        if (!layout.isRoot()) {
            UiTopoLayoutId parentId = layout.parent();
            UiTopoLayout parentLayout = ls.getLayout(parentId);
            RegionId parentRegionId = parentLayout.regionId();
            region.setParent(parentRegionId);
        }

        Set<UiTopoLayout> kids = ls.getChildren(layoutId);
        Set<RegionId> kidRegionIds = new HashSet<>(kids.size());
        kids.forEach(k -> kidRegionIds.add(k.regionId()));
        region.setChildren(kidRegionIds);
    }

    private void loadRegions() {
        for (Region r : services.region().getRegions()) {
            UiRegion region = addNewRegion(r);
            updateRegion(region);
        }
    }

    // invoked from UiSharedTopologyModel region listener
    void addOrUpdateRegion(Region region) {
        RegionId id = region.id();
        UiRegion uiRegion = uiTopology.findRegion(id);
        if (uiRegion == null) {
            uiRegion = addNewRegion(region);
        }
        updateRegion(uiRegion);

        postEvent(REGION_ADDED_OR_UPDATED, uiRegion);
    }

    // package private for unit test access
    UiRegion accessRegion(RegionId id) {
        return id == null ? null : uiTopology.findRegion(id);
    }

    // invoked from UiSharedTopologyModel region listener
    void removeRegion(Region region) {
        RegionId id = region.id();
        UiRegion uiRegion = uiTopology.findRegion(id);
        if (uiRegion != null) {
            uiTopology.remove(uiRegion);
            postEvent(REGION_REMOVED, uiRegion);
        } else {
            log.warn(E_NO_ELEMENT, "region", id);
        }
    }

    Set<UiRegion> getAllRegions() {
        return uiTopology.allRegions();
    }


    // === DEVICES

    private UiDevice addNewDevice(Device d) {
        UiDevice device = new UiDevice(uiTopology, d);
        updateDevice(device);
        uiTopology.add(device);
        log.debug("Device {} added to topology", device);
        return device;
    }

    // make sure the UiDevice is tagged with the region it belongs to
    private void updateDevice(UiDevice device) {
        Region r = services.region().getRegionForDevice(device.id());
        RegionId rid = r == null ? UiRegion.NULL_ID : r.id();
        device.setRegionId(rid);
    }

    private void loadDevices() {
        for (Device d : services.device().getDevices()) {
            addNewDevice(d);
        }
    }

    // invoked from UiSharedTopologyModel device listener
    void addOrUpdateDevice(Device device) {
        DeviceId id = device.id();
        UiDevice uiDevice = uiTopology.findDevice(id);
        if (uiDevice == null) {
            uiDevice = addNewDevice(device);
        } else {
            updateDevice(uiDevice);
        }

        postEvent(DEVICE_ADDED_OR_UPDATED, uiDevice);
    }

    // package private for unit test access
    UiDevice accessDevice(DeviceId id) {
        return uiTopology.findDevice(id);
    }

    // invoked from UiSharedTopologyModel device listener
    void removeDevice(Device device) {
        DeviceId id = device.id();
        UiDevice uiDevice = uiTopology.findDevice(id);
        if (uiDevice != null) {
            uiTopology.remove(uiDevice);
            postEvent(DEVICE_REMOVED, uiDevice);
        } else {
            log.warn(E_NO_ELEMENT, "device", id);
        }
    }

    Set<UiDevice> getAllDevices() {
        return uiTopology.allDevices();
    }


    // === LINKS ===

    private UiDeviceLink addNewDeviceLink(UiLinkId id) {
        UiDeviceLink uiDeviceLink = new UiDeviceLink(uiTopology, id);
        uiTopology.add(uiDeviceLink);
        return uiDeviceLink;
    }

    private UiEdgeLink addNewEdgeLink(UiLinkId id) {
        UiEdgeLink uiEdgeLink = new UiEdgeLink(uiTopology, id);
        uiTopology.add(uiEdgeLink);
        return uiEdgeLink;
    }

    private void updateDeviceLink(UiDeviceLink uiDeviceLink, Link link) {
        uiDeviceLink.attachBackingLink(link);
    }

    private void loadDeviceLinks() {
        for (Link link : services.link().getLinks()) {
            UiLinkId id = uiLinkId(link);

            UiDeviceLink uiDeviceLink = uiTopology.findDeviceLink(id);
            if (uiDeviceLink == null) {
                uiDeviceLink = addNewDeviceLink(id);
            }
            updateDeviceLink(uiDeviceLink, link);
        }
    }

    // invoked from UiSharedTopologyModel link listener
    void addOrUpdateDeviceLink(Link link) {
        UiLinkId id = uiLinkId(link);
        UiDeviceLink uiDeviceLink = uiTopology.findDeviceLink(id);
        if (uiDeviceLink == null) {
            uiDeviceLink = addNewDeviceLink(id);
        }
        updateDeviceLink(uiDeviceLink, link);

        postEvent(LINK_ADDED_OR_UPDATED, uiDeviceLink);
    }

    // package private for unit test access
    UiDeviceLink accessDeviceLink(UiLinkId id) {
        return uiTopology.findDeviceLink(id);
    }

    // invoked from UiSharedTopologyModel link listener
    void removeDeviceLink(Link link) {
        UiLinkId id = uiLinkId(link);
        UiDeviceLink uiDeviceLink = uiTopology.findDeviceLink(id);
        if (uiDeviceLink != null) {
            boolean remaining = uiDeviceLink.detachBackingLink(link);
            if (remaining) {
                postEvent(LINK_ADDED_OR_UPDATED, uiDeviceLink);
            } else {
                uiTopology.remove(uiDeviceLink);
                postEvent(LINK_REMOVED, uiDeviceLink);
            }
        } else {
            log.warn(E_NO_ELEMENT, "Device link", id);
        }
    }

    Set<UiDeviceLink> getAllDeviceLinks() {
        return uiTopology.allDeviceLinks();
    }

    // === HOSTS

    private EdgeLink synthesizeLink(Host h) {
        return createEdgeLink(h, true);
    }

    private UiHost addNewHost(Host h) {
        UiHost host = new UiHost(uiTopology, h);
        uiTopology.add(host);

        EdgeLink elink = synthesizeLink(h);
        UiLinkId elinkId = uiLinkId(elink);
        host.setEdgeLinkId(elinkId);

        // add synthesized edge link to the topology
        UiEdgeLink edgeLink = addNewEdgeLink(elinkId);
        edgeLink.attachEdgeLink(elink);

        return host;
    }

    private void insertNewUiEdgeLink(UiLinkId id, EdgeLink e) {
        UiEdgeLink newEdgeLink = addNewEdgeLink(id);
        newEdgeLink.attachEdgeLink(e);
    }

    private void updateHost(UiHost uiHost, Host h) {
        UiEdgeLink existing = uiTopology.findEdgeLink(uiHost.edgeLinkId());

        EdgeLink currentElink = synthesizeLink(h);
        UiLinkId currentElinkId = uiLinkId(currentElink);

        if (existing != null) {
            if (!currentElinkId.equals(existing.id())) {
                // edge link has changed
                insertNewUiEdgeLink(currentElinkId, currentElink);
                uiHost.setEdgeLinkId(currentElinkId);

                uiTopology.remove(existing);
            }

        } else {
            // no previously existing edge link
            insertNewUiEdgeLink(currentElinkId, currentElink);
            uiHost.setEdgeLinkId(currentElinkId);

        }

        HostLocation hloc = h.location();
        uiHost.setLocation(hloc.deviceId(), hloc.port());
    }

    private void loadHosts() {
        for (Host h : services.host().getHosts()) {
            UiHost host = addNewHost(h);
            updateHost(host, h);
        }
    }

    // invoked from UiSharedTopologyModel host listener
    void addOrUpdateHost(Host host) {
        HostId id = host.id();
        UiHost uiHost = uiTopology.findHost(id);
        if (uiHost == null) {
            uiHost = addNewHost(host);
        }
        updateHost(uiHost, host);

        postEvent(HOST_ADDED_OR_UPDATED, uiHost);
    }

    // invoked from UiSharedTopologyModel host listener
    void moveHost(Host host, Host prevHost) {
        UiHost uiHost = uiTopology.findHost(prevHost.id());
        if (uiHost != null) {
            updateHost(uiHost, host);
            postEvent(HOST_MOVED, uiHost);
        } else {
            log.warn(E_NO_ELEMENT, "host", prevHost.id());
        }
    }

    // package private for unit test access
    UiHost accessHost(HostId id) {
        return uiTopology.findHost(id);
    }

    // invoked from UiSharedTopologyModel host listener
    void removeHost(Host host) {
        HostId id = host.id();
        UiHost uiHost = uiTopology.findHost(id);
        if (uiHost != null) {
            UiEdgeLink edgeLink = uiTopology.findEdgeLink(uiHost.edgeLinkId());
            uiTopology.remove(edgeLink);
            uiTopology.remove(uiHost);
            postEvent(HOST_REMOVED, uiHost);
        } else {
            log.warn(E_NO_ELEMENT, "host", id);
        }
    }

    Set<UiHost> getAllHosts() {
        return uiTopology.allHosts();
    }


    // === SYNTHETIC LINKS

    List<UiSynthLink> getSynthLinks(RegionId regionId) {
        return uiTopology.findSynthLinks(regionId);
    }

    /**
     * Refreshes the internal state.
     */
    public void refresh() {
        // fix up internal linkages to ensure they are correct

        // make sure regions reflect layout containment hierarchy
        fixupContainmentHierarchy(uiTopology.nullRegion());
        uiTopology.allRegions().forEach(this::fixupContainmentHierarchy);

        // make sure devices are in the correct region
        Set<UiDevice> allDevices = uiTopology.allDevices();

        services.region().getRegions().forEach(r -> {
            RegionId rid = r.id();
            UiRegion region = uiTopology.findRegion(rid);
            if (region != null) {
                Set<DeviceId> deviceIds = services.region().getRegionDevices(rid);
                region.reconcileDevices(deviceIds);

                deviceIds.forEach(devId -> {
                    UiDevice dev = uiTopology.findDevice(devId);
                    if (dev != null) {
                        dev.setRegionId(r.id());
                        allDevices.remove(dev);
                    } else {
                        log.warn("Region device ID {} but no UiDevice in topology",
                                devId);
                    }
                });
            } else {
                log.warn("No UiRegion in topology for ID {}", rid);
            }
        });

        // what is left over, must belong to the null-region
        Set<DeviceId> leftOver = new HashSet<>(allDevices.size());
        allDevices.forEach(d -> leftOver.add(d.id()));
        uiTopology.nullRegion().reconcileDevices(leftOver);

        // now that we have correct region hierarchy, and devices are in their
        //  respective regions, we can compute synthetic links for each region.
        uiTopology.computeSynthLinks();
    }


    // === CACHE STATISTICS

    /**
     * Returns a detailed (multi-line) string showing the contents of the cache.
     *
     * @return detailed string
     */
    public String dumpString() {
        return uiTopology.dumpString();
    }

    /**
     * Returns the number of members in the cluster.
     *
     * @return number of cluster members
     */
    public int clusterMemberCount() {
        return uiTopology.clusterMemberCount();
    }

    /**
     * Returns the number of regions in the topology.
     *
     * @return number of regions
     */
    public int regionCount() {
        return uiTopology.regionCount();
    }

    /**
     * Returns the number of devices in the topology.
     *
     * @return number of devices
     */
    public int deviceCount() {
        return uiTopology.deviceCount();
    }

    /**
     * Returns the number of device links in the topology.
     *
     * @return number of device links
     */
    public int deviceLinkCount() {
        return uiTopology.deviceLinkCount();
    }

    /**
     * Returns the number of edge links in the topology.
     *
     * @return number of edge links
     */
    public int edgeLinkCount() {
        return uiTopology.edgeLinkCount();
    }

    /**
     * Returns the number of hosts in the topology.
     *
     * @return number of hosts
     */
    public int hostCount() {
        return uiTopology.hostCount();
    }

    /**
     * Returns the number of synthetic links in the topology.
     *
     * @return the number of synthetic links
     */
    public int synthLinkCount() {
        return uiTopology.synthLinkCount();
    }
}
