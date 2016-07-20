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

package org.onosproject.net.key.impl;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestTools;
import org.onlab.junit.TestUtils;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.event.Event;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyEvent;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.key.DeviceKeyListener;
import org.onosproject.net.key.DeviceKeyService;
import org.onosproject.store.key.impl.DistributedDeviceKeyStore;
import org.onosproject.net.NetTestTools;
import org.onosproject.store.service.TestStorageService;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for DeviceKeyManager.
 */
public class DeviceKeyManagerTest {

    final String deviceKeyIdValue = "DeviceKeyId";
    final String deviceKeyLabel = "DeviceKeyLabel";
    final String deviceKeyLabel2 = "DeviceKeyLabel2";
    final String deviceKeySnmpName = "DeviceKeySnmpName";

    private DeviceKeyManager manager;
    private DeviceKeyService deviceKeyService;
    private DistributedDeviceKeyStore deviceKeyStore;
    protected TestListener listener = new TestListener();

    @Before
    public void setUp() throws Exception {
        deviceKeyStore = new DistributedDeviceKeyStore();
        TestUtils.setField(deviceKeyStore, "storageService", new TestStorageService());
        deviceKeyStore.activate();

        manager = new DeviceKeyManager();
        manager.store = deviceKeyStore;
        manager.addListener(listener);
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());
        manager.activate();
        deviceKeyService = manager;
    }

    @After
    public void tearDown() {
        deviceKeyStore.deactivate();
        manager.removeListener(listener);
        manager.deactivate();
        NetTestTools.injectEventDispatcher(manager, null);
    }

    /**
     * Tests adding, query and removal of a device key.
     */
    @Test(expected = NullPointerException.class)
    public void testAddNullKey() {
        manager.addKey(null);
    }

    /**
     * Tests adding a device key using the device key manager.
     * This also tests the device key manager query methods.
     */
    @Test
    public void testAddKey() {
        DeviceKeyId deviceKeyId = DeviceKeyId.deviceKeyId(deviceKeyIdValue);

        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingCommunityName(deviceKeyId,
                                                                          deviceKeyLabel, deviceKeySnmpName);

        // Test to make sure that the device key store is empty
        Collection<DeviceKey> deviceKeys = manager.getDeviceKeys();
        assertTrue("The device key set should be empty.", deviceKeys.isEmpty());

        // Add the new device key using the device key manager.
        manager.addKey(deviceKey);

        // Test the getDeviceKeys method to make sure that the new device key exists
        deviceKeys = manager.getDeviceKeys();
        assertEquals("There should be one device key in the set.", deviceKeys.size(), 1);

        // Test the getDeviceKey method using the device key unique identifier
        deviceKey = manager.getDeviceKey(deviceKeyId);
        assertEquals("There should be one device key in the set.", deviceKeys.size(), 1);

        // Validate that only the DEVICE_KEY_ADDED event was received.
        validateEvents(DeviceKeyEvent.Type.DEVICE_KEY_ADDED);

    }

    /**
     * Tests re-adding the same device key to the store but with a different label.
     */
    @Test
    public void testAddSameKey() {
        DeviceKeyId deviceKeyId = DeviceKeyId.deviceKeyId(deviceKeyIdValue);

        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingCommunityName(deviceKeyId,
                                                                          deviceKeyLabel, deviceKeySnmpName);

        // Add the first device key via the device key manager
        manager.addKey(deviceKey);

        // Test the getDeviceKeys method
        Collection<DeviceKey> deviceKeys = manager.getDeviceKeys();
        assertEquals("There should be one device key in the set.", deviceKeys.size(), 1);

        // Now let's create a new device key with the same device key identifier as exists in the store.
        DeviceKey deviceKey2 = DeviceKey.createDeviceKeyUsingCommunityName(deviceKeyId,
                                                                           deviceKeyLabel2, deviceKeySnmpName);

        // Replace the new device key in the store
        manager.addKey(deviceKey2);

        // Test the getDeviceKeys method to ensure that only 1 device key exists in the store.
        deviceKeys = manager.getDeviceKeys();
        assertEquals("There should be one device key in the set.", deviceKeys.size(), 1);

        // Test the getDeviceKey method using the device key unique identifier
        deviceKey = manager.getDeviceKey(deviceKeyId);
        assertNotNull("The device key should not be null.", deviceKey);
        assertEquals("The device key label should match.", deviceKeyLabel2, deviceKey.label());

        // Validate that the following events were received in order,
        // DEVICE_KEY_ADDED, DEVICE_KEY_REMOVED, DEVICE_KEY_ADDED.
        validateEvents(DeviceKeyEvent.Type.DEVICE_KEY_ADDED, DeviceKeyEvent.Type.DEVICE_KEY_UPDATED);

    }

    /**
     * Tests removal of a device key from the store using the device key identifier.
     */
    @Test
    public void testRemoveKey() {
        DeviceKeyId deviceKeyId = DeviceKeyId.deviceKeyId(deviceKeyIdValue);
        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingCommunityName(deviceKeyId,
                                                                          deviceKeyLabel, deviceKeySnmpName);
        // Add the new device key using the device key manager
        manager.addKey(deviceKey);

        // Remove the device key from the store
        manager.removeKey(deviceKeyId);

        // Validate that the device key was removed from the store by querying it.
        deviceKey = manager.getDeviceKey(deviceKeyId);
        assertNull("The device key set should be empty.", deviceKey);

        // Validate that the following events were received in order,
        // DEVICE_KEY_ADDED, DEVICE_KEY_REMOVED.
        validateEvents(DeviceKeyEvent.Type.DEVICE_KEY_ADDED, DeviceKeyEvent.Type.DEVICE_KEY_REMOVED);
    }

    /**
     * Method to validate that actual versus expected device key events were
     * received correctly.
     *
     * @param types expected device key events.
     */
    private void validateEvents(Enum... types) {
        TestTools.assertAfter(100, () -> {
            int i = 0;
            assertEquals("wrong events received", types.length, listener.events.size());
            for (Event event : listener.events) {
                assertEquals("incorrect event type", types[i], event.type());
                i++;
            }
            listener.events.clear();
        });
    }

    /**
     * Test listener class to receive device key events.
     */
    private static class TestListener implements DeviceKeyListener {

        protected List<DeviceKeyEvent> events = Lists.newArrayList();

        @Override
        public void event(DeviceKeyEvent event) {
            events.add(event);
        }

    }
}
