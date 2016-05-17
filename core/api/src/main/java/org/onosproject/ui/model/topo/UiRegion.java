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

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents a region.
 */
public class UiRegion extends UiNode {

    // loose bindings to things in this region
    private final Set<DeviceId> deviceIds = new HashSet<>();
    private final Set<HostId> hostIds = new HashSet<>();
    private final Set<UiLinkId> uiLinkIds = new HashSet<>();

    private final UiTopology topology;

    private final Region region;

    /**
     * Constructs a UI region, with a reference to the specified backing region.
     *
     * @param topology parent topology
     * @param region   backing region
     */
    public UiRegion(UiTopology topology, Region region) {
        this.topology = topology;
        this.region = region;
    }

    @Override
    protected void destroy() {
        deviceIds.clear();
        hostIds.clear();
        uiLinkIds.clear();
    }

    /**
     * Returns the identity of the region.
     *
     * @return region ID
     */
    public RegionId id() {
        return region.id();
    }

    @Override
    public String idAsString() {
        return id().toString();
    }

    @Override
    public String name() {
        return region.name();
    }

    /**
     * Returns the region instance backing this UI region.
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
        return region.type();
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
}
