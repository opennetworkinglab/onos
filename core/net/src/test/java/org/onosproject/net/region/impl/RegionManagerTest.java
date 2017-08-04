/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.cluster.NodeId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionAdminService;
import org.onosproject.net.region.RegionEvent;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.region.RegionListener;
import org.onosproject.store.region.impl.DistributedRegionStore;
import org.onosproject.store.service.TestStorageService;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.onosproject.net.region.Region.Type.*;
import static org.onosproject.net.region.RegionEvent.Type.*;

/**
 * Tests of the region service implementation.
 */
public class RegionManagerTest {

    private static final RegionId RID1 = RegionId.regionId("r1");
    private static final RegionId RID2 = RegionId.regionId("r2");

    private static final DeviceId DID1 = DeviceId.deviceId("foo:d1");
    private static final DeviceId DID2 = DeviceId.deviceId("foo:d2");
    private static final DeviceId DID3 = DeviceId.deviceId("foo:d3");

    private static final NodeId NID1 = NodeId.nodeId("n1");

    private static final List<Set<NodeId>> MASTERS = ImmutableList.of(ImmutableSet.of(NID1));

    private TestManager manager = new TestManager();
    private RegionAdminService service;
    private TestStore store = new TestStore();
    private TestListener listener = new TestListener();

    @Before
    public void setUp() throws Exception {
        TestUtils.setField(store, "storageService", new TestStorageService());
        store.activate();

        manager.store = store;
        manager.addListener(listener);
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());
        manager.activate();
        service = manager;
    }

    @After
    public void tearDown() {
        store.deactivate();
        manager.removeListener(listener);
        manager.deactivate();
        NetTestTools.injectEventDispatcher(manager, null);
    }

    @Test
    public void basics() {
        Region r1 = service.createRegion(RID1, "R1", METRO, MASTERS);
        assertEquals("incorrect id", RID1, r1.id());
        assertEquals("incorrect event", REGION_ADDED, listener.event.type());

        Region r2 = service.createRegion(RID2, "R2", CAMPUS, MASTERS);
        assertEquals("incorrect id", RID2, r2.id());
        assertEquals("incorrect type", CAMPUS, r2.type());
        assertEquals("incorrect event", REGION_ADDED, listener.event.type());

        r2 = service.updateRegion(RID2, "R2", COUNTRY, MASTERS);
        assertEquals("incorrect type", COUNTRY, r2.type());
        assertEquals("incorrect event", REGION_UPDATED, listener.event.type());

        Set<Region> regions = service.getRegions();
        assertEquals("incorrect size", 2, regions.size());
        assertTrue("missing r1", regions.contains(r1));
        assertTrue("missing r2", regions.contains(r2));

        r1 = service.getRegion(RID1);
        assertEquals("incorrect id", RID1, r1.id());

        service.removeRegion(RID1);
        regions = service.getRegions();
        assertEquals("incorrect size", 1, regions.size());
        assertTrue("missing r2", regions.contains(r2));
        assertEquals("incorrect event", REGION_REMOVED, listener.event.type());
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateCreate() {
        service.createRegion(RID1, "R1", METRO, MASTERS);
        service.createRegion(RID1, "R2", CAMPUS, MASTERS);
    }

    @Test(expected = ItemNotFoundException.class)
    public void missingUpdate() {
        service.updateRegion(RID1, "R1", METRO, MASTERS);
    }

    @Test
    public void membership() {
        Region r = service.createRegion(RID1, "R1", METRO, MASTERS);
        assertTrue("no devices expected", service.getRegionDevices(RID1).isEmpty());
        assertNull("no region expected", service.getRegionForDevice(DID1));

        service.addDevices(RID1, ImmutableSet.of(DID1, DID2));
        Set<DeviceId> deviceIds = service.getRegionDevices(RID1);
        assertEquals("incorrect device count", 2, deviceIds.size());
        assertTrue("missing d1", deviceIds.contains(DID1));
        assertTrue("missing d2", deviceIds.contains(DID2));
        assertEquals("wrong region", r, service.getRegionForDevice(DID1));
        assertEquals("incorrect event", REGION_MEMBERSHIP_CHANGED, listener.event.type());

        service.addDevices(RID1, ImmutableSet.of(DID3));
        deviceIds = service.getRegionDevices(RID1);
        assertEquals("incorrect device count", 3, deviceIds.size());
        assertTrue("missing d3", deviceIds.contains(DID3));
        assertEquals("incorrect event", REGION_MEMBERSHIP_CHANGED, listener.event.type());

        service.addDevices(RID1, ImmutableSet.of(DID3, DID1));
        deviceIds = service.getRegionDevices(RID1);
        assertEquals("incorrect device count", 3, deviceIds.size());

        service.removeDevices(RID1, ImmutableSet.of(DID2, DID3));
        deviceIds = service.getRegionDevices(RID1);
        assertEquals("incorrect device count", 1, deviceIds.size());
        assertTrue("missing d1", deviceIds.contains(DID1));

        service.removeDevices(RID1, ImmutableSet.of(DID1, DID3));
        assertTrue("no devices expected", service.getRegionDevices(RID1).isEmpty());

        service.removeDevices(RID1, ImmutableSet.of(DID2));
        assertTrue("no devices expected", service.getRegionDevices(RID1).isEmpty());
    }

    private class TestStore extends DistributedRegionStore {
        @Override
        protected void activate() {
            super.activate();
        }

        @Override
        protected void deactivate() {
            super.deactivate();
        }
    }

    private class TestManager extends RegionManager {
        TestManager() {
            eventDispatcher = new TestEventDispatcher();
            networkConfigService = new NetworkConfigServiceAdapter();
        }
    }

    private class TestListener implements RegionListener {
        RegionEvent event;

        @Override
        public void event(RegionEvent event) {
            this.event = event;
        }
    }
}