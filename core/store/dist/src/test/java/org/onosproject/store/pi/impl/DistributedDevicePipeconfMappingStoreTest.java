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
package org.onosproject.store.pi.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.store.service.TestStorageService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the Distributed Device to Pipeconf store.
 */
public class DistributedDevicePipeconfMappingStoreTest {

    private DistributedDevicePipeconfMappingStore store;
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("foo:bar");
    private static final PiPipeconfId PIPECONF_ID = new PiPipeconfId("foo-pipeconf");

    /**
     * Sets up the device key store and the storage service test harness.
     */
    @Before
    public void setUp() {
        store = new DistributedDevicePipeconfMappingStore();
        store.storageService = new TestStorageService();
        store.setDelegate(event -> {
        });
        store.activate();
    }

    /**
     * Test for activate.
     */
    @Test
    public void activate() {
        assertNotNull(store.storageService);
        assertTrue("Store must have delegate", store.hasDelegate());
        assertTrue("No value should be in the map", store.deviceToPipeconf.isEmpty());
        assertTrue("No value should be in the map", store.pipeconfToDevices.isEmpty());
    }

    /**
     * Test for deactivate.
     */
    @Test
    public void deactivate() {
        store.deactivate();
        assertNull("Should be null", store.deviceToPipeconf);
        assertNull("Should be null", store.pipeconfToDevices);
    }

    /**
     * Test for saving the binding in eventually consistent map and in reverse map.
     */
    @Test
    public void createOrUpdatePipeconfToDeviceBinding() {
        store.createOrUpdateBinding(DEVICE_ID, PIPECONF_ID);
        assertTrue("Value should be in the map",
                   store.deviceToPipeconf.containsKey(DEVICE_ID));
        assertEquals("Value should be in the map",
                     PIPECONF_ID, store.deviceToPipeconf.get(DEVICE_ID).value());
        assertTrue("Value should be in the map",
                   store.pipeconfToDevices.containsKey(PIPECONF_ID));
        assertTrue("Value should be in the map",
                   store.pipeconfToDevices.get(PIPECONF_ID).contains(DEVICE_ID));
    }

    /**
     * Test for getting the deviceId to pipeconfId binding.
     */
    @Test
    public void getPipeconfIdDevice() throws Exception {
        clear();
        createOrUpdatePipeconfToDeviceBinding();
        assertEquals("Wrong PipeconfId", store.getPipeconfId(DEVICE_ID), PIPECONF_ID);
    }

    /**
     * Test for getting the pipeconfId to devices binding.
     */
    @Test
    public void getDevices() {
        clear();
        createOrUpdatePipeconfToDeviceBinding();
        assertEquals("Wrong set of DeviceIds", store.getDevices(PIPECONF_ID), ImmutableSet.of(DEVICE_ID));

    }

    /**
     * Test for clearing binding for a given device.
     */
    @Test
    public void clearDeviceToPipeconfBinding() throws Exception {
        clear();
        createOrUpdatePipeconfToDeviceBinding();
        store.removeBinding(DEVICE_ID);
        assertFalse("Unexpected DeviceId in the map", store.deviceToPipeconf.containsKey(DEVICE_ID));
        assertTrue("No value should be in the map", store.pipeconfToDevices.get(PIPECONF_ID).isEmpty());
    }

    /**
     * Clears the store and revers map.
     */
    private void clear() {
        store.pipeconfToDevices.clear();
        store.deviceToPipeconf.clear();
    }

}
