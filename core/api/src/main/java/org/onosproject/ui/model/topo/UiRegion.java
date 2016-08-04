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

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.net.region.RegionId.regionId;

/**
 * Represents a region.
 */
public class UiRegion extends UiNode {

    private static final String NULL_NAME = "<null-region>";

    /**
     * The identifier for the null-region. That is, a container for devices,
     * hosts, and links for those that belong to no region.
     */
    public static final RegionId NULL_ID = regionId(NULL_NAME);

    private static final String[] DEFAULT_LAYER_TAGS = {
            UiNode.LAYER_OPTICAL,
            UiNode.LAYER_PACKET,
            UiNode.LAYER_DEFAULT
    };

    // loose bindings to things in this region
    private final Set<DeviceId> deviceIds = new HashSet<>();
    private final Set<HostId> hostIds = new HashSet<>();
    private final Set<UiLinkId> uiLinkIds = new HashSet<>();

    private final List<String> layerOrder = new ArrayList<>();

    private final UiTopology topology;

    private final Region region;

    // keep track of hierarchy (inferred from UiTopoLayoutService)
    private RegionId parent;
    private final Set<RegionId> kids = new HashSet<>();

    /**
     * Constructs a UI region, with a reference to the specified backing region.
     *
     * @param topology parent topology
     * @param region   backing region
     */
    public UiRegion(UiTopology topology, Region region) {
        // Implementation Note: if region is null, this UiRegion is being used
        //  as a container for devices, hosts, links that belong to no region.
        this.topology = topology;
        this.region = region;

        setLayerOrder(DEFAULT_LAYER_TAGS);
    }

    @Override
    protected void destroy() {
        deviceIds.clear();
        hostIds.clear();
        uiLinkIds.clear();
    }

    /**
     * Sets the layer order for this region.
     * Typically, the {@code UiNode.LAYER_*} constants will be used here.
     *
     * @param layers the layers
     */
    public void setLayerOrder(String... layers) {
        layerOrder.clear();
        Collections.addAll(layerOrder, layers);
    }

    /**
     * Returns the identity of the region.
     *
     * @return region ID
     */
    public RegionId id() {
        return region == null ? NULL_ID : region.id();
    }

    /**
     * Returns the identity of the parent region.
     *
     * @return parent region ID
     */
    public RegionId parent() {
        return parent;
    }

    /**
     * Returns true if this is the root (default) region.
     *
     * @return true if root region
     */
    public boolean isRoot() {
        return id().equals(parent);
    }

    /**
     * Returns the identities of the child regions.
     *
     * @return child region IDs
     */
    public Set<RegionId> children() {
        return ImmutableSet.copyOf(kids);
    }

    /**
     * Sets the parent ID for this region.
     *
     * @param parentId parent ID
     */
    public void setParent(RegionId parentId) {
        parent = parentId;
    }

    /**
     * Sets the children IDs for this region.
     *
     * @param children children IDs
     */
    public void setChildren(Set<RegionId> children) {
        kids.clear();
        kids.addAll(children);
    }

    @Override
    public String idAsString() {
        return id().toString();
    }

    @Override
    public String name() {
        return region == null ? NULL_NAME : region.name();
    }

    /**
     * Returns the region instance backing this UI region. If this instance
     * represents the "null-region", the value returned will be null.
     *
     * @return the backing region instance
     */
    public Region backingRegion() {
        return region;
    }

    /**
     * Make sure we have only these devices in the region.
     *
     * @param devices devices in the region
     */
    public void reconcileDevices(Set<DeviceId> devices) {
        deviceIds.clear();
        deviceIds.addAll(devices);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id())
                .add("name", name())
                .add("parent", parent)
                .add("kids", kids)
                .add("devices", deviceIds)
                .add("#hosts", hostIds.size())
                .add("#links", uiLinkIds.size())
                .toString();
    }

    /**
     * Returns the region's type.
     *
     * @return region type
     */
    public Region.Type type() {
        return region == null ? null : region.type();
    }


    /**
     * Returns the count of devices in this region.
     *
     * @return the device count
     */
    public int deviceCount() {
        return deviceIds.size();
    }

    /**
     * Returns the set of device identifiers for this region.
     *
     * @return device identifiers for this region
     */
    public Set<DeviceId> deviceIds() {
        return ImmutableSet.copyOf(deviceIds);
    }

    /**
     * Returns the devices in this region.
     *
     * @return the devices in this region
     */
    public Set<UiDevice> devices() {
        return topology.deviceSet(deviceIds);
    }

    /**
     * Returns the set of host identifiers for this region.
     *
     * @return host identifiers for this region
     */
    public Set<HostId> hostIds() {
        return ImmutableSet.copyOf(hostIds);
    }

    /**
     * Returns the hosts in this region.
     *
     * @return the hosts in this region
     */
    public Set<UiHost> hosts() {
        return topology.hostSet(hostIds);
    }

    /**
     * Returns the set of link identifiers for this region.
     *
     * @return link identifiers for this region
     */
    public Set<UiLinkId> linkIds() {
        return ImmutableSet.copyOf(uiLinkIds);
    }

    /**
     * Returns the links in this region.
     *
     * @return the links in this region
     */
    public Set<UiLink> links() {
        return topology.linkSet(uiLinkIds);
    }

    /**
     * Returns the order in which layers should be rendered. Lower layers
     * come earlier in the list. For example, to indicate that nodes in the
     * optical layer should be rendered "below" nodes in the packet layer,
     * this method should return:
     * <pre>
     * [UiNode.LAYER_OPTICAL, UiNode.LAYER_PACKET, UiNode.LAYER_DEFAULT]
     * </pre>
     *
     * @return layer ordering
     */
    public List<String> layerOrder() {
        return Collections.unmodifiableList(layerOrder);
    }
}
