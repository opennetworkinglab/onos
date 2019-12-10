/*
 * Copyright 2019-present Open Networking Foundation
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
 * limitations under the License.%
 */

package org.onosproject.pipelines.fabric.impl.behaviour.bng;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/**
 * Tests for FabricBngProgrammableService.
 */
public class FabricBngProgrammableServiceTest {

    private static final int SIZE = 10;
    private static final DeviceId DEVICE_ID_1 = DeviceId.deviceId("device:1");
    private static final DeviceId DEVICE_ID_2 = DeviceId.deviceId("device:2");

    private final FabricBngProgrammableService service = new FabricBngProgrammableService();

    @Before
    public void setUp() throws Exception {
        service.deviceService = new MockDeviceService();
        service.activate();
    }

    @After
    public void tearDown() throws Exception {
        service.deactivate();
        try {
            service.getLineIdAllocator(DEVICE_ID_1, SIZE);
            fail("Service methods should fail after deactivation");
        } catch (NullPointerException e) {
            // Expected.
        }
    }

    @Test
    public void getLineIdAllocatorTest() {
        var allocator1 = service.getLineIdAllocator(DEVICE_ID_1, SIZE);
        var sameAsAllocator1 = service.getLineIdAllocator(DEVICE_ID_1, SIZE);
        var allocator2 = service.getLineIdAllocator(DEVICE_ID_2, SIZE);

        assertEquals(allocator1.size(), SIZE);
        assertEquals(allocator1, sameAsAllocator1);
        assertNotEquals(allocator1, allocator2);

        try {
            service.getLineIdAllocator(DEVICE_ID_1, SIZE + 1);
            fail("Retrieving allocators with different size should fail");
        } catch (IllegalArgumentException e) {
            // Expected.
        }
    }
}