/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.store.key.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.store.service.TestStorageService;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Test class for DistributedDeviceKeyStore.
 */
public class DistributedDeviceKeyStoreTest {
    private DistributedDeviceKeyStore deviceKeyStore;

    final String deviceKeyIdValue = "DeviceKeyId";
    final String deviceKeyLabel = "DeviceKeyLabel";
    final String deviceKeyLabel2 = "DeviceKeyLabel2";
    final String deviceKeySnmpName = "DeviceKeySnmpName";

    /**
     * Sets up the device key store and the storage service test harness.
     */
    @Before
    public void setUp() {
        deviceKeyStore = new DistributedDeviceKeyStore();
        deviceKeyStore.storageService = new TestStorageService();
        deviceKeyStore.setDelegate(event -> {
        });
        deviceKeyStore.activate();
    }

    /**
     * Tears down the device key store.
     */
    @After
    public void tearDown() {
        deviceKeyStore.deactivate();
    }

    /**
     * Tests adding, query and removal of a device key.
     */
    @Test(expected = NullPointerException.class)
    public void testAddNullKey() {
        deviceKeyStore.createOrUpdateDeviceKey(null);
    }

    /**
     * Tests adding a device key to the store. This also tests the device key store
     * query methods.
     */
    @Test
    public void testAddKey() {
        DeviceKeyId deviceKeyId = DeviceKeyId.deviceKeyId(deviceKeyIdValue);

        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingCommunityName(deviceKeyId,
                                                                          deviceKeyLabel, deviceKeySnmpName);

        // Test to make sure that the device key store is empty
        Collection<DeviceKey> deviceKeys = deviceKeyStore.getDeviceKeys();
        assertTrue("The device key set should be empty.", deviceKeys.isEmpty());

        // Add the new device key to the store
        deviceKeyStore.createOrUpdateDeviceKey(deviceKey);

        // Test the getDeviceKeys method to make sure that the new device key exists
        deviceKeys = deviceKeyStore.getDeviceKeys();
        assertEquals("There should be one device key in the set.", deviceKeys.size(), 1);

        // Test the getDeviceKey method using the device key unique identifier
        deviceKey = deviceKeyStore.getDeviceKey(deviceKeyId);
        assertEquals("There should be one device key in the set.", deviceKeys.size(), 1);
    }

    /**
     * Tests re-adding the same device key to the store but with a different label.
     */
    @Test
    public void testAddSameKey() {
        DeviceKeyId deviceKeyId = DeviceKeyId.deviceKeyId(deviceKeyIdValue);

        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingCommunityName(deviceKeyId,
                                                                          deviceKeyLabel, deviceKeySnmpName);

        // Add the first device key to the store
        deviceKeyStore.createOrUpdateDeviceKey(deviceKey);

        // Test the getDeviceKeys method
        Collection<DeviceKey> deviceKeys = deviceKeyStore.getDeviceKeys();
        assertEquals("There should be one device key in the set.", deviceKeys.size(), 1);

        // Now let's create a new device key with the same device key identifier as exists in the store.
        DeviceKey deviceKey2 = DeviceKey.createDeviceKeyUsingCommunityName(deviceKeyId,
                                                                           deviceKeyLabel2, deviceKeySnmpName);

        // Replace the new device key in the store
        deviceKeyStore.createOrUpdateDeviceKey(deviceKey2);

        // Test the getDeviceKeys method to ensure that only 1 device key exists in the store.
        deviceKeys = deviceKeyStore.getDeviceKeys();
        assertEquals("There should be one device key in the set.", deviceKeys.size(), 1);

        // Test the getDeviceKey method using the device key unique identifier
        deviceKey = deviceKeyStore.getDeviceKey(deviceKeyId);
        assertNotNull("The device key should not be null.", deviceKey);
        assertEquals("The device key label should match.", deviceKeyLabel2, deviceKey.label());
    }

    /**
     * Tests removal of a device key from the store using the device key identifier.
     */
    @Test
    public void testRemoveKey() {
        DeviceKeyId deviceKeyId = DeviceKeyId.deviceKeyId(deviceKeyIdValue);
        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingCommunityName(deviceKeyId,
                                                                          deviceKeyLabel, deviceKeySnmpName);
        // Add the new device key to the store
        deviceKeyStore.createOrUpdateDeviceKey(deviceKey);

        // Remove the device key from the store
        deviceKeyStore.deleteDeviceKey(deviceKeyId);

        // Validate that the device key was removed from the store by querying it.
        deviceKey = deviceKeyStore.getDeviceKey(deviceKeyId);
        assertNull("The device key set should be empty.", deviceKey);
    }
}
