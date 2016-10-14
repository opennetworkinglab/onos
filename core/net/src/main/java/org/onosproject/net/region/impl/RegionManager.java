/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.net.region.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionAdminService;
import org.onosproject.net.region.RegionEvent;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.region.RegionListener;
import org.onosproject.net.region.RegionService;
import org.onosproject.net.region.RegionStore;
import org.onosproject.net.region.RegionStoreDelegate;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.of;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.REGION_READ;

/**
 * Provides implementation of the region service APIs.
 */
@Component(immediate = true)
@Service
public class RegionManager extends AbstractListenerManager<RegionEvent, RegionListener>
        implements RegionAdminService, RegionService {

    private static final String REGION_ID_NULL = "Region ID cannot be null";
    private static final String REGION_TYPE_NULL = "Region type cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String DEVICE_IDS_NULL = "Device IDs cannot be null";
    private static final String DEVICE_IDS_EMPTY = "Device IDs cannot be empty";
    private static final String NAME_NULL = "Name cannot be null";

    private final Logger log = getLogger(getClass());

    private RegionStoreDelegate delegate = this::post;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RegionStore store;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(RegionEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(RegionEvent.class);
        log.info("Stopped");
    }

    @Override
    public Region createRegion(RegionId regionId, String name, Region.Type type,
                               List<Set<NodeId>> masterNodeIds) {
        checkNotNull(regionId, REGION_ID_NULL);
        checkNotNull(name, NAME_NULL);
        checkNotNull(name, REGION_TYPE_NULL);
        return store.createRegion(regionId, name, type, masterNodeIds == null ? of() : masterNodeIds);
    }

    @Override
    public Region updateRegion(RegionId regionId, String name, Region.Type type,
                               List<Set<NodeId>> masterNodeIds) {
        checkNotNull(regionId, REGION_ID_NULL);
        checkNotNull(name, NAME_NULL);
        checkNotNull(name, REGION_TYPE_NULL);
        return store.updateRegion(regionId, name, type, masterNodeIds == null ? of() : masterNodeIds);
    }

    @Override
    public void removeRegion(RegionId regionId) {
        checkNotNull(regionId, REGION_ID_NULL);
        store.removeRegion(regionId);
    }

    @Override
    public void addDevices(RegionId regionId, Collection<DeviceId> deviceIds) {
        checkNotNull(regionId, REGION_ID_NULL);
        checkNotNull(deviceIds, DEVICE_IDS_NULL);
        checkState(!deviceIds.isEmpty(), DEVICE_IDS_EMPTY);
        store.addDevices(regionId, deviceIds);
    }

    @Override
    public void removeDevices(RegionId regionId, Collection<DeviceId> deviceIds) {
        checkNotNull(regionId, REGION_ID_NULL);
        checkNotNull(deviceIds, DEVICE_IDS_NULL);
        checkState(!deviceIds.isEmpty(), DEVICE_IDS_EMPTY);
        store.removeDevices(regionId, deviceIds);
    }

    @Override
    public Set<Region> getRegions() {
        checkPermission(REGION_READ);
        return store.getRegions();
    }

    @Override
    public Region getRegion(RegionId regionId) {
        checkPermission(REGION_READ);
        checkNotNull(regionId, REGION_ID_NULL);
        return store.getRegion(regionId);
    }

    @Override
    public Region getRegionForDevice(DeviceId deviceId) {
        checkPermission(REGION_READ);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getRegionForDevice(deviceId);
    }

    @Override
    public Set<DeviceId> getRegionDevices(RegionId regionId) {
        checkPermission(REGION_READ);
        checkNotNull(regionId, REGION_ID_NULL);
        return store.getRegionDevices(regionId);
    }

    @Override
    public Set<HostId> getRegionHosts(RegionId regionId) {
        checkPermission(REGION_READ);
        checkNotNull(regionId, REGION_ID_NULL);
        // TODO: compute hosts from region devices
        return ImmutableSet.of();
    }

}
