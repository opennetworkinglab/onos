/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.meter.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestTools;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.TestApplicationId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.incubator.store.meter.impl.DistributedMeterStore;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.behaviour.MeterQuery;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.DriverRegistry;
import org.onosproject.net.driver.impl.DriverManager;
import org.onosproject.net.driver.impl.DriverRegistryManager;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.DefaultMeterFeatures;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterFeatures;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterOperations;
import org.onosproject.net.meter.MeterProgrammable;
import org.onosproject.net.meter.MeterProvider;
import org.onosproject.net.meter.MeterProviderRegistry;
import org.onosproject.net.meter.MeterProviderService;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TestStorageService;

import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.did;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Meter manager tests.
 */
public class MeterManagerTest {

    // Test node id
    private static final NodeId NID_LOCAL = new NodeId("local");

    // Test ip address
    private static final IpAddress LOCALHOST = IpAddress.valueOf("127.0.0.1");

    private static final ProviderId PID = new ProviderId("of", "foo");

    private static final ProviderId PROGRAMMABLE_PROVIDER = new ProviderId("foo", "foo");
    private static final DeviceId PROGRAMMABLE_DID = DeviceId.deviceId("test:002");

    private static final DefaultAnnotations ANNOTATIONS =
            DefaultAnnotations.builder().set(AnnotationKeys.DRIVER, "foo").build();

    private static final Device PROGRAMMABLE_DEV =
            new DefaultDevice(PROGRAMMABLE_PROVIDER, PROGRAMMABLE_DID, Device.Type.SWITCH,
                    "", "", "", "", null, ANNOTATIONS);


    private MeterService service;

    // Test Driver service used during the tests
    private DriverManager driverService;

    // Test device service used during the tests
    private DeviceService deviceService;

    // Test provider used during the tests
    private TestProvider provider;

    // Meter manager
    private MeterManager manager;

    // Meter provider registry
    private MeterProviderRegistry registry;

    // Meter provider service
    private MeterProviderService providerService;

    // Store under testing
    private DistributedMeterStore meterStore;

    // Device ids used during the tests
    private DeviceId did1 = did("1");
    private DeviceId did2 = did("2");

    // Meter ids used during the tests
    private MeterId mid1 = MeterId.meterId(1);

    // Bands used during the tests
    private static Band b1 = DefaultBand.builder()
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
    private Meter m2 = DefaultMeter.builder()
            .forDevice(did2)
            .fromApp(APP_ID)
            .withId(mid1)
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1))
            .build();

    private static Meter mProgrammable = DefaultMeter.builder()
            .forDevice(PROGRAMMABLE_DID)
            .fromApp(APP_ID)
            .withId(MeterId.meterId(1))
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1))
            .build();

    // Meter requests used during the tests
    private MeterRequest.Builder m1Request = DefaultMeterRequest.builder()
            .forDevice(did1)
            .fromApp(APP_ID)
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1));
    private MeterRequest.Builder m2Request = DefaultMeterRequest.builder()
            .forDevice(did2)
            .fromApp(APP_ID)
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1));

    private MeterRequest.Builder mProgrammableRequest = DefaultMeterRequest.builder()
            .forDevice(PROGRAMMABLE_DID)
            .fromApp(APP_ID)
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1));

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
        //Init step for the deviceService
        deviceService = new TestDeviceService();
        //Init step for the driver registry and driver service.
        DriverRegistryManager driverRegistry = new DriverRegistryManager();
        driverService = new TestDriverManager(driverRegistry, deviceService, new NetworkConfigServiceAdapter());
        driverRegistry.addDriver(new DefaultDriver("foo", ImmutableList.of(), "",
                "", "",
                ImmutableMap.of(MeterProgrammable.class,
                        TestMeterProgrammable.class, MeterQuery.class, TestMeterQuery.class),
                ImmutableMap.of()));

        // Init step for the store
        meterStore = new DistributedMeterStore();
        // Let's initialize some internal services of the store
        TestUtils.setField(meterStore, "storageService", new TestStorageService());
        TestUtils.setField(meterStore, "clusterService", new TestClusterService());
        TestUtils.setField(meterStore, "mastershipService", new TestMastershipService());
        TestUtils.setField(meterStore, "driverService", driverService);

        // Inject TestApplicationId into the DistributedMeterStore serializer
        KryoNamespace.Builder testKryoBuilder = TestUtils.getField(meterStore, "APP_KRYO_BUILDER");
        testKryoBuilder.register(TestApplicationId.class);
        Serializer testSerializer = Serializer.using(Lists.newArrayList(testKryoBuilder.build()));
        TestUtils.setField(meterStore, "serializer", testSerializer);

        // Activate the store
        meterStore.activate();
        // Init step for the manager
        manager = new MeterManager();
        // Let's initialize some internal services of the manager
        TestUtils.setField(manager, "store", meterStore);
        injectEventDispatcher(manager, new TestEventDispatcher());
        manager.deviceService = deviceService;
        manager.mastershipService = new TestMastershipService();
        manager.cfgService = new ComponentConfigAdapter();
        TestUtils.setField(manager, "storageService", new TestStorageService());
        // Init the reference of the registry
        registry = manager;

        manager.driverService = driverService;

        // Activate the manager
        manager.activate(null);
        // Initialize the test provider

        provider = new TestProvider(PID);
        // Register the provider against the manager
        providerService = registry.register(provider);
        // Verify register
        assertTrue("provider should be registered",
                   registry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        // Unregister provider
        registry.unregister(provider);
        // Verify unregister
        assertFalse("provider should not be registered",
                    registry.getProviders().contains(provider.id()));
        // Deactivate manager
        manager.deactivate();
        // Remove event dispatcher
        injectEventDispatcher(manager, null);
        // Deactivate store
        meterStore.deactivate();
    }

    private void initMeterStore() {
        // Let's store feature for device 1
        meterStore.storeMeterFeatures(mef1);
        // Let's store feature for device 2
        meterStore.storeMeterFeatures(mef2);
    }

    // Emulate metrics coming from the dataplane
    private void pushMetrics(MeterOperation.Type type, Meter meter) {
        // If it is an add operation
        if (type == MeterOperation.Type.ADD) {
            // Update state to added
            ((DefaultMeter) meter).setState(MeterState.ADDED);
            // Push the update in the store
            providerService.pushMeterMetrics(meter.deviceId(), Collections.singletonList(meter));
        } else {
            providerService.pushMeterMetrics(meter.deviceId(), Collections.emptyList());
        }
    }

    /**
     * Test add meter.
     */
    @Test
    public void testAdd() {
        // Init store
        initMeterStore();
        // Submit meter request
        manager.submit(m1Request.add());
        // Verify add
        assertTrue("The meter was not added", manager.getAllMeters().size() == 1);
        assertTrue("The meter was not added", manager.getMeters(did1).size() == 1);
        // Get Meter
        Meter installingMeter = manager.getMeter(did1, mid1);
        // Verify add of installingMeter and pending add state
        assertThat(installingMeter, is(m1));
        // Verify pending add state
        assertThat(installingMeter.state(), is(MeterState.PENDING_ADD));
        // Let's simulate a working data-plane
        pushMetrics(MeterOperation.Type.ADD, installingMeter);
        // Get meter
        Meter installedMeter = manager.getMeter(did1, mid1);
        // Verify installation
        assertThat(installedMeter.state(), is(MeterState.ADDED));
        assertTrue("The meter was not installed", manager.getAllMeters().size() == 1);
        assertTrue("The meter was not installed", manager.getMeters(did1).size() == 1);
    }

    /**
     * Test remove meter.
     */
    @Test
    public void testRemove() {
        // Init store
        initMeterStore();
        // Submit meter request
        manager.submit(m1Request.add());
        // Withdraw meter
        manager.withdraw(m1Request.remove(), m1.id());
        // Get Meter
        Meter withdrawingMeter = manager.getMeter(did1, mid1);
        // Verify withdrawing
        assertThat(withdrawingMeter.state(), is(MeterState.PENDING_REMOVE));
        assertTrue("The meter was not withdrawn", manager.getAllMeters().size() == 1);
        assertTrue("The meter was not withdrawn", manager.getMeters(did1).size() == 1);
        // Let's simulate a working data-plane
        pushMetrics(MeterOperation.Type.REMOVE, withdrawingMeter);
        // Verify withdrawn
        assertNull(manager.getMeter(did1, mid1));
        assertTrue("The meter was not removed", manager.getAllMeters().size() == 0);
        assertTrue("The meter was not removed", manager.getMeters(did1).size() == 0);
    }

    /**
     * Test add multiple device.
     */
    @Test
    public void testAddMultipleDevice() {
        // Init store
        initMeterStore();
        // Submit meter 1
        manager.submit(m1Request.add());
        // Submit meter 2
        manager.submit(m2Request.add());
        // Verify add
        assertTrue("The meter was not added", manager.getAllMeters().size() == 2);
        assertTrue("The meter was not added", manager.getMeters(did1).size() == 1);
        assertTrue("The meter was not added", manager.getMeters(did2).size() == 1);
        // Get Meters
        Meter installingMeter1 = manager.getMeter(did1, mid1);
        Meter installingMeter2 = manager.getMeter(did2, mid1);
        // Verify add of installingMeter
        assertThat(installingMeter1, is(m1));
        assertThat(installingMeter2, is(m2));
        // Verify pending add state
        assertThat(installingMeter1.state(), is(MeterState.PENDING_ADD));
        assertThat(installingMeter2.state(), is(MeterState.PENDING_ADD));
        // Let's simulate a working data-plane
        pushMetrics(MeterOperation.Type.ADD, installingMeter1);
        pushMetrics(MeterOperation.Type.ADD, installingMeter2);
        // Get meter
        Meter installedMeter1 = manager.getMeter(did1, mid1);
        Meter installedMeter2 = manager.getMeter(did2, mid1);
        // Verify installation
        assertThat(installedMeter1.state(), is(MeterState.ADDED));
        assertThat(installedMeter2.state(), is(MeterState.ADDED));
        assertTrue("The meter was not installed", manager.getAllMeters().size() == 2);
        assertTrue("The meter was not installed", manager.getMeters(did1).size() == 1);
        assertTrue("The meter was not installed", manager.getMeters(did2).size() == 1);
    }

    /**
     * Test remove meter.
     */
    @Test
    public void testRemoveMultipleDevice() {
        // Init store
        initMeterStore();
        // Submit meter 1
        manager.submit(m1Request.add());
        // Submit meter 2
        manager.submit(m2Request.add());
        // Withdraw meter
        manager.withdraw(m1Request.remove(), m1.id());
        // Withdraw meter
        manager.withdraw(m2Request.remove(), m2.id());
        // Get Meters
        Meter withdrawingMeter1 = manager.getMeter(did1, mid1);
        Meter withdrawingMeter2 = manager.getMeter(did2, mid1);
        // Verify withdrawing
        assertThat(withdrawingMeter1.state(), is(MeterState.PENDING_REMOVE));
        assertThat(withdrawingMeter2.state(), is(MeterState.PENDING_REMOVE));
        assertTrue("The meter was not withdrawn", manager.getAllMeters().size() == 2);
        assertTrue("The meter was not withdrawn", manager.getMeters(did1).size() == 1);
        assertTrue("The meter was not withdrawn", manager.getMeters(did2).size() == 1);
        // Let's simulate a working data-plane
        pushMetrics(MeterOperation.Type.REMOVE, withdrawingMeter1);
        pushMetrics(MeterOperation.Type.REMOVE, withdrawingMeter2);
        // Verify withdrawn
        assertNull(manager.getMeter(did1, mid1));
        assertNull(manager.getMeter(did2, mid1));
        assertTrue("The meter was not removed", manager.getAllMeters().size() == 0);
        assertTrue("The meter was not removed", manager.getMeters(did1).size() == 0);
        assertTrue("The meter was not removed", manager.getMeters(did2).size() == 0);
    }

    @Test
    public void testAddFromMeterProgrammable()  {

        // Init store
        initMeterStore();

        manager.submit(mProgrammableRequest.add());

        TestTools.assertAfter(500, () -> {

            assertTrue("The meter was not added", manager.getAllMeters().size() == 1);

            assertThat(manager.getMeter(PROGRAMMABLE_DID, MeterId.meterId(1)), is(mProgrammable));
        });

    }

    @Test
    public void testAddBatchFromMeterProgrammable()  {

        // Init store
        initMeterStore();

        List<MeterOperation> operations = ImmutableList.of(new MeterOperation(mProgrammable, MeterOperation.Type.ADD));
        manager.defaultProvider().performMeterOperation(PROGRAMMABLE_DID, new MeterOperations(operations));

        TestTools.assertAfter(500, () -> {

            assertTrue("The meter was not added", meterOperations.size() == 1);

            assertTrue("Wrong Meter Operation", meterOperations.get(0).meter().id().equals(mProgrammable.id()));
        });

    }

    @Test
    public void testGetFromMeterProgrammable()  {

        // Init store
        initMeterStore();

        MeterDriverProvider fallback = (MeterDriverProvider) manager.defaultProvider();

        testAddFromMeterProgrammable();

        fallback.init(manager.deviceService, fallback.meterProviderService, manager.mastershipService, 1);

        TestTools.assertAfter(2000, () -> {
            assertTrue("The meter was not added", manager.getAllMeters().size() == 1);
            Meter m = manager.getMeters(PROGRAMMABLE_DID).iterator().next();
            assertEquals("incorrect state", MeterState.ADDED, m.state());
        });

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

    private static class TestDeviceService extends DeviceServiceAdapter {
        @Override
        public int getDeviceCount() {
            return 1;
        }

        @Override
        public Iterable<Device> getDevices() {
            return ImmutableList.of(PROGRAMMABLE_DEV);
        }

        @Override
        public Iterable<Device> getAvailableDevices() {
            return getDevices();
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return PROGRAMMABLE_DEV;
        }
    }

    private class TestDriverManager extends DriverManager {
        TestDriverManager(DriverRegistry registry, DeviceService deviceService,
                          NetworkConfigService networkConfigService) {
            this.registry = registry;
            this.deviceService = deviceService;
            this.networkConfigService = networkConfigService;
            activate();
        }
    }

    public static class TestMeterQuery extends AbstractHandlerBehaviour
            implements MeterQuery {
        private static final long MAX_METER = 0x00000FFF;

        @Override
        public long getMaxMeters() {
            return MAX_METER;
        }
    }

    private static List<MeterOperation> meterOperations = new ArrayList<>();

    public static class TestMeterProgrammable extends AbstractHandlerBehaviour
            implements MeterProgrammable {

        @Override
        public CompletableFuture<Boolean> performMeterOperation(MeterOperation meterOp) {
            return CompletableFuture.completedFuture(meterOperations.add(meterOp));
        }

        @Override
        public CompletableFuture<Collection<Meter>> getMeters() {
            //ADD METER
            DefaultMeter mProgrammableAdded = (DefaultMeter) mProgrammable;
            mProgrammableAdded.setState(MeterState.ADDED);
            return CompletableFuture.completedFuture(ImmutableList.of(mProgrammableAdded));
        }
    }

    private class TestProvider extends AbstractProvider implements MeterProvider {

        protected TestProvider(ProviderId id) {
            super(PID);
        }

        @Override
        public void performMeterOperation(DeviceId deviceId, MeterOperations meterOps) {
            //Currently unused.
        }

        @Override
        public void performMeterOperation(DeviceId deviceId, MeterOperation meterOp) {
            //Currently unused
        }
    }

    // Test mastership service
    private final class TestMastershipService extends MastershipServiceAdapter {
        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return NID_LOCAL;
        }

        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return MastershipRole.MASTER;
        }
    }

}
