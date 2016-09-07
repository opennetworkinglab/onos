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

package org.onosproject.store.region.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Identifier;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.region.DefaultRegion;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionEvent;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.region.RegionStore;
import org.onosproject.net.region.RegionStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.region.RegionEvent.Type.REGION_MEMBERSHIP_CHANGED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Consistent store implementation for tracking region definitions and device
 * region affiliation.
 */
@Component(immediate = true)
@Service
public class DistributedRegionStore
        extends AbstractStore<RegionEvent, RegionStoreDelegate>
        implements RegionStore {

    private static final String NO_REGION = "Region does not exist";
    private static final String DUPLICATE_REGION = "Region already exists";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ConsistentMap<RegionId, Region> regionsRepo;
    private Map<RegionId, Region> regionsById;

    private ConsistentMap<RegionId, Set<DeviceId>> membershipRepo;
    private Map<RegionId, Set<DeviceId>> regionDevices;

    private Map<DeviceId, Region> regionsByDevice = new HashMap<>();

    private final MapEventListener<RegionId, Region> listener =
            new InternalRegionListener();
    private final MapEventListener<RegionId, Set<DeviceId>> membershipListener =
            new InternalMembershipListener();

    @Activate
    protected void activate() {
        Serializer serializer =
                Serializer.using(Arrays.asList(KryoNamespaces.API),
                                 Identifier.class);

        regionsRepo = storageService.<RegionId, Region>consistentMapBuilder()
                .withSerializer(serializer)
                .withName("onos-regions")
                .withRelaxedReadConsistency()
                .build();
        regionsRepo.addListener(listener);
        regionsById = regionsRepo.asJavaMap();

        membershipRepo = storageService.<RegionId, Set<DeviceId>>consistentMapBuilder()
                .withSerializer(serializer)
                .withName("onos-region-devices")
                .withRelaxedReadConsistency()
                .build();
        membershipRepo.addListener(membershipListener);
        regionDevices = membershipRepo.asJavaMap();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        regionsRepo.removeListener(listener);
        membershipRepo.removeListener(membershipListener);
        regionsByDevice.clear();
        log.info("Stopped");
    }

    @Override
    public Set<Region> getRegions() {
        return ImmutableSet.copyOf(regionsById.values());
    }

    @Override
    public Region getRegion(RegionId regionId) {
        return nullIsNotFound(regionsById.get(regionId), NO_REGION);
    }

    @Override
    public Region getRegionForDevice(DeviceId deviceId) {
        return regionsByDevice.get(deviceId);
    }

    @Override
    public Set<DeviceId> getRegionDevices(RegionId regionId) {
        Set<DeviceId> deviceIds = regionDevices.get(regionId);
        return deviceIds != null ? ImmutableSet.copyOf(deviceIds) : ImmutableSet.of();
    }

    @Override
    public Region createRegion(RegionId regionId, String name, Region.Type type,
                               List<Set<NodeId>> masterNodeIds) {
        return regionsRepo.compute(regionId, (id, region) -> {
            checkArgument(region == null, DUPLICATE_REGION);
            return new DefaultRegion(regionId, name, type, masterNodeIds);
        }).value();
    }

    @Override
    public Region updateRegion(RegionId regionId, String name, Region.Type type,
                               List<Set<NodeId>> masterNodeIds) {
        return regionsRepo.compute(regionId, (id, region) -> {
            nullIsNotFound(region, NO_REGION);
            return new DefaultRegion(regionId, name, type, masterNodeIds);
        }).value();
    }

    @Override
    public void removeRegion(RegionId regionId) {
        membershipRepo.remove(regionId);
        regionsRepo.remove(regionId);
    }

    @Override
    public void addDevices(RegionId regionId, Collection<DeviceId> deviceIds) {
        // Devices can only be a member in one region.  Remove the device if it belongs to
        // a different region than the region for which we are attempting to add it.
        for (DeviceId deviceId : deviceIds) {
            Region region = getRegionForDevice(deviceId);
            if ((region != null) && (!regionId.id().equals(region.id().id()))) {
                Set<DeviceId> deviceIdSet1 = ImmutableSet.of(deviceId);
                removeDevices(region.id(), deviceIdSet1);
            }
        }

        membershipRepo.compute(regionId, (id, existingDevices) -> {
            if (existingDevices == null) {
                return ImmutableSet.copyOf(deviceIds);
            } else if (!existingDevices.containsAll(deviceIds)) {
                return ImmutableSet.<DeviceId>builder()
                        .addAll(existingDevices)
                        .addAll(deviceIds)
                        .build();
            } else {
                return existingDevices;
            }
        });

        Region region = regionsById.get(regionId);
        deviceIds.forEach(deviceId -> regionsByDevice.put(deviceId, region));
    }

    @Override
    public void removeDevices(RegionId regionId, Collection<DeviceId> deviceIds) {
        membershipRepo.compute(regionId, (id, existingDevices) -> {
            if (existingDevices == null || existingDevices.isEmpty()) {
                return ImmutableSet.of();
            } else {
                return ImmutableSet.<DeviceId>builder()
                        .addAll(Sets.difference(existingDevices,
                                                ImmutableSet.copyOf(deviceIds)))
                        .build();
            }
        });

        deviceIds.forEach(deviceId -> regionsByDevice.remove(deviceId));
    }

    /**
     * Listener class to map listener events to the region inventory events.
     */
    private class InternalRegionListener implements MapEventListener<RegionId, Region> {
        @Override
        public void event(MapEvent<RegionId, Region> event) {
            Region region = null;
            RegionEvent.Type type = null;
            switch (event.type()) {
                case INSERT:
                    type = RegionEvent.Type.REGION_ADDED;
                    region = checkNotNull(event.newValue().value());
                    break;
                case UPDATE:
                    type = RegionEvent.Type.REGION_UPDATED;
                    region = checkNotNull(event.newValue().value());
                    break;
                case REMOVE:
                    type = RegionEvent.Type.REGION_REMOVED;
                    region = checkNotNull(event.oldValue().value());
                    break;
                default:
                    log.error("Unsupported event type: " + event.type());
            }
            notifyDelegate(new RegionEvent(type, region));
        }
    }

    /**
     * Listener class to map listener events to the region membership events.
     */
    private class InternalMembershipListener implements MapEventListener<RegionId, Set<DeviceId>> {
        @Override
        public void event(MapEvent<RegionId, Set<DeviceId>> event) {
            if (event.type() != MapEvent.Type.REMOVE) {
                notifyDelegate(new RegionEvent(REGION_MEMBERSHIP_CHANGED,
                                               regionsById.get(event.key()),
                                               event.newValue().value()));
            }
        }
    }
}
