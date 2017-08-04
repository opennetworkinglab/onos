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
package org.onosproject.bgpio.types;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Test for AutonomousSystem Tlv.
 */
public class AutonomousSystemTest {
    private final int value1 = 101;
    private final int value2 = 201;
    private final AutonomousSystemTlv tlv1 = AutonomousSystemTlv.of(value1);
    private final AutonomousSystemTlv sameAsTlv1 = AutonomousSystemTlv.of(value1);
    private final AutonomousSystemTlv tlv2 = AutonomousSystemTlv.of(value2);

    @Test
    public void testEquality() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
