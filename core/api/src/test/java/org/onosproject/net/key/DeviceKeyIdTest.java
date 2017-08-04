/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.key;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test class for DeviceDeyId.
 */
public class DeviceKeyIdTest {

    final String deviceKeyIdValue1 = "DeviceKeyId1";
    final String deviceKeyIdValue2 = "DeviceKeyId2";

    /**
     * Checks that the DeviceKeyId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DeviceKeyId.class);
    }

    /**
     * Checks the construction of a DeviceKeyId object throws an
     * IllegalArgumentException when the input identifier is null.
     */
    @Test(expected = NullPointerException.class)
    public void testConstructionUsingNullId() {
        DeviceKeyId.deviceKeyId(null);
    }

    /**
     * Checks the construction of a DeviceKeyId object.
     */
    @Test
    public void testConstruction() {
        DeviceKeyId deviceKeyId = DeviceKeyId.deviceKeyId(deviceKeyIdValue1);
        assertNotNull("The deviceKeyId should not be null.", deviceKeyId);
        assertEquals("The id should match the expected value.",
                     deviceKeyIdValue1, deviceKeyId.id());
    }

    /**
     * Tests the equality of device key identifiers.
     */
    @Test
    public void testEquality() {
        DeviceKeyId deviceKeyId1 = DeviceKeyId.deviceKeyId(deviceKeyIdValue1);
        DeviceKeyId deviceKeyId2 = DeviceKeyId.deviceKeyId(deviceKeyIdValue1);
        DeviceKeyId deviceKeyId3 = DeviceKeyId.deviceKeyId(deviceKeyIdValue2);

        new EqualsTester().addEqualityGroup(deviceKeyId1, deviceKeyId2)
                .addEqualityGroup(deviceKeyId3)
                .testEquals();
    }
}
