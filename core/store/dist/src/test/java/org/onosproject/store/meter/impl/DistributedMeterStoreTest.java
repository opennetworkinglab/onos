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
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.TestApplicationId;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipServiceAdapter;
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
import org.onosproject.net.meter.MeterFeatures;
import org.onosproject.net.meter.MeterFeaturesKey;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterKey;
import org.onosproject.net.meter.MeterState;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TestStorageService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.did;

/**
 * Meter store tests.
 */
public class DistributedMeterStoreTest {

    // Test node id
    private static final NodeId NID_LOCAL = new NodeId("local");

    // Test ip address
    private static final IpAddress LOCALHOST = IpAddress.valueOf("127.0.0.1");

    // Store under testing
    private DistributedMeterStore meterStore;

    // Device ids used during the tests
    private DeviceId did1 = did("1");
    private DeviceId did2 = did("2");
    private DeviceId did3 = did("3");
    private DeviceId did4 = did("4");

    // Meter ids used during the tests
    private MeterId mid1 = MeterId.meterId(1);
    private MeterId mid2 = MeterId.meterId(2);
    private MeterId mid10 = MeterId.meterId(10);

    // Bands used during the tests
    private Band b1 = DefaultBand.builder()
            .ofType(Band.Type.DROP)
            .withRate(500)
            .build();

    // Meters used during the tests
    private Meter m1 = DefaultMeter.builder()
            .forDevice(did1)
            .fromApp(APP_ID)
            .withId(mid1)
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1))
            .build();

    // Meter features used during the tests
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

    @Before
    public void setup() {
        // Init step
        meterStore = new DistributedMeterStore();
        // Let's initialize some internal services
        TestUtils.setField(meterStore, "storageService", new TestStorageService());
        TestUtils.setField(meterStore, "clusterService", new TestClusterService());
        TestUtils.setField(meterStore, "mastershipService", new TestMastershipService());
        TestUtils.setField(meterStore, "driverService", new TestDriverService());

        // Inject TestApplicationId into the DistributedMeterStore serializer
        KryoNamespace.Builder testKryoBuilder = TestUtils.getField(meterStore, "APP_KRYO_BUILDER");
        testKryoBuilder.register(TestApplicationId.class);
        Serializer testSerializer = Serializer.using(Lists.newArrayList(testKryoBuilder.build()));
        TestUtils.setField(meterStore, "serializer", testSerializer);

        // Activate the store
        meterStore.activate();
    }

    @After
    public void tearDown() {
        // Deactivate the store
        meterStore.deactivate();
    }

    private void initMeterStore() {
        // Let's store feature for device 1
        meterStore.storeMeterFeatures(mef1);
        // Let's store feature for device 2
        meterStore.storeMeterFeatures(mef2);
    }

    /**
     * Test proper store of meter features.
     */
    @Test
    public void testStoreMeterFeatures() {
        // Let's store feature for device 1
        meterStore.storeMeterFeatures(mef1);
        // Verify store meter features
        assertThat(meterStore.getMaxMeters(MeterFeaturesKey.key(did1)), is(3L));
        // Let's store feature for device 1
        meterStore.storeMeterFeatures(mef2);
        // Verify store meter features
        assertThat(meterStore.getMaxMeters(MeterFeaturesKey.key(did2)), is(10L));
    }

    /**
     * Test proper delete of meter features.
     */
    @Test
    public void testDeleteMeterFeatures() {
        // Let's store feature for device 1
        meterStore.storeMeterFeatures(mef1);
        // Verify store meter features
        assertThat(meterStore.getMaxMeters(MeterFeaturesKey.key(did1)), is(3L));
        // Let's delete the features
        meterStore.deleteMeterFeatures(did1);
        // Verify delete meter features
        assertThat(meterStore.getMaxMeters(MeterFeaturesKey.key(did1)), is(0L));
    }

    /**
     * Test proper allocation of meter ids.
     */
    @Test
    public void testAllocateId() {
        // Init the store
        initMeterStore();
        // Allocate a meter id and verify is equal to mid1
        assertThat(mid1, is(meterStore.allocateMeterId(did1)));
        // Allocate a meter id and verify is equal to mid2
        assertThat(mid2, is(meterStore.allocateMeterId(did1)));
    }

    /**
     * Test proper free of meter ids.
     */
    @Test
    public void testFreeId() {
        // Init the store
        initMeterStore();
        // Allocate a meter id and verify is equal to mid1
        assertThat(mid1, is(meterStore.allocateMeterId(did1)));
        // Free the above id
        meterStore.freeMeterId(did1, mid1);
        // Allocate a meter id and verify is equal to mid1
        assertThat(mid1, is(meterStore.allocateMeterId(did1)));
        // Free an id not allocated
        meterStore.freeMeterId(did1, mid10);
        // Allocate a meter id and verify is equal to mid2
        assertThat(mid2, is(meterStore.allocateMeterId(did1)));
    }

    /**
     * Test proper reuse of meter ids.
     */
    @Test
    public void testReuseId() {
        // Init the store
        initMeterStore();
        // Reserve id 1
        MeterId meterIdOne = meterStore.allocateMeterId(did2);
        // Free the above id
        meterStore.freeMeterId(did2, meterIdOne);
        // Start an async reservation
        CompletableFuture<MeterId> future = CompletableFuture.supplyAsync(
                () -> meterStore.allocateMeterId(did2)
        );
        // Start another reservation
        MeterId meterIdTwo = meterStore.allocateMeterId(did2);
        try {
            meterIdOne = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        // Ids should be different, otherwise we had clash in the store
        assertNotEquals("Ids should be different", meterIdOne, meterIdTwo);

        // Free the above id
        meterStore.freeMeterId(did1, meterIdOne);
        // Free the above id
        meterStore.freeMeterId(did1, meterIdTwo);
        // Reserve id 1
        meterIdOne = meterStore.allocateMeterId(did2);
        // Reserve id 2
        meterStore.allocateMeterId(did2);
        // Reserve id 3
        MeterId meterIdThree = meterStore.allocateMeterId(did2);
        // Reserve id 4
        MeterId meterIdFour = meterStore.allocateMeterId(did2);
        // Free the above id
        meterStore.freeMeterId(did1, meterIdOne);
        // Free the above id
        meterStore.freeMeterId(did1, meterIdThree);
        // Free the above id
        meterStore.freeMeterId(did1, meterIdFour);
        // Start an async reservation
        future = CompletableFuture.supplyAsync(
                () -> meterStore.allocateMeterId(did2)
        );
        // Start another reservation
        MeterId meterAnotherId = meterStore.allocateMeterId(did2);
        try {
            meterAnotherId = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        // Ids should be different, otherwise we had clash in the store
        assertNotEquals("Ids should be different", meterAnotherId, meterIdOne);
    }

    /**
     * Test query meters mechanism.
     */
    @Test
    public void testQueryMeters() {
        // Init the store
        initMeterStore();
        // Let's test queryMeters
        assertThat(mid1, is(meterStore.allocateMeterId(did3)));
        // Let's test queryMeters error
        assertNull(meterStore.allocateMeterId(did4));
    }

    /**
     * Test max meter error.
     */
    @Test
    public void testMaxMeterError() {
        // Init the store
        initMeterStore();
        // Reserve id 1
        assertThat(mid1, is(meterStore.allocateMeterId(did1)));
        // Reserve id 2
        assertThat(mid2, is(meterStore.allocateMeterId(did1)));
        // Max meter error
        assertNull(meterStore.allocateMeterId(did1));
    }

    /**
     * Test store meter.
     */
    @Test
    public void testStoreMeter() {
        // Init the store
        initMeterStore();
        // Simulate the allocation of an id
        MeterId idOne = meterStore.allocateMeterId(did1);
        // Verify the allocation
        assertThat(mid1, is(idOne));
        // Let's create a meter
        Meter meterOne = DefaultMeter.builder()
                .forDevice(did1)
                .fromApp(APP_ID)
                .withId(mid1)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(b1))
                .build();
        // Set the state
        ((DefaultMeter) meterOne).setState(MeterState.PENDING_ADD);
        // Store the meter
        meterStore.storeMeter(meterOne);
        // Let's create meter key
        MeterKey meterKey = MeterKey.key(did1, mid1);
        // Verify the store
        assertThat(1, is(meterStore.getAllMeters().size()));
        assertThat(1, is(meterStore.getAllMeters(did1).size()));
        assertThat(m1, is(meterStore.getMeter(meterKey)));
    }

    /**
     * Test delete meter.
     */
    @Test
    public void testDeleteMeter() {
        // Init the store
        initMeterStore();
        // Simulate the allocation of an id
        MeterId idOne = meterStore.allocateMeterId(did1);
        // Verify the allocation
        assertThat(mid1, is(idOne));
        // Let's create a meter
        Meter meterOne = DefaultMeter.builder()
                .forDevice(did1)
                .fromApp(APP_ID)
                .withId(mid1)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(b1))
                .build();
        // Set the state
        ((DefaultMeter) meterOne).setState(MeterState.PENDING_ADD);
        // Store the meter
        meterStore.storeMeter(meterOne);
        // Set the state
        ((DefaultMeter) meterOne).setState(MeterState.PENDING_REMOVE);
        // Let's create meter key
        MeterKey meterKey = MeterKey.key(did1, mid1);
        // Delete meter
        meterStore.deleteMeter(meterOne);
        // Start an async delete, simulating the operation of the provider
        CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> meterStore.deleteMeterNow(meterOne)
        );
        // Let's wait
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        // Verify delete
        assertThat(0, is(meterStore.getAllMeters().size()));
        assertThat(0, is(meterStore.getAllMeters(did1).size()));
        assertNull(meterStore.getMeter(meterKey));
        assertThat(mid1, is(meterStore.allocateMeterId(did1)));
    }

    /**
     * Test no delete meter.
     */
    @Test
    public void testNoDeleteMeter() {
        // Init the store
        initMeterStore();
        // Simulate the allocation of an id
        MeterId idOne = meterStore.allocateMeterId(did1);
        // Create the key
        MeterKey keyOne = MeterKey.key(did1, idOne);
        // Let's create a meter
        Meter meterOne = DefaultMeter.builder()
                .forDevice(did1)
                .fromApp(APP_ID)
                .withId(mid1)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(b1))
                .build();
        // Set the state
        ((DefaultMeter) meterOne).setState(MeterState.PENDING_REMOVE);
        // Delete meter
        meterStore.deleteMeter(meterOne);
        // Verify No delete
        assertThat(0, is(meterStore.getAllMeters().size()));
        assertThat(0, is(meterStore.getAllMeters(did1).size()));
        assertNull(meterStore.getMeter(keyOne));
    }

    // Test cluster service
    private final class TestClusterService extends ClusterServiceAdapter {

        private ControllerNode local = new DefaultControllerNode(NID_LOCAL, LOCALHOST);

        @Override
        public ControllerNode getLocalNode() {
            return local;
        }

        @Override
        public Set<ControllerNode> getNodes() {
            return Sets.newHashSet();
        }

    }

    // Test mastership service
    private final class TestMastershipService extends MastershipServiceAdapter {
        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return NID_LOCAL;
        }
    }

    // Test class for driver service.
    private class TestDriverService extends DriverServiceAdapter {
        @Override
        public DriverHandler createHandler(DeviceId deviceId, String... credentials) {
            return deviceId.equals(did3) ? new TestDriverHandler() : null;
        }
    }

    // Test class for driver handler.
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

    // Test meter query
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
