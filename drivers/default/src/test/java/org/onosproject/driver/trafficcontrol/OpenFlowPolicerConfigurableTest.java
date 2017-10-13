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

package org.onosproject.driver.trafficcontrol;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.trafficcontrol.PolicerConfigurable;
import org.onosproject.net.behaviour.trafficcontrol.PolicerId;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterServiceAdapter;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.NetTestTools.did;

/**
 * OpenFlow Policer config test.
 */
public class OpenFlowPolicerConfigurableTest {

    // Device id used during the tests
    private DeviceId ofDid = did("1");
    private DeviceId fooDid = DeviceId.deviceId(FOO_SCHEME + ":1");

    // Schemes used during the tests
    private static final String OF_SCHEME = "of";
    private static final String FOO_SCHEME = "foo";

    // Policer id used during the tests
    private PolicerId fooPid = PolicerId.policerId(FOO_SCHEME + ":1");

    // Meter ids used during the tests
    private MeterId mid1 = MeterId.meterId(1);
    private MeterId mid10 = MeterId.meterId(10);
    private MeterId mid100 = MeterId.meterId(100);

    // Test Driver service used during the tests
    private DriverService driverService = new TestDriverService();

    // Test Meter service used during the tests
    private TestMeterService meterService = new TestMeterService();

    /**
     * Test allocate policer id.
     */
    @Test
    public void testAllocateId() {
        // Get device handler
        DriverHandler driverHandler = driverService.createHandler(ofDid);
        // Get policer config behavior
        PolicerConfigurable policerConfigurable = driverHandler.behaviour(PolicerConfigurable.class);
        // Get policer id
        PolicerId policerId = policerConfigurable.allocatePolicerId();
        // Assert that scheme is equal to OF
        assertThat(policerId.uri().getScheme(), is(OF_SCHEME));
        // Convert in hex the id
        String hexId = Long.toHexString((mid1.id()));
        // Assert that specific part contains hex of meter id
        assertThat(policerId.uri().getSchemeSpecificPart(), is(hexId));
    }

    /**
     * Test no corresponding device.
     */
    @Test
    public void testWrongDevice() {
        // Get device handler
        DriverHandler driverHandler = driverService.createHandler(fooDid);
        // Get policer config behavior
        PolicerConfigurable policerConfigurable = driverHandler.behaviour(PolicerConfigurable.class);
        // Get policer id
        PolicerId policerId = policerConfigurable.allocatePolicerId();
        // Assert that is none
        assertThat(policerId, is(PolicerId.NONE));
    }

    /**
     * Test meter problems.
     */
    @Test
    public void testMeterNull() {
        // Get device handler
        DriverHandler driverHandler = driverService.createHandler(ofDid);
        // Get policer config behavior
        PolicerConfigurable policerConfigurable = driverHandler.behaviour(PolicerConfigurable.class);
        // Get policer id
        PolicerId policerId = policerConfigurable.allocatePolicerId();
        // this works
        assertThat(policerId.uri().getScheme(), is(OF_SCHEME));
        String hexId = Long.toHexString((mid1.id()));
        assertThat(policerId.uri().getSchemeSpecificPart(), is(hexId));
        // Get another policer id
        policerId = policerConfigurable.allocatePolicerId();
        assertThat(policerId.uri().getScheme(), is(OF_SCHEME));
        hexId = Long.toHexString((mid10.id()));
        assertThat(policerId.uri().getSchemeSpecificPart(), is(hexId));
        // Get the last policer id
        policerId = policerConfigurable.allocatePolicerId();
        assertThat(policerId.uri().getScheme(), is(OF_SCHEME));
        hexId = Long.toHexString((mid100.id()));
        assertThat(policerId.uri().getSchemeSpecificPart(), is(hexId));
        // this does not work
        policerId = policerConfigurable.allocatePolicerId();
        // Assert that is none
        assertThat(policerId, is(PolicerId.NONE));
    }

    /**
     * Test free policer id.
     */
    @Test
    public void testFreeId() {
        // Get device handler
        DriverHandler driverHandler = driverService.createHandler(ofDid);
        // Get policer config behavior
        PolicerConfigurable policerConfigurable = driverHandler.behaviour(PolicerConfigurable.class);
        // Get policer id
        PolicerId policerId = policerConfigurable.allocatePolicerId();
        // this works
        assertThat(policerId.uri().getScheme(), is(OF_SCHEME));
        String hexId = Long.toHexString((mid1.id()));
        assertThat(policerId.uri().getSchemeSpecificPart(), is(hexId));
        // Verify the allocation before free
        assertThat(meterService.availableIds.size(), is(2));
        // Let's free the policer id
        policerConfigurable.freePolicerId(policerId);
        // Verify the availability after free
        assertThat(meterService.availableIds.size(), is(3));
    }

    /**
     * Test wrong policer id.
     */
    @Test
    public void testWrongId() {
        // Get device handler
        DriverHandler driverHandler = driverService.createHandler(ofDid);
        // Get policer config behavior
        PolicerConfigurable policerConfigurable = driverHandler.behaviour(PolicerConfigurable.class);
        // Get policer id
        PolicerId policerId = policerConfigurable.allocatePolicerId();
        // this works
        assertThat(policerId.uri().getScheme(), is(OF_SCHEME));
        String hexId = Long.toHexString((mid1.id()));
        assertThat(policerId.uri().getSchemeSpecificPart(), is(hexId));
        // Verify the allocation before free
        assertThat(meterService.availableIds.size(), is(2));
        // Update the pid with a wrong id (same id but wrong schema)
        policerId = fooPid;
        // Let's free the policer id
        policerConfigurable.freePolicerId(policerId);
        // Free does not end correctly
        assertThat(meterService.availableIds.size(), is(2));
    }

    // Test class for driver handler
    private class TestDriverHandler implements DriverHandler {

        private final DeviceId deviceId;

        TestDriverHandler(DeviceId deviceId) {
            this.deviceId = deviceId;
        }

        @Override
        public Driver driver() {
            return null;
        }

        @Override
        public DriverData data() {
            // Just create a fake driver data
            return new DefaultDriverData(null, deviceId);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Behaviour> T behaviour(Class<T> behaviourClass) {
            // Let's create the behavior
            PolicerConfigurable policerConfigurable = new OpenFlowPolicerConfigurable();
            // Set the handler
            policerConfigurable.setHandler(this);
            // Done, return the behavior
            return (T) policerConfigurable;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Class<T> serviceClass) {
            return (T) meterService;
        }

        @Override
        public boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
            return true;
        }
    }

    // Test class for driver service
    private class TestDriverService extends DriverServiceAdapter {

        @Override
        public DriverHandler createHandler(DeviceId deviceId, String... credentials) {
            return new TestDriverHandler(deviceId);
        }

    }

    // Test class for meter service
    private class TestMeterService extends MeterServiceAdapter {

        // Let's simulate a store
        Set<MeterId> availableIds = new TreeSet<>(
                (Comparator<MeterId>) (id1, id2) -> id1.id().compareTo(id2.id())
        );

        TestMeterService() {
            availableIds.addAll(ImmutableList.of(mid1, mid10, mid100));
        }

        @Override
        public MeterId allocateMeterId(DeviceId deviceId) {
            // If there are no more ids, return null
            if (availableIds.isEmpty()) {
                return null;
            }
            // Get the next id
            MeterId meterId = availableIds.iterator().next();
            // Make it unavailable
            availableIds.remove(meterId);
            // Done, return it
            return meterId;
        }

        @Override
        public void freeMeterId(DeviceId deviceId, MeterId meterId) {
            // Make the id available
            availableIds.add(meterId);
        }

    }

}
