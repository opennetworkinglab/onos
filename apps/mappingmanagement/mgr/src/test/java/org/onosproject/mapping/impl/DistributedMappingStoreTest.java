/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.mapping.impl;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpPrefix;
import org.onosproject.mapping.DefaultMapping;
import org.onosproject.mapping.DefaultMappingEntry;
import org.onosproject.mapping.DefaultMappingKey;
import org.onosproject.mapping.DefaultMappingTreatment;
import org.onosproject.mapping.DefaultMappingValue;
import org.onosproject.mapping.Mapping;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.actions.MappingActions;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.onosproject.mapping.MappingStore.Type.MAP_DATABASE;

/**
 * Unit tests for DistributedMappingStore.
 */
public class DistributedMappingStoreTest {
    private static final DeviceId DEVICE_ID_1 = DeviceId.deviceId("foo");
    private static final DeviceId DEVICE_ID_2 = DeviceId.deviceId("bar");
    private static final String IP_ADDRESS = "1.2.3.4/24";

    private DistributedMappingStore mappingStore;
    private MappingEntry mapping1;
    private MappingEntry mapping2;

    private Device device1;
    private Device device2;

    /**
     * Sets up the storage service test harness.
     */
    @Before
    public void setUp() {
        mappingStore = new DistributedMappingStore();
        mappingStore.storageService = new TestStorageService();
        mappingStore.deviceService = new InternalDeviceServiceAdapter();
        mappingStore.setDelegate(event -> {
        });

        IpPrefix ipPrefix = IpPrefix.valueOf(IP_ADDRESS);
        MappingAddress address = MappingAddresses.ipv4MappingAddress(ipPrefix);

        MappingKey key = DefaultMappingKey.builder()
                .withAddress(address)
                .build();

        MappingAction action = MappingActions.noAction();
        MappingTreatment treatment = DefaultMappingTreatment.builder()
                .withAddress(address)
                .setUnicastPriority(10)
                .setUnicastWeight(10)
                .build();

        MappingValue value = DefaultMappingValue.builder()
                .withAction(action)
                .add(treatment)
                .build();

        device1 = new MockDevice(ProviderId.NONE, DEVICE_ID_1, Device.Type.OTHER,
                "foo.inc", "0", "0", "0", null,
                DefaultAnnotations.builder().build());

        device2 = new MockDevice(ProviderId.NONE, DEVICE_ID_2, Device.Type.OTHER,
                "foo.inc", "0", "0", "0", null,
                DefaultAnnotations.builder().build());

        Mapping originalMapping1 = DefaultMapping.builder()
                .forDevice(DEVICE_ID_1)
                .withId(1000L)
                .withKey(key)
                .withValue(value)
                .build();

        Mapping originalMapping2 = DefaultMapping.builder()
                .forDevice(DEVICE_ID_2)
                .withId(2000L)
                .withKey(key)
                .withValue(value)
                .build();

        mapping1 = new DefaultMappingEntry(originalMapping1);
        mapping2 = new DefaultMappingEntry(originalMapping2);

        mappingStore.activate();
    }

    private class InternalDeviceServiceAdapter extends DeviceServiceAdapter {

        List<Device> devices = Lists.newArrayList();

        @Override
        public Iterable<Device> getDevices() {
            devices.add(device1);
            devices.add(device2);
            return devices;
        }
    }

    private class MockDevice extends DefaultDevice {
        public MockDevice(ProviderId providerId, DeviceId id, Type type,
                          String manufacturer, String hwVersion, String swVersion,
                          String serialNumber, ChassisId chassisId, Annotations... annotations) {
            super(providerId, id, type, manufacturer, hwVersion, swVersion, serialNumber,
                    chassisId, annotations);
        }
    }

    /**
     * Tears down the mapping1 store.
     */
    @After
    public void tearDown() {
        mappingStore.deactivate();
    }

    /**
     * Tests adding, removing and getting.
     */
    @Test
    public void basics() {
        mappingStore.storeMapping(MAP_DATABASE, mapping1);
        mappingStore.storeMapping(MAP_DATABASE, mapping2);

        assertThat("There should be one mapping1 in the map database.",
                mappingStore.getMappingCount(MAP_DATABASE), is(2));
        assertTrue("There should be one mapping1 in the map database.",
                mapping1.equals(mappingStore.getMappingEntries(MAP_DATABASE, DEVICE_ID_1)
                        .iterator().next()));
        assertTrue("The mapping1 should be identical.",
                mappingStore.getMappingEntry(MAP_DATABASE, mapping1).equals(mapping1));
        mappingStore.removeMapping(MAP_DATABASE, mapping1);
        assertFalse("There should not be any mapping1 in the map database.",
                mappingStore.getMappingEntries(MAP_DATABASE, DEVICE_ID_1).iterator().hasNext());
    }
}
