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

package org.onosproject.store.region.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionEvent;
import org.onosproject.net.region.RegionId;
import org.onosproject.store.service.TestStorageService;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.onosproject.net.region.Region.Type.*;
import static org.onosproject.net.region.RegionEvent.Type.*;

/**
 * Test of the distributed region store implementation.
 */
public class DistributedRegionStoreTest {

    private static final RegionId RID1 = RegionId.regionId("r1");
    private static final RegionId RID2 = RegionId.regionId("r2");

    private static final DeviceId DID1 = DeviceId.deviceId("foo:d1");
    private static final DeviceId DID2 = DeviceId.deviceId("foo:d2");
    private static final DeviceId DID3 = DeviceId.deviceId("foo:d3");

    private static final NodeId NID1 = NodeId.nodeId("n1");

    private static final Annotations NO_ANNOTS = DefaultAnnotations.EMPTY;

    private static final List<Set<NodeId>> MASTERS = ImmutableList.of(ImmutableSet.of(NID1));

    private TestStore store;
    private RegionEvent event;

    /**
     * Sets up the device key store and the storage service test harness.
     */
    @Before
    public void setUp() {
        store = new TestStore();
        store.storageService = new TestStorageService();
        store.setDelegate(e -> this.event = e);
        store.activate();
    }

    /**
     * Tears down the device key store.
     */
    @After
    public void tearDown() {
        store.deactivate();
    }

    @Test
    public void basics() {
        Region r1 = store.createRegion(RID1, "R1", METRO, NO_ANNOTS, MASTERS);
        assertEquals("incorrect id", RID1, r1.id());
        assertEquals("incorrect event", REGION_ADDED, event.type());

        Region r2 = store.createRegion(RID2, "R2", CAMPUS, NO_ANNOTS, MASTERS);
        assertEquals("incorrect id", RID2, r2.id());
        assertEquals("incorrect type", CAMPUS, r2.type());
        assertEquals("incorrect event", REGION_ADDED, event.type());

        r2 = store.updateRegion(RID2, "R2", COUNTRY, NO_ANNOTS, MASTERS);
        assertEquals("incorrect type", COUNTRY, r2.type());
        assertEquals("incorrect event", REGION_UPDATED, event.type());

        Set<Region> regions = store.getRegions();
        assertEquals("incorrect size", 2, regions.size());
        assertTrue("missing r1", regions.contains(r1));
        assertTrue("missing r2", regions.contains(r2));

        r1 = store.getRegion(RID1);
        assertEquals("incorrect id", RID1, r1.id());

        store.removeRegion(RID1);
        regions = store.getRegions();
        assertEquals("incorrect size", 1, regions.size());
        assertTrue("missing r2", regions.contains(r2));
        assertEquals("incorrect event", REGION_REMOVED, event.type());
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateCreate() {
        store.createRegion(RID1, "R1", METRO, NO_ANNOTS, MASTERS);
        store.createRegion(RID1, "R2", CAMPUS, NO_ANNOTS, MASTERS);
    }

    @Test(expected = ItemNotFoundException.class)
    public void missingUpdate() {
        store.updateRegion(RID1, "R1", METRO, NO_ANNOTS, MASTERS);
    }

    @Test
    public void membership() {
        Region r = store.createRegion(RID1, "R1", METRO, NO_ANNOTS, MASTERS);
        assertTrue("no devices expected", store.getRegionDevices(RID1).isEmpty());
        assertNull("no region expected", store.getRegionForDevice(DID1));

        store.addDevices(RID1, ImmutableSet.of(DID1, DID2));
        Set<DeviceId> deviceIds = store.getRegionDevices(RID1);
        assertEquals("incorrect device count", 2, deviceIds.size());
        assertTrue("missing d1", deviceIds.contains(DID1));
        assertTrue("missing d2", deviceIds.contains(DID2));
        assertEquals("wrong region", r, store.getRegionForDevice(DID1));

        store.addDevices(RID1, ImmutableSet.of(DID3));
        deviceIds = store.getRegionDevices(RID1);
        assertEquals("incorrect device count", 3, deviceIds.size());
        assertTrue("missing d3", deviceIds.contains(DID3));

        store.addDevices(RID1, ImmutableSet.of(DID3, DID1));
        deviceIds = store.getRegionDevices(RID1);
        assertEquals("incorrect device count", 3, deviceIds.size());

        // Test adding DID3 to RID2 but it is already in RID1.
        // DID3 will be removed from RID1 and added to RID2.
        Region r2 = store.createRegion(RID2, "R2", CAMPUS, NO_ANNOTS, MASTERS);
        store.addDevices(RID2, ImmutableSet.of(DID3));
        deviceIds = store.getRegionDevices(RID1);
        assertEquals("incorrect device count", 2, deviceIds.size());
        deviceIds = store.getRegionDevices(RID2);
        assertEquals("incorrect device count", 1, deviceIds.size());

        store.removeDevices(RID1, ImmutableSet.of(DID2, DID3));
        deviceIds = store.getRegionDevices(RID1);
        assertEquals("incorrect device count", 1, deviceIds.size());
        assertTrue("missing d1", deviceIds.contains(DID1));

        store.removeDevices(RID1, ImmutableSet.of(DID1, DID3));
        assertTrue("no devices expected", store.getRegionDevices(RID1).isEmpty());

        store.removeDevices(RID1, ImmutableSet.of(DID2));
        assertTrue("no devices expected", store.getRegionDevices(RID1).isEmpty());
    }

    class TestStore extends DistributedRegionStore {
    }
}