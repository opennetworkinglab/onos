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
package org.onosproject.bgpio.types;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Test for IsIsNonPseudonode Tlv.
 */
public class IsIsNonPseudonodeTest {
    private final byte[] value1 = new byte[] {0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58};
    private final byte[] value2 = new byte[] {0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x59};
    private final IsIsNonPseudonode tlv1 = IsIsNonPseudonode.of(value1);
    private final IsIsNonPseudonode sameAsTlv1 = IsIsNonPseudonode.of(value1);
    private final IsIsNonPseudonode tlv2 = IsIsNonPseudonode.of(value2);

    @Test
    public void testEquality() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
