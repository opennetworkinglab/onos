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
import org.onlab.util.ItemNotFoundException;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicElementConfig;
import org.onosproject.net.config.basics.BasicRegionConfig;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionAdminService;
import org.onosproject.net.region.RegionEvent;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.region.RegionListener;
import org.onosproject.net.region.RegionService;
import org.onosproject.net.region.RegionStore;
import org.onosproject.net.region.RegionStoreDelegate;
import org.onosproject.ui.topo.LayoutLocation;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.of;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.REGION_READ;
import static org.onosproject.ui.topo.LayoutLocation.toCompactListString;
import static org.slf4j.LoggerFactory.getLogger;

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

    private static final String PEER_LOCATIONS = "peerLocations";

    private final Logger log = getLogger(getClass());

    private final NetworkConfigListener networkConfigListener =
            new InternalNetworkConfigListener();

    private RegionStoreDelegate delegate = this::post;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RegionStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(RegionEvent.class, listenerRegistry);
        networkConfigService.addListener(networkConfigListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        networkConfigService.removeListener(networkConfigListener);
        eventDispatcher.removeSink(RegionEvent.class);
        log.info("Stopped");
    }

    private String dstr(double d) {
        return Double.toString(d);
    }

    private Annotations genAnnots(RegionId id) {
        BasicRegionConfig cfg =
                networkConfigService.getConfig(id, BasicRegionConfig.class);
        if (cfg == null) {
            return DefaultAnnotations.builder().build();
        }
        return genAnnots(cfg, id);
    }

    private Annotations genAnnots(BasicRegionConfig cfg, RegionId rid) {

        DefaultAnnotations.Builder builder = DefaultAnnotations.builder()
                .set(BasicElementConfig.NAME, cfg.name())
                .set(BasicElementConfig.LATITUDE, dstr(cfg.latitude()))
                .set(BasicElementConfig.LONGITUDE, dstr(cfg.longitude()));

        // only set the UI_TYPE annotation if it is not null in the config
        String uiType = cfg.uiType();
        if (uiType != null) {
            builder.set(BasicElementConfig.UI_TYPE, uiType);
        }

        List<LayoutLocation> locMappings = cfg.getMappings();
        builder.set(PEER_LOCATIONS, toCompactListString(locMappings));

        return builder.build();
    }

    @Override
    public Region createRegion(RegionId regionId, String name, Region.Type type,
                               List<Set<NodeId>> masterNodeIds) {
        checkNotNull(regionId, REGION_ID_NULL);
        checkNotNull(name, NAME_NULL);
        checkNotNull(name, REGION_TYPE_NULL);

        return store.createRegion(regionId, name, type, genAnnots(regionId),
                                  masterNodeIds == null ? of() : masterNodeIds);
    }

    @Override
    public Region updateRegion(RegionId regionId, String name, Region.Type type,
                               List<Set<NodeId>> masterNodeIds) {
        checkNotNull(regionId, REGION_ID_NULL);
        checkNotNull(name, NAME_NULL);
        checkNotNull(name, REGION_TYPE_NULL);

        return store.updateRegion(regionId, name, type, genAnnots(regionId),
                                  masterNodeIds == null ? of() : masterNodeIds);
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

    // by default allowed, otherwise check flag
    private boolean isAllowed(BasicRegionConfig cfg) {
        return (cfg == null || cfg.isAllowed());
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            RegionId rid = (RegionId) event.subject();
            BasicRegionConfig cfg =
                    networkConfigService.getConfig(rid, BasicRegionConfig.class);

            if (!isAllowed(cfg)) {
                kickOutBadRegion(rid);

            } else {
                // (1) Find the region
                // (2) Syntehsize new region + cfg details
                // (3) re-insert new region element into store

                try {
                    Region region = getRegion(rid);
                    String name = region.name();
                    Region.Type type = region.type();
                    Annotations annots = genAnnots(cfg, rid);
                    List<Set<NodeId>> masterNodeIds = region.masters();

                    store.updateRegion(rid, name, type, annots, masterNodeIds);

                } catch (ItemNotFoundException infe) {
                    log.debug("warn: no region found with id {}", rid);
                }
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED
                    || event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED)
                    && (event.configClass().equals(BasicRegionConfig.class));
        }

        private void kickOutBadRegion(RegionId regionId) {
            Region badRegion = getRegion(regionId);
            if (badRegion != null) {
                removeRegion(regionId);
            }
        }
    }
}
