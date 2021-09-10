/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.store.meter.impl;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.util.KryoNamespace;
import org.onosproject.TestApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.MeterQuery;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.DefaultMeterFeatures;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterCellId;
import org.onosproject.net.meter.MeterFeatures;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterKey;
import org.onosproject.net.meter.MeterScope;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.meter.MeterTableKey;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TestStorageService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.APP_ID_2;
import static org.onosproject.net.NetTestTools.did;

/**
 * Meter store tests.
 */
public class DistributedMeterStoreTest {

    private DistributedMeterStore meterStore;

    private DeviceId did1 = did("1");
    private DeviceId did2 = did("2");
    private DeviceId did3 = did("3");
    private DeviceId did4 = did("4");

    private MeterId mid1 = MeterId.meterId(1);
    private MeterId mid2 = MeterId.meterId(2);
    private MeterId mid3 = MeterId.meterId(3);
    private MeterId mid5 = MeterId.meterId(5);
    private MeterId mid6 = MeterId.meterId(6);
    private MeterId mid10 = MeterId.meterId(10);
    private MeterCellId cid4 = PiMeterCellId.ofIndirect(
            PiMeterId.of("foo"), 4);
    private MeterCellId invalidCid = PiMeterCellId.ofIndirect(
            PiMeterId.of("foo"), 11);

    private Band b1 = DefaultBand.builder()
            .ofType(Band.Type.DROP)
            .withRate(500)
            .build();

    private Meter m1 = DefaultMeter.builder()
            .forDevice(did1)
            .fromApp(APP_ID)
            .withId(mid1)
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1))
            .build();
    private Meter m2 = DefaultMeter.builder()
            .forDevice(did1)
            .fromApp(APP_ID_2)
            .withCellId(mid2)
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1))
            .build();
    private Meter m3 = DefaultMeter.builder()
            .forDevice(did2)
            .fromApp(APP_ID_2)
            .withCellId(mid3)
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1))
            .build();
    private Meter m4 = DefaultMeter.builder()
            .forDevice(did3)
            .fromApp(APP_ID)
            .withCellId(cid4)
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1))
            .build();

    private MeterFeatures mef1 = DefaultMeterFeatures.builder().forDevice(did1)
            .withMaxMeters(3L)
            .withBandTypes(new HashSet<>())
            .withUnits(new HashSet<>())
            .hasStats(false)
            .hasBurst(false)
            .withMaxBands((byte) 0)
            .withMaxColors((byte) 0)
            .build();
    private MeterFeatures mef2 = DefaultMeterFeatures.builder().forDevice(did2)
            .withMaxMeters(10L)
            .withBandTypes(new HashSet<>())
            .withUnits(new HashSet<>())
            .hasStats(false)
            .hasBurst(false)
            .withMaxBands((byte) 0)
            .withMaxColors((byte) 0)
            .build();
    private MeterFeatures mef3 = DefaultMeterFeatures.builder().forDevice(did3)
            .withStartIndex(0L)
            .withEndIndex(10L)
            .withScope(MeterScope.of("foo"))
            .withBandTypes(new HashSet<>())
            .withUnits(new HashSet<>())
            .hasStats(false)
            .hasBurst(false)
            .withMaxBands((byte) 0)
            .withMaxColors((byte) 0)
            .build();

    @Before
    public void setup() {
        meterStore = new DistributedMeterStore();

        TestUtils.setField(meterStore, "storageService", new TestStorageService());
        TestUtils.setField(meterStore, "driverService", new TestDriverService());
        KryoNamespace.Builder testKryoBuilder = TestUtils.getField(meterStore, "APP_KRYO_BUILDER");
        testKryoBuilder.register(TestApplicationId.class);
        Serializer testSerializer = Serializer.using(Lists.newArrayList(testKryoBuilder.build()));
        TestUtils.setField(meterStore, "serializer", testSerializer);

        meterStore.activate();
    }

    @After
    public void tearDown() {
        meterStore.deactivate();
    }

    private void initMeterStore(boolean enableUserDefinedIndex) {
        meterStore.userDefinedIndexMode(enableUserDefinedIndex);
        meterStore.storeMeterFeatures(mef1);
        meterStore.storeMeterFeatures(mef2);
        meterStore.storeMeterFeatures(mef3);
    }

    /**
     * Test proper store of meter features.
     */
    @Test
    public void testStoreMeterFeatures() {
        initMeterStore(false);

        assertThat(meterStore.getMaxMeters(MeterTableKey.key(did1, MeterScope.globalScope())), is(3L));
        assertThat(meterStore.getMaxMeters(MeterTableKey.key(did2, MeterScope.globalScope())), is(10L));
    }

    /**
     * Test proper delete of meter features.
     */
    @Test
    public void testDeleteMeterFeatures() {
        initMeterStore(false);
        assertThat(meterStore.getMaxMeters(MeterTableKey.key(did1, MeterScope.globalScope())), is(3L));

        meterStore.deleteMeterFeatures(did1);
        assertThat(meterStore.getMaxMeters(MeterTableKey.key(did1, MeterScope.globalScope())), is(0L));
    }

    /**
     * Test proper allocation of meter ids.
     */
    @Test
    public void testAllocateId() {
        initMeterStore(false);

        assertThat(mid1, is(meterStore.allocateMeterId(did1, MeterScope.globalScope())));
        assertThat(mid2, is(meterStore.allocateMeterId(did1, MeterScope.globalScope())));
    }

    /**
     * Test proper free of meter ids.
     */
    @Test
    public void testFreeId() {
        initMeterStore(false);
        assertThat(mid1, is(meterStore.allocateMeterId(did1, MeterScope.globalScope())));

        // Verify reuse strategy
        meterStore.freeMeterId(did1, mid1);
        assertThat(mid1, is(meterStore.allocateMeterId(did1, MeterScope.globalScope())));

        // Following free does not have effect
        meterStore.freeMeterId(did1, mid10);
        assertThat(mid2, is(meterStore.allocateMeterId(did1, MeterScope.globalScope())));
    }

    /**
     * Test proper reuse of meter ids.
     */
    @Test
    public void testReuseId() {
        initMeterStore(false);

        MeterCellId meterIdOne = meterStore.allocateMeterId(did2, MeterScope.globalScope());
        meterStore.freeMeterId(did2, meterIdOne);
        CompletableFuture<MeterCellId> future = CompletableFuture.supplyAsync(
                () -> meterStore.allocateMeterId(did2, MeterScope.globalScope())
        );
        MeterCellId meterIdTwo = meterStore.allocateMeterId(did2, MeterScope.globalScope());
        try {
            meterIdOne = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assertNotEquals("Ids should be different", meterIdOne, meterIdTwo);

        meterStore.freeMeterId(did1, meterIdOne);
        meterStore.freeMeterId(did1, meterIdTwo);
        meterIdOne = meterStore.allocateMeterId(did2, MeterScope.globalScope());
        meterStore.allocateMeterId(did2, MeterScope.globalScope());
        MeterCellId meterIdThree = meterStore.allocateMeterId(did2, MeterScope.globalScope());
        MeterCellId meterIdFour = meterStore.allocateMeterId(did2, MeterScope.globalScope());
        meterStore.freeMeterId(did1, meterIdOne);
        meterStore.freeMeterId(did1, meterIdThree);
        meterStore.freeMeterId(did1, meterIdFour);
        future = CompletableFuture.supplyAsync(
                () -> meterStore.allocateMeterId(did2, MeterScope.globalScope())
        );
        MeterCellId meterAnotherId = meterStore.allocateMeterId(did2, MeterScope.globalScope());
        try {
            meterAnotherId = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assertNotEquals("Ids should be different", meterAnotherId, meterIdOne);
    }

    /**
     * Test query meters mechanism.
     */
    @Test
    public void testQueryMeters() {
        initMeterStore(false);

        assertThat(mid1, is(meterStore.allocateMeterId(did3, MeterScope.globalScope())));
        assertNull(meterStore.allocateMeterId(did4, MeterScope.globalScope()));
    }

    /**
     * Test max meter error.
     */
    @Test
    public void testMaxMeterError() {
        initMeterStore(false);

        assertThat(mid1, is(meterStore.allocateMeterId(did1, MeterScope.globalScope())));
        assertThat(mid2, is(meterStore.allocateMeterId(did1, MeterScope.globalScope())));
        assertThat(mid3, is(meterStore.allocateMeterId(did1, MeterScope.globalScope())));
        assertNull(meterStore.allocateMeterId(did1, MeterScope.globalScope()));
    }

    /**
     * Test store meter.
     */
    @Test
    public void testStoreMeter() {
        initMeterStore(false);

        MeterCellId idOne = meterStore.allocateMeterId(did1, MeterScope.globalScope());
        assertThat(mid1, is(idOne));

        Meter meterOne = DefaultMeter.builder()
                .forDevice(did1)
                .fromApp(APP_ID)
                .withId(mid1)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(b1))
                .build();
        ((DefaultMeter) meterOne).setState(MeterState.PENDING_ADD);
        meterStore.addOrUpdateMeter(meterOne);
        MeterKey meterKey = MeterKey.key(did1, mid1);
        assertThat(1, is(meterStore.getAllMeters().size()));
        assertThat(1, is(meterStore.getAllMeters(did1).size()));
        assertThat(m1, is(meterStore.getMeter(meterKey)));
    }

    /**
     * Test delete meter.
     */
    @Test
    public void testDeleteMeter() {
        initMeterStore(false);

        MeterCellId idOne = meterStore.allocateMeterId(did1, MeterScope.globalScope());
        assertThat(mid1, is(idOne));

        Meter meterOne = DefaultMeter.builder()
                .forDevice(did1)
                .fromApp(APP_ID)
                .withId(mid1)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(b1))
                .build();
        ((DefaultMeter) meterOne).setState(MeterState.PENDING_ADD);
        meterStore.addOrUpdateMeter(meterOne);
        ((DefaultMeter) meterOne).setState(MeterState.PENDING_REMOVE);
        MeterKey meterKey = MeterKey.key(did1, mid1);
        meterStore.deleteMeter(meterOne);
        CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> meterStore.purgeMeter(meterOne)
        );
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assertThat(0, is(meterStore.getAllMeters().size()));
        assertThat(0, is(meterStore.getAllMeters(did1).size()));
        assertNull(meterStore.getMeter(meterKey));
        assertThat(mid1, is(meterStore.allocateMeterId(did1, MeterScope.globalScope())));
    }

    /**
     * Test no delete meter.
     */
    @Test
    public void testNoDeleteMeter() {
        initMeterStore(false);

        MeterCellId idOne = meterStore.allocateMeterId(did1, MeterScope.globalScope());
        MeterKey keyOne = MeterKey.key(did1, idOne);
        Meter meterOne = DefaultMeter.builder()
                .forDevice(did1)
                .fromApp(APP_ID)
                .withId(mid1)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(b1))
                .build();
        ((DefaultMeter) meterOne).setState(MeterState.PENDING_REMOVE);
        meterStore.deleteMeter(meterOne);
        assertThat(0, is(meterStore.getAllMeters().size()));
        assertThat(0, is(meterStore.getAllMeters(did1).size()));
        assertNull(meterStore.getMeter(keyOne));
    }

    /**
     * Test purge meter.
     */
    @Test
    public void testPurgeMeters() {
        testStoreMeter();

        meterStore.purgeMeters(did1);
        MeterKey keyOne = MeterKey.key(did1, mid1);
        assertThat(0, is(meterStore.getAllMeters().size()));
        assertThat(0, is(meterStore.getAllMeters(did1).size()));
        assertNull(meterStore.getMeter(keyOne));
    }

    /**
     * Test purge meter given device and application.
     */
    @Test
    public void testPurgeMetersDeviceAndApp() {
        initMeterStore(false);

        ((DefaultMeter) m1).setState(MeterState.PENDING_ADD);
        ((DefaultMeter) m2).setState(MeterState.PENDING_ADD);
        ((DefaultMeter) m3).setState(MeterState.PENDING_ADD);
        meterStore.addOrUpdateMeter(m1);
        meterStore.addOrUpdateMeter(m2);
        meterStore.addOrUpdateMeter(m3);
        assertThat(3, is(meterStore.getAllMeters().size()));

        meterStore.purgeMeters(did1, APP_ID_2);
        MeterKey keyTwo = MeterKey.key(did1, mid2);
        assertThat(2, is(meterStore.getAllMeters().size()));
        assertThat(1, is(meterStore.getAllMeters(did1).size()));
        assertThat(1, is(meterStore.getAllMeters(did2).size()));
        assertNull(meterStore.getMeter(keyTwo));
    }

    /**
     * Test getMeters API immutability.
     */
    @Test
    public void testGetMetersImmutability() {
        initMeterStore(false);

        MeterCellId idOne = meterStore.allocateMeterId(did1, MeterScope.globalScope());
        assertThat(mid1, is(idOne));
        Meter meterOne = DefaultMeter.builder()
                .forDevice(did1)
                .fromApp(APP_ID)
                .withId(mid1)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(b1))
                .build();
        ((DefaultMeter) meterOne).setState(MeterState.PENDING_ADD);
        meterStore.addOrUpdateMeter(meterOne);

        Collection<Meter> meters = meterStore.getAllMeters();
        Collection<Meter> metersDevice = meterStore.getAllMeters(did1);
        assertThat(1, is(meters.size()));
        assertThat(1, is(metersDevice.size()));

        MeterCellId idTwo = meterStore.allocateMeterId(did1, MeterScope.globalScope());
        assertThat(mid2, is(idTwo));
        Meter meterTwo = DefaultMeter.builder()
                .forDevice(did1)
                .fromApp(APP_ID)
                .withId(mid2)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(b1))
                .build();
        ((DefaultMeter) meterTwo).setState(MeterState.PENDING_ADD);
        meterStore.addOrUpdateMeter(meterTwo);

        assertThat(1, is(meters.size()));
        assertThat(1, is(metersDevice.size()));
        meters = meterStore.getAllMeters();
        metersDevice = meterStore.getAllMeters(did1);
        assertThat(2, is(meters.size()));
        assertThat(2, is(metersDevice.size()));
    }

    /**
     * Test invalid allocation of a cell id.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCellId() {
        initMeterStore(true);

        // MF defines an end index equals to 10
        Meter meterBad = DefaultMeter.builder()
                .forDevice(did3)
                .fromApp(APP_ID)
                .withCellId(invalidCid)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(b1))
                .build();
        ((DefaultMeter) meterBad).setState(MeterState.PENDING_ADD);
        meterStore.addOrUpdateMeter(meterBad);
    }

    /**
     * Test enabling user defined index mode.
     */
    @Test
    public void testEnableUserDefinedIndex() {
        initMeterStore(false);

        assertTrue(meterStore.userDefinedIndexMode(true));
    }

    /**
     * Test invalid enabling user defined index mode.
     */
    @Test
    public void testInvalidEnableUserDefinedIndex() {
        testStoreMeter();

        assertFalse(meterStore.userDefinedIndexMode(true));
    }

    /**
     * Test disabling user defined index mode.
     */
    @Test
    public void testDisableUserDefinedIndex() {
        initMeterStore(true);

        assertFalse(meterStore.userDefinedIndexMode(false));
    }

    /**
     * Test store meter in user defined index mode.
     */
    @Test
    public void testStoreMeterInUserDefinedIndexMode() {
        initMeterStore(true);

        Meter meterOne = DefaultMeter.builder()
                .forDevice(did3)
                .fromApp(APP_ID)
                .withCellId(cid4)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(b1))
                .build();
        ((DefaultMeter) meterOne).setState(MeterState.PENDING_ADD);
        meterStore.addOrUpdateMeter(meterOne);
        MeterKey meterKey = MeterKey.key(did3, cid4);
        assertThat(1, is(meterStore.getAllMeters().size()));
        assertThat(1, is(meterStore.getAllMeters(did3).size()));
        assertThat(m4, is(meterStore.getMeter(meterKey)));
    }

    /**
     * Test invalid disabling user defined index mode.
     */
    @Test
    public void testInvalidDisableUserDefinedIndex() {
        testStoreMeterInUserDefinedIndexMode();

        assertTrue(meterStore.userDefinedIndexMode(false));
    }

    /**
     * Test allocation of meter ids in user defined index mode.
     */
    @Test
    public void testAllocateIdInUserDefinedIndexMode() {
        initMeterStore(true);

        assertNull(meterStore.allocateMeterId(did1, MeterScope.globalScope()));
    }

    /**
     * Test free of meter ids in user defined index mode.
     */
    @Test
    public void testFreeIdInUserMode() {
        initMeterStore(true);

        meterStore.freeMeterId(did1, mid1);
        MeterTableKey globalKey = MeterTableKey.key(did1, MeterScope.globalScope());
        assertNotNull(meterStore.availableMeterIds.get(globalKey));
        assertTrue(meterStore.availableMeterIds.get(globalKey).isEmpty());
    }

    /**
     * Test delete meter in user defined index mode.
     */
    @Test
    public void testDeleteMeterInUserDefinedIndexMode() {
        initMeterStore(true);

        Meter meterOne = DefaultMeter.builder()
                .forDevice(did3)
                .fromApp(APP_ID)
                .withCellId(cid4)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(b1))
                .build();
        ((DefaultMeter) meterOne).setState(MeterState.PENDING_ADD);
        meterStore.addOrUpdateMeter(meterOne);

        ((DefaultMeter) meterOne).setState(MeterState.PENDING_REMOVE);
        MeterKey meterKey = MeterKey.key(did3, cid4);
        meterStore.deleteMeter(meterOne);
        CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> meterStore.purgeMeter(meterOne)
        );

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assertThat(0, is(meterStore.getAllMeters().size()));
        assertThat(0, is(meterStore.getAllMeters(did3).size()));
        assertNull(meterStore.getMeter(meterKey));
        MeterTableKey globalKey = MeterTableKey.key(did1, MeterScope.globalScope());
        assertNotNull(meterStore.availableMeterIds.get(globalKey));
        assertTrue(meterStore.availableMeterIds.get(globalKey).isEmpty());
    }

    private class TestDriverService extends DriverServiceAdapter {
        @Override
        public DriverHandler createHandler(DeviceId deviceId, String... credentials) {
            return deviceId.equals(did3) ? new TestDriverHandler() : null;
        }
    }

    private class TestDriverHandler implements DriverHandler {

        @Override
        public Driver driver() {
            return null;
        }

        @Override
        public DriverData data() {
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Behaviour> T behaviour(Class<T> behaviourClass) {
            return (T) new TestMeterQuery();
        }

        @Override
        public <T> T get(Class<T> serviceClass) {
            return null;
        }

        @Override
        public boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
            return true;
        }
    }

    private class TestMeterQuery implements MeterQuery {

        @Override
        public DriverData data() {
            return null;
        }

        @Override
        public void setData(DriverData data) {

        }
        @Override
        public DriverHandler handler() {
            return null;
        }

        @Override
        public void setHandler(DriverHandler handler) {

        }

        @Override
        public long getMaxMeters() {
            return 100;
        }
    }

}
