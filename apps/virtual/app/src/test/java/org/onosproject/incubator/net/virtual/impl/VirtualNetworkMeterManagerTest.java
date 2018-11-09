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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.TestApplicationId;
import org.onosproject.cluster.NodeId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkMeterStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.event.VirtualListenerRegistryManager;
import org.onosproject.incubator.net.virtual.impl.provider.VirtualProviderManager;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualMeterProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualMeterProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.incubator.net.virtual.store.impl.DistributedVirtualNetworkStore;
import org.onosproject.incubator.net.virtual.store.impl.SimpleVirtualMeterStore;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.TestableIntentService;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.DefaultMeterFeatures;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterFeaturesKey;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterOperations;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TestStorageService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Virtual Network meter manager tests.
 */
public class VirtualNetworkMeterManagerTest extends VirtualNetworkTestUtil {
    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final NodeId NID_LOCAL = new NodeId("local");
    private static final IpAddress LOCALHOST = IpAddress.valueOf("127.0.0.1");

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private TestableIntentService intentService = new FakeIntentManager();
    private ServiceDirectory testDirectory;
    private VirtualProviderManager providerRegistryService;

    private EventDeliveryService eventDeliveryService;
    VirtualListenerRegistryManager listenerRegistryManager =
            VirtualListenerRegistryManager.getInstance();

    private VirtualNetwork vnet1;
    private VirtualNetwork vnet2;

    private SimpleVirtualMeterStore meterStore;

    private VirtualNetworkMeterManager meterManager1;
    private VirtualNetworkMeterManager meterManager2;

    private TestProvider provider = new TestProvider();
    private VirtualMeterProviderService providerService1;
    private VirtualMeterProviderService providerService2;

    private ApplicationId appId;

    private Meter m1;
    private Meter m2;
    private MeterRequest.Builder m1Request;
    private MeterRequest.Builder m2Request;

    private Map<MeterId, Meter> meters = Maps.newHashMap();

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();
        CoreService coreService = new TestCoreService();
        TestStorageService storageService = new TestStorageService();
        TestUtils.setField(virtualNetworkManagerStore, "coreService", coreService);
        TestUtils.setField(virtualNetworkManagerStore, "storageService", storageService);
        virtualNetworkManagerStore.activate();

        meterStore = new SimpleVirtualMeterStore();

        providerRegistryService = new VirtualProviderManager();
        providerRegistryService.registerProvider(provider);

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        TestUtils.setField(manager, "coreService", coreService);

        eventDeliveryService = new TestEventDispatcher();
        NetTestTools.injectEventDispatcher(manager, eventDeliveryService);
//        eventDeliveryService.addSink(VirtualEvent.class, listenerRegistryManager);

        appId = new TestApplicationId("MeterManagerTest");

        testDirectory = new TestServiceDirectory()
                .add(VirtualNetworkStore.class, virtualNetworkManagerStore)
                .add(CoreService.class, coreService)
                .add(VirtualProviderRegistryService.class, providerRegistryService)
                .add(EventDeliveryService.class, eventDeliveryService)
                .add(StorageService.class, storageService)
                .add(VirtualNetworkMeterStore.class, meterStore);
        TestUtils.setField(manager, "serviceDirectory", testDirectory);

        manager.activate();

        vnet1 = setupVirtualNetworkTopology(manager, TID1);
        vnet2 = setupVirtualNetworkTopology(manager, TID2);

        meterManager1 = new VirtualNetworkMeterManager(manager, vnet1.id());
        meterManager2 = new VirtualNetworkMeterManager(manager, vnet2.id());

        providerService1 = (VirtualMeterProviderService)
                providerRegistryService.getProviderService(vnet1.id(), VirtualMeterProvider.class);
        providerService2 = (VirtualMeterProviderService)
                providerRegistryService.getProviderService(vnet2.id(), VirtualMeterProvider.class);

        assertTrue("provider should be registered",
                   providerRegistryService.getProviders().contains(provider.id()));

        setupMeterTestVariables();
    }

    @After
    public void tearDown() {
        providerRegistryService.unregisterProvider(provider);
        assertFalse("provider should not be registered",
                    providerRegistryService.getProviders().contains(provider.id()));

        manager.deactivate();
        NetTestTools.injectEventDispatcher(manager, null);

        virtualNetworkManagerStore.deactivate();
    }

    /** Test for meter submit(). */
    @Test
    public void testAddition() {
        meterManager1.submit(m1Request.add());

        assertTrue("The meter was not added",
                   meterManager1.getAllMeters().size() == 1);
        assertThat(meterManager1.getMeter(VDID1, MeterId.meterId(1)), is(m1));

        assertTrue("The meter shouldn't be added for vnet2",
                   meterManager2.getAllMeters().size() == 0);
    }

    /** Test for meter remove(). */
    @Test
    public void testRemove() {
        meterManager1.submit(m1Request.add());
        meterManager1.withdraw(m1Request.remove(), m1.id());

        assertThat(meterManager1.getMeter(VDID1, MeterId.meterId(1)).state(),
                   is(MeterState.PENDING_REMOVE));

        providerService1.pushMeterMetrics(m1.deviceId(), Collections.emptyList());

        assertTrue("The meter was not removed", meterManager1.getAllMeters().size() == 0);
        assertTrue("The meter shouldn't be added for vnet2",
                   meterManager2.getAllMeters().size() == 0);
    }

    /** Test for meter submit with multiple devices. */
    @Test
    public void testMultipleDevice() {
        meterManager1.submit(m1Request.add());
        meterManager1.submit(m2Request.add());

        assertTrue("The meters were not added",
                   meterManager1.getAllMeters().size() == 2);
        assertTrue("The meter shouldn't be added for vnet2",
                   meterManager2.getAllMeters().size() == 0);

        assertThat(meterManager1.getMeter(VDID1, MeterId.meterId(1)), is(m1));
        assertThat(meterManager1.getMeter(VDID2, MeterId.meterId(1)), is(m2));
    }

    /** Test for meter features inside store. */
    @Test
    public void testMeterFeatures() {
        //Test for virtual network 1
        assertEquals(meterStore.getMaxMeters(vnet1.id(),
                                             MeterFeaturesKey.key(VDID1)), 255L);
        assertEquals(meterStore.getMaxMeters(vnet1.id(),
                                             MeterFeaturesKey.key(VDID2)), 2);
        //Test for virtual network 2
        assertEquals(meterStore.getMaxMeters(vnet2.id(),
                                             MeterFeaturesKey.key(VDID1)), 100);
        assertEquals(meterStore.getMaxMeters(vnet2.id(),
                                             MeterFeaturesKey.key(VDID2)), 10);
    }

    /** Set variables such as meters and request required for testing. */
    private void setupMeterTestVariables() {
        Band band = DefaultBand.builder()
                .ofType(Band.Type.DROP)
                .withRate(500)
                .build();

        m1 = DefaultMeter.builder()
                .forDevice(VDID1)
                .fromApp(appId)
                .withId(MeterId.meterId(1))
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band))
                .build();

        m2 = DefaultMeter.builder()
                .forDevice(VDID2)
                .fromApp(appId)
                .withId(MeterId.meterId(1))
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band))
                .build();

        m1Request = DefaultMeterRequest.builder()
                .forDevice(VDID1)
                .fromApp(appId)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band));

        m2Request = DefaultMeterRequest.builder()
                .forDevice(VDID2)
                .fromApp(appId)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band));

        meterStore.storeMeterFeatures(vnet1.id(),
                                      DefaultMeterFeatures.builder().forDevice(VDID1)
                                              .withMaxMeters(255L)
                                              .withBandTypes(new HashSet<>())
                                              .withUnits(new HashSet<>())
                                              .hasStats(false)
                                              .hasBurst(false)
                                              .withMaxBands((byte) 0)
                                              .withMaxColors((byte) 0)
                                              .build());
        meterStore.storeMeterFeatures(vnet1.id(),
                                      DefaultMeterFeatures.builder().forDevice(VDID2)
                                              .withMaxMeters(2)
                                              .withBandTypes(new HashSet<>())
                                              .withUnits(new HashSet<>())
                                              .hasBurst(false)
                                              .hasStats(false)
                                              .withMaxBands((byte) 0)
                                              .withMaxColors((byte) 0)
                                              .build());

        meterStore.storeMeterFeatures(vnet2.id(),
                                      DefaultMeterFeatures.builder().forDevice(VDID1)
                                              .withMaxMeters(100L)
                                              .withBandTypes(new HashSet<>())
                                              .withUnits(new HashSet<>())
                                              .hasStats(false)
                                              .hasBurst(false)
                                              .withMaxBands((byte) 0)
                                              .withMaxColors((byte) 0)
                                              .build());
        meterStore.storeMeterFeatures(vnet2.id(),
                                      DefaultMeterFeatures.builder().forDevice(VDID2)
                                              .withMaxMeters(10)
                                              .withBandTypes(new HashSet<>())
                                              .withUnits(new HashSet<>())
                                              .hasBurst(false)
                                              .hasStats(false)
                                              .withMaxBands((byte) 0)
                                              .withMaxColors((byte) 0)
                                              .build());
    }

    private class TestProvider
            extends AbstractVirtualProvider
            implements VirtualMeterProvider {

        protected TestProvider() {
            super(PID);
        }

        @Override
        public void performMeterOperation(NetworkId networkId, DeviceId deviceId,
                                          MeterOperations meterOps) {

        }

        @Override
        public void performMeterOperation(NetworkId networkId, DeviceId deviceId,
                                          MeterOperation meterOp) {
            meters.put(meterOp.meter().id(), meterOp.meter());
        }
    }
}