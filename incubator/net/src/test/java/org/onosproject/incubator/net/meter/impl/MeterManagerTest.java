/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.net.meter.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.incubator.store.meter.impl.DistributedMeterStore;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.DeviceId;
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
import org.onosproject.net.meter.MeterProvider;
import org.onosproject.net.meter.MeterProviderRegistry;
import org.onosproject.net.meter.MeterProviderService;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.TestStorageService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.did;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Meter manager tests.
 */
public class MeterManagerTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final NodeId NID_LOCAL = new NodeId("local");
    private static final IpAddress LOCALHOST = IpAddress.valueOf("127.0.0.1");

    private MeterService service;
    private MeterManager manager;
    private DistributedMeterStore meterStore;
    private MeterProviderRegistry registry;
    private MeterProviderService providerService;

    private TestProvider provider;

    private ApplicationId appId;


    private Meter m1;
    private Meter m2;
    private MeterRequest.Builder m1Request;
    private MeterRequest.Builder m2Request;
    private MeterRequest.Builder m3Request;

    private Map<MeterId, Meter> meters = Maps.newHashMap();

    @Before
    public void setup() throws Exception {
        meterStore = new DistributedMeterStore();
        TestUtils.setField(meterStore, "storageService", new TestStorageService());
        TestUtils.setField(meterStore, "clusterService", new TestClusterService());
        TestUtils.setField(meterStore, "mastershipService", new TestMastershipService());
        meterStore.activate();
        meterStore.storeMeterFeatures(DefaultMeterFeatures.builder().forDevice(did("1"))
                .withMaxMeters(255L)
                .withBandTypes(new HashSet<>())
                .withUnits(new HashSet<>())
                .hasStats(false)
                .hasBurst(false)
                .withMaxBands((byte) 0)
                .withMaxColors((byte) 0)
                .build());
        meterStore.storeMeterFeatures(DefaultMeterFeatures.builder().forDevice(did("2"))
                .withMaxMeters(2)
                .withBandTypes(new HashSet<>())
                .withUnits(new HashSet<>())
                .hasBurst(false)
                .hasStats(false)
                .withMaxBands((byte) 0)
                .withMaxColors((byte) 0)
                .build());

        manager = new MeterManager();
        manager.store = meterStore;
        TestUtils.setField(manager, "storageService", new TestStorageService());
        injectEventDispatcher(manager, new TestEventDispatcher());
        service = manager;
        registry = manager;

        manager.activate();

        provider = new TestProvider(PID);
        providerService = registry.register(provider);

        appId = new TestApplicationId(0, "MeterManagerTest");

        assertTrue("provider should be registered",
                   registry.getProviders().contains(provider.id()));

        Band band = DefaultBand.builder()
                .ofType(Band.Type.DROP)
                .withRate(500)
                .build();

        m1 = DefaultMeter.builder()
                .forDevice(did("1"))
                .fromApp(APP_ID)
                .withId(MeterId.meterId(1))
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band))
                .build();

        m2 = DefaultMeter.builder()
                .forDevice(did("2"))
                .fromApp(APP_ID)
                .withId(MeterId.meterId(1))
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band))
                .build();

        m1Request = DefaultMeterRequest.builder()
                .forDevice(did("1"))
                .fromApp(APP_ID)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band));

        m2Request = DefaultMeterRequest.builder()
                .forDevice(did("2"))
                .fromApp(APP_ID)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band));

        m3Request = DefaultMeterRequest.builder()
                .forDevice(did("1"))
                .fromApp(APP_ID)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band));

    }

    @After
    public void tearDown() {
        registry.unregister(provider);
        assertFalse("provider should not be registered",
                    registry.getProviders().contains(provider.id()));

        manager.deactivate();
        injectEventDispatcher(manager, null);

    }

    @Test
    public void testAddition() {
        manager.submit(m1Request.add());

        assertTrue("The meter was not added", manager.getAllMeters().size() == 1);

        assertThat(manager.getMeter(did("1"), MeterId.meterId(1)), is(m1));
    }

    @Test
    public void testRemove() {
        manager.submit(m1Request.add());
        manager.withdraw(m1Request.remove(), m1.id());

        assertThat(manager.getMeter(did("1"), MeterId.meterId(1)).state(),
                   is(MeterState.PENDING_REMOVE));

        providerService.pushMeterMetrics(m1.deviceId(), Collections.emptyList());

        assertTrue("The meter was not removed", manager.getAllMeters().size() == 0);

    }

    @Test
    public void testMultipleDevice() {
        manager.submit(m1Request.add());
        manager.submit(m2Request.add());

        assertTrue("The meters were not added", manager.getAllMeters().size() == 2);

        assertThat(manager.getMeter(did("1"), MeterId.meterId(1)), is(m1));
        assertThat(manager.getMeter(did("2"), MeterId.meterId(1)), is(m2));
    }

    @Test
    public void testMeterFeatures() {
        assertEquals(meterStore.getMaxMeters(MeterFeaturesKey.key(did("1"))), 255L);
        assertEquals(meterStore.getMaxMeters(MeterFeaturesKey.key(did("2"))), 2);
    }

    @Test
    public void testMeterReuse() {
        manager.submit(m1Request.add());
        manager.submit(m3Request.add());
        Collection<Meter> meters = manager.getMeters(did("1"));
        Meter m = meters.iterator().next();
        MeterRequest mr = DefaultMeterRequest.builder()
                .forDevice(m.deviceId())
                .fromApp(m.appId())
                .withBands(m.bands())
                .withUnit(m.unit())
                .remove();
        manager.withdraw(mr, m.id());
        mr = DefaultMeterRequest.builder()
                .forDevice(m.deviceId())
                .fromApp(m.appId())
                .withBands(m.bands())
                .withUnit(m.unit())
                .add();
        Meter meter = manager.submit(mr);
        assertTrue("Meter id not reused", m.id().equals(meter.id()));

    }



    public class TestApplicationId extends DefaultApplicationId {
        public TestApplicationId(int id, String name) {
            super(id, name);
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
            meters.put(meterOp.meter().id(), meterOp.meter());
        }
    }

    private final class TestClusterService extends ClusterServiceAdapter {

        ControllerNode local = new DefaultControllerNode(NID_LOCAL, LOCALHOST);

        @Override
        public ControllerNode getLocalNode() {
            return local;
        }

        @Override
        public Set<ControllerNode> getNodes() {
            return Sets.newHashSet();
        }

    }

    private class TestMastershipService extends MastershipServiceAdapter {
        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return NID_LOCAL;
        }
    }
}
