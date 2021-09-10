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
import org.easymock.EasyMock;
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
import org.onosproject.net.meter.MeterCellId;
import org.onosproject.net.meter.MeterFeatures;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterOperations;
import org.onosproject.net.meter.MeterProgrammable;
import org.onosproject.net.meter.MeterProvider;
import org.onosproject.net.meter.MeterProviderRegistry;
import org.onosproject.net.meter.MeterProviderService;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterScope;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.pi.PiPipeconfServiceAdapter;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.meter.impl.DistributedMeterStore;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TestStorageService;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.did;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;
import static org.onosproject.net.OsgiPropertyConstants.MM_USER_DEFINED_INDEX;

/**
 * Meter manager tests.
 */
public class MeterManagerTest {

    private static final NodeId NID_LOCAL = new NodeId("local");
    private static final IpAddress LOCALHOST = IpAddress.valueOf("127.0.0.1");

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final ProviderId PROGRAMMABLE_PROVIDER = new ProviderId("foo", "foo");
    private static final DeviceId PROGRAMMABLE_DID = DeviceId.deviceId("test:002");

    private static final DefaultAnnotations ANNOTATIONS =
            DefaultAnnotations.builder().set(AnnotationKeys.DRIVER, "foo").build();

    private static final Device PROGRAMMABLE_DEV =
            new DefaultDevice(PROGRAMMABLE_PROVIDER, PROGRAMMABLE_DID, Device.Type.SWITCH,
                    "", "", "", "", null, ANNOTATIONS);

    private TestProvider provider;
    private MeterManager manager;
    private MeterProviderRegistry registry;
    private MeterProviderService providerService;
    private DistributedMeterStore meterStore;

    private DeviceId did1 = did("1");
    private DeviceId did2 = did("2");

    private MeterId mid1 = MeterId.meterId(1);
    private MeterCellId cid0 = PiMeterCellId.ofIndirect(PiMeterId.of("foo"), 0L);

    private static Band b1 = DefaultBand.builder()
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
    private static Meter mProgrammable2 = DefaultMeter.builder()
            .forDevice(PROGRAMMABLE_DID)
            .fromApp(APP_ID)
            .withId(MeterId.meterId(2))
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1))
            .build();
    private static Meter mUserDefined = DefaultMeter.builder()
            .forDevice(PROGRAMMABLE_DID)
            .fromApp(APP_ID)
            .withCellId(PiMeterCellId.ofIndirect(PiMeterId.of("foo"), 0L))
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1))
            .build();

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
    private MeterRequest.Builder mProgrammableRequest2 = DefaultMeterRequest.builder()
            .forDevice(PROGRAMMABLE_DID)
            .fromApp(APP_ID)
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1));
    private MeterRequest.Builder userDefinedRequest = DefaultMeterRequest.builder()
            .forDevice(PROGRAMMABLE_DID)
            .fromApp(APP_ID)
            .withUnit(Meter.Unit.KB_PER_SEC)
            .withBands(Collections.singletonList(b1))
            .withScope(MeterScope.of("foo"))
            .withIndex(0L);

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
    private MeterFeatures programmableMef1 = DefaultMeterFeatures.builder().forDevice(PROGRAMMABLE_DID)
            .withStartIndex(1)
            .withEndIndex(10L)
            .withBandTypes(new HashSet<>())
            .withUnits(new HashSet<>())
            .hasStats(false)
            .hasBurst(false)
            .withMaxBands((byte) 0)
            .withMaxColors((byte) 0)
            .build();
    private MeterFeatures programmableMef2 = DefaultMeterFeatures.builder().forDevice(PROGRAMMABLE_DID)
            .withStartIndex(0)
            .withEndIndex(10L)
            .withScope(MeterScope.of("foo"))
            .withBandTypes(new HashSet<>())
            .withUnits(new HashSet<>())
            .hasStats(false)
            .hasBurst(false)
            .withMaxBands((byte) 0)
            .withMaxColors((byte) 0)
            .build();

    private ComponentContext componentContext = EasyMock.createMock(ComponentContext.class);

    @Before
    public void setup() {
        DeviceService deviceService = new TestDeviceService();
        DriverRegistryManager driverRegistry = new DriverRegistryManager();
        DriverManager driverService = new TestDriverManager(driverRegistry, deviceService,
                new NetworkConfigServiceAdapter());
        driverRegistry.addDriver(new DefaultDriver("foo", ImmutableList.of(), "",
                "", "",
                ImmutableMap.of(MeterProgrammable.class,
                        TestMeterProgrammable.class, MeterQuery.class, TestMeterQuery.class),
                ImmutableMap.of()));
        meterStore = new DistributedMeterStore();
        TestUtils.setField(meterStore, "storageService", new TestStorageService());
        TestUtils.setField(meterStore, "driverService", driverService);
        KryoNamespace.Builder testKryoBuilder = TestUtils.getField(meterStore, "APP_KRYO_BUILDER");
        testKryoBuilder.register(TestApplicationId.class);
        Serializer testSerializer = Serializer.using(Lists.newArrayList(testKryoBuilder.build()));
        TestUtils.setField(meterStore, "serializer", testSerializer);

        meterStore.activate();

        manager = new MeterManager();
        TestUtils.setField(manager, "store", meterStore);
        injectEventDispatcher(manager, new TestEventDispatcher());
        manager.deviceService = deviceService;
        manager.mastershipService = new TestMastershipService();
        manager.cfgService = new ComponentConfigAdapter();
        manager.clusterService = new TestClusterService();
        registry = manager;
        manager.driverService = driverService;

        Dictionary<String, Object> cfgDict = new Hashtable<>();
        expect(componentContext.getProperties()).andReturn(cfgDict);
        replay(componentContext);
        manager.activate(componentContext);

        provider = new TestProvider(PID);
        providerService = registry.register(provider);
        assertTrue("provider should be registered",
                   registry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        registry.unregister(provider);
        assertFalse("provider should not be registered",
                    registry.getProviders().contains(provider.id()));
        manager.deactivate();
        injectEventDispatcher(manager, null);
        meterStore.deactivate();
    }

    // Store meter features for all the devices
    private void initMeterStore() {
        meterStore.storeMeterFeatures(mef1);
        meterStore.storeMeterFeatures(mef2);
        meterStore.storeMeterFeatures(programmableMef1);
        meterStore.storeMeterFeatures(programmableMef2);
    }

    // Emulate metrics coming from the dataplane
    private void pushMetrics(MeterOperation.Type type, Meter meter) {
        if (type == MeterOperation.Type.ADD) {
            ((DefaultMeter) meter).setState(MeterState.ADDED);
            providerService.pushMeterMetrics(meter.deviceId(), Collections.singletonList(meter));
        } else {
            providerService.pushMeterMetrics(meter.deviceId(), Collections.emptyList());
        }
    }

    /**
     * Verify enabling user defined index mode in meter service.
     */
    @Test
    public void testEnableUserDefinedIndex() {
        reset(componentContext);
        Dictionary<String, Object> cfgDict = new Hashtable<>();
        cfgDict.put(MM_USER_DEFINED_INDEX, true);
        expect(componentContext.getProperties()).andReturn(cfgDict);
        replay(componentContext);

        Object returnValue = TestUtils.callMethod(manager, "readComponentConfiguration",
                ComponentContext.class, componentContext);
        assertNull(returnValue);
        assertTrue(manager.userDefinedIndex);
    }

    /**
     * Verify disabling user defined index mode in meter service.
     */
    @Test
    public void testDisableUserDefinedIndex() {
        testEnableUserDefinedIndex();

        reset(componentContext);
        Dictionary<String, Object> cfgDict = new Hashtable<>();
        cfgDict.put(MM_USER_DEFINED_INDEX, false);
        expect(componentContext.getProperties()).andReturn(cfgDict);
        replay(componentContext);

        Object returnValue = TestUtils.callMethod(manager, "readComponentConfiguration",
                ComponentContext.class, componentContext);
        assertNull(returnValue);
        assertFalse(manager.userDefinedIndex);
    }

    /**
     * Test add meter.
     */
    @Test
    public void testAdd() {
        initMeterStore();
        manager.submit(m1Request.add());

        assertEquals("The meter was not added", 1, manager.getAllMeters().size());
        assertEquals("The meter was not added", 1, manager.getMeters(did1).size());
        Meter installingMeter = manager.getMeter(did1, mid1);
        assertThat(installingMeter, is(m1));
        assertThat(installingMeter.state(), is(MeterState.PENDING_ADD));

        pushMetrics(MeterOperation.Type.ADD, installingMeter);

        Meter installedMeter = manager.getMeter(did1, mid1);
        assertThat(installedMeter.state(), is(MeterState.ADDED));
        assertEquals("The meter was not installed", 1, manager.getAllMeters().size());
        assertEquals("The meter was not installed", 1, manager.getMeters(did1).size());
    }

    /**
     * Test add meter with user defined index.
     */
    @Test
    public void testAddWithUserDefinedIndex() {
        initMeterStore();
        testEnableUserDefinedIndex();

        manager.submit(userDefinedRequest.add());
        assertEquals("The meter was not added", 1, manager.getAllMeters().size());
        assertEquals("The meter was not added", 1, manager.getMeters(PROGRAMMABLE_DID).size());
        Meter installingMeter = manager.getMeter(PROGRAMMABLE_DID, cid0);
        assertThat(installingMeter, is(mUserDefined));
        assertThat(installingMeter.state(), is(MeterState.PENDING_ADD));

        pushMetrics(MeterOperation.Type.ADD, installingMeter);
        Meter installedMeter = manager.getMeter(PROGRAMMABLE_DID, cid0);
        assertThat(installedMeter.state(), is(MeterState.ADDED));
        assertEquals("The meter was not installed", 1, manager.getAllMeters().size());
        assertEquals("The meter was not installed", 1, manager.getMeters(PROGRAMMABLE_DID).size());
    }

    /**
     * Test wrong add meter.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWrongAdd() {
        initMeterStore();

        manager.submit(userDefinedRequest.add());
    }

    /**
     * Test wrong add meter in user defined index mode.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWrongAddInUserDefinedIndexMode() {
        initMeterStore();
        testEnableUserDefinedIndex();

        manager.submit(m1Request.add());
    }

    /**
     * Test remove meter.
     */
    @Test
    public void testRemove() {
        initMeterStore();
        manager.submit(m1Request.add());
        manager.withdraw(m1Request.remove(), m1.id());

        Meter withdrawingMeter = manager.getMeter(did1, mid1);
        assertThat(withdrawingMeter.state(), is(MeterState.PENDING_REMOVE));
        assertEquals("The meter was not withdrawn", 1, manager.getAllMeters().size());
        assertEquals("The meter was not withdrawn", 1, manager.getMeters(did1).size());

        pushMetrics(MeterOperation.Type.REMOVE, withdrawingMeter);
        assertNull(manager.getMeter(did1, mid1));
        assertEquals("The meter was not removed", 0, manager.getAllMeters().size());
        assertEquals("The meter was not removed", 0, manager.getMeters(did1).size());
    }

    /**
     * Test remove meter in user defined index mode.
     */
    @Test
    public void testRemoveInUserDefinedIndexMode() {
        initMeterStore();
        testEnableUserDefinedIndex();

        manager.submit(userDefinedRequest.add());

        manager.withdraw(userDefinedRequest.remove(), cid0);
        Meter withdrawingMeter = manager.getMeter(PROGRAMMABLE_DID, cid0);
        assertThat(withdrawingMeter.state(), is(MeterState.PENDING_REMOVE));
        assertEquals("The meter was not withdrawn", 1, manager.getAllMeters().size());
        assertEquals("The meter was not withdrawn", 1, manager.getMeters(PROGRAMMABLE_DID).size());

        pushMetrics(MeterOperation.Type.REMOVE, withdrawingMeter);
        assertNull(manager.getMeter(PROGRAMMABLE_DID, cid0));
        assertEquals("The meter was not removed", 0, manager.getAllMeters().size());
        assertEquals("The meter was not removed", 0, manager.getMeters(PROGRAMMABLE_DID).size());
    }

    /**
     * Test add multiple devices.
     */
    @Test
    public void testAddMultipleDevice() {
        initMeterStore();
        manager.submit(m1Request.add());
        manager.submit(m2Request.add());

        assertEquals("The meter was not added", 2, manager.getAllMeters().size());
        assertEquals("The meter was not added", 1, manager.getMeters(did1).size());
        assertEquals("The meter was not added", 1, manager.getMeters(did2).size());
        Meter installingMeter1 = manager.getMeter(did1, mid1);
        Meter installingMeter2 = manager.getMeter(did2, mid1);
        assertThat(installingMeter1, is(m1));
        assertThat(installingMeter2, is(m2));
        assertThat(installingMeter1.state(), is(MeterState.PENDING_ADD));
        assertThat(installingMeter2.state(), is(MeterState.PENDING_ADD));

        pushMetrics(MeterOperation.Type.ADD, installingMeter1);
        pushMetrics(MeterOperation.Type.ADD, installingMeter2);
        Meter installedMeter1 = manager.getMeter(did1, mid1);
        Meter installedMeter2 = manager.getMeter(did2, mid1);
        assertThat(installedMeter1.state(), is(MeterState.ADDED));
        assertThat(installedMeter2.state(), is(MeterState.ADDED));
        assertEquals("The meter was not installed", 2, manager.getAllMeters().size());
        assertEquals("The meter was not installed", 1, manager.getMeters(did1).size());
        assertEquals("The meter was not installed", 1, manager.getMeters(did2).size());
    }

    /**
     * Test remove multiple devices.
     */
    @Test
    public void testRemoveMultipleDevice() {
        initMeterStore();
        manager.submit(m1Request.add());
        manager.submit(m2Request.add());
        manager.withdraw(m1Request.remove(), m1.id());
        manager.withdraw(m2Request.remove(), m2.id());

        Meter withdrawingMeter1 = manager.getMeter(did1, mid1);
        Meter withdrawingMeter2 = manager.getMeter(did2, mid1);
        assertThat(withdrawingMeter1.state(), is(MeterState.PENDING_REMOVE));
        assertThat(withdrawingMeter2.state(), is(MeterState.PENDING_REMOVE));
        assertEquals("The meter was not withdrawn", 2, manager.getAllMeters().size());
        assertEquals("The meter was not withdrawn", 1, manager.getMeters(did1).size());
        assertEquals("The meter was not withdrawn", 1, manager.getMeters(did2).size());

        pushMetrics(MeterOperation.Type.REMOVE, withdrawingMeter1);
        pushMetrics(MeterOperation.Type.REMOVE, withdrawingMeter2);
        assertNull(manager.getMeter(did1, mid1));
        assertNull(manager.getMeter(did2, mid1));
        assertEquals("The meter was not removed", 0, manager.getAllMeters().size());
        assertEquals("The meter was not removed", 0, manager.getMeters(did1).size());
        assertEquals("The meter was not removed", 0, manager.getMeters(did2).size());
    }

    /**
     * Test purge meter.
     */
    @Test
    public void testPurge() {
        initMeterStore();
        manager.submit(m1Request.add());

        Meter submittingMeter = manager.getMeter(did1, mid1);
        assertThat(submittingMeter.state(), is(MeterState.PENDING_ADD));
        assertEquals("The meter was not added", 1, manager.getAllMeters().size());
        assertEquals("The meter was not added", 1, manager.getMeters(did1).size());

        manager.purgeMeters(did1);
        assertNull(manager.getMeter(did1, mid1));
        assertEquals("The meter was not purged", 0, manager.getAllMeters().size());
        assertEquals("The meter was not purged", 0, manager.getMeters(did1).size());
    }

    /**
     * Test submit for programmable devices.
     */
    @Test
    public void testAddFromMeterProgrammable()  {
        initMeterStore();
        manager.submit(mProgrammableRequest.add());

        TestTools.assertAfter(500, () -> {
            assertEquals("The meter was not added", 1, manager.getAllMeters().size());
            assertThat(manager.getMeter(PROGRAMMABLE_DID, MeterId.meterId(1)), is(mProgrammable));
        });
    }

    /**
     * Test batch submission for meter programmable.
     */
    @Test
    public void testAddBatchFromMeterProgrammable()  {
        initMeterStore();
        List<MeterOperation> operations = ImmutableList.of(new MeterOperation(mProgrammable, MeterOperation.Type.ADD));
        manager.defaultProvider().performMeterOperation(PROGRAMMABLE_DID, new MeterOperations(operations));

        TestTools.assertAfter(500, () -> {
            assertEquals("The meter was not added", 1, meterOperations.size());
            assertEquals("Wrong Meter Operation", meterOperations.get(0).meter().id(), mProgrammable.id());
        });

    }

    /**
     * Verify get from meter programmable.
     */
    @Test
    public void testGetFromMeterProgrammable()  {
        initMeterStore();
        MeterDriverProvider fallback = (MeterDriverProvider) manager.defaultProvider();
        testAddFromMeterProgrammable();
        fallback.init(manager.deviceService, fallback.meterProviderService, manager.mastershipService, 1);

        TestTools.assertAfter(2000, () -> {
            assertEquals("The meter was not added", 1, manager.getAllMeters().size());
            Meter m = manager.getMeters(PROGRAMMABLE_DID).iterator().next();
            assertEquals("incorrect state", MeterState.ADDED, m.state());
        });
    }

    /**
     * Verify installation of missing meters when using meter programmable devices.
     */
    @Test
    public void testMissingFromMeterProgrammable() {
        // Workaround when running the tests all together
        meterOperations.clear();
        testGetFromMeterProgrammable();

        assertThat(meterOperations.size(), is(1));
        manager.submit(mProgrammableRequest2.add());
        TestTools.assertAfter(500, () -> {
            assertEquals("The meter was not added", 2, manager.getAllMeters().size());
            assertThat(manager.getMeter(PROGRAMMABLE_DID, MeterId.meterId(2)), is(mProgrammable2));
            assertThat(meterOperations.size(), is(2));
            assertThat(meterOperations.get(meterOperations.size() - 1), is(new MeterOperation(mProgrammable2,
                    MeterOperation.Type.ADD)));
        });

        TestTools.assertAfter(2000, () -> {
            assertEquals("The meter was not added", 2, manager.getAllMeters().size());
            Meter pendingMeter = manager.getMeter(PROGRAMMABLE_DID, MeterId.meterId(2));
            assertThat(pendingMeter, is(mProgrammable2));
            assertEquals("incorrect state", MeterState.PENDING_ADD, pendingMeter.state());
            assertThat(meterOperations.size(), is(3));
            assertThat(meterOperations.get(meterOperations.size() - 1), is(new MeterOperation(mProgrammable2,
                    MeterOperation.Type.ADD)));
        });
    }

    /**
     * Verify removal of unknown meters when using meter programmable devices.
     */
    @Test
    public void testUnknownFromMeterProgrammable() {
        meterOperations.clear();
        testGetFromMeterProgrammable();
        TestMeterProgrammable.unknownMeter = true;

        assertThat(meterOperations.size(), is(1));
        TestTools.assertAfter(2000, () -> {
            assertEquals("The meter was not added", 1, manager.getAllMeters().size());
            Meter pendingMeter = manager.getMeter(PROGRAMMABLE_DID, MeterId.meterId(2));
            assertNull(pendingMeter);
            assertThat(meterOperations.size(), is(2));
            assertThat(meterOperations.get(meterOperations.size() - 1), is(new MeterOperation(mProgrammable2,
                    MeterOperation.Type.REMOVE)));
        });
    }

    /**
     * Verify removal of meters when using meter programmable devices.
     */
    @Test
    public void testRemoveFromMeterProgrammable()  {
        testEnableUserDefinedIndex();
        initMeterStore();
        MeterDriverProvider fallback = (MeterDriverProvider) manager.defaultProvider();
        fallback.init(manager.deviceService, fallback.meterProviderService, manager.mastershipService, 1);

        manager.submit(mProgrammableRequest2.withIndex(2L).add());
        TestTools.assertAfter(500, () -> {
            assertEquals("The meter was not added", 1, manager.getAllMeters().size());
            Meter pendingMeter = manager.getMeter(PROGRAMMABLE_DID, MeterId.meterId(2));
            assertThat(pendingMeter, is(mProgrammable2));
            assertEquals("incorrect state", MeterState.PENDING_ADD, pendingMeter.state());
        });

        manager.withdraw(mProgrammableRequest2.remove(), MeterId.meterId(2));
        TestTools.assertAfter(500, () -> {
            assertEquals("The meter was not withdrawn", 1, manager.getAllMeters().size());
            Meter pendingMeter = manager.getMeter(PROGRAMMABLE_DID, MeterId.meterId(2));
            assertThat(pendingMeter, is(mProgrammable2));
            assertEquals("incorrect state", MeterState.PENDING_REMOVE, pendingMeter.state());
        });

        TestTools.assertAfter(2000, () -> {
            assertEquals("The meter was not removed", 0, manager.getAllMeters().size());
            Meter pendingMeter = manager.getMeter(PROGRAMMABLE_DID, MeterId.meterId(2));
            assertNull(pendingMeter);
        });
    }

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
            this.pipeconfService = new PiPipeconfServiceAdapter();
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

        private static boolean unknownMeter;

        @Override
        public CompletableFuture<Boolean> performMeterOperation(MeterOperation meterOp) {
            return CompletableFuture.completedFuture(meterOperations.add(meterOp));
        }

        @Override
        public CompletableFuture<Collection<Meter>> getMeters() {
            // ADD METER
            Collection<Meter> meters = Lists.newArrayList();
            DefaultMeter mProgrammableAdded = (DefaultMeter) mProgrammable;
            mProgrammableAdded.setState(MeterState.ADDED);
            meters.add(mProgrammableAdded);
            if (unknownMeter) {
                DefaultMeter mProgrammable2Added = (DefaultMeter) mProgrammable2;
                mProgrammable2Added.setState(MeterState.ADDED);
                meters.add(mProgrammable2Added);
            }
            return CompletableFuture.completedFuture(meters);
        }

        @Override
        public CompletableFuture<Collection<MeterFeatures>> getMeterFeatures() {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }
    }

    private class TestProvider extends AbstractProvider implements MeterProvider {

        protected TestProvider(ProviderId id) {
            super(PID);
        }

        @Override
        public void performMeterOperation(DeviceId deviceId, MeterOperations meterOps) {

        }

        @Override
        public void performMeterOperation(DeviceId deviceId, MeterOperation meterOp) {

        }
    }

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
