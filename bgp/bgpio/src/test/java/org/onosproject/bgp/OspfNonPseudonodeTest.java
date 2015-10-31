/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.bgp;

import org.junit.Test;
import org.onosproject.bgpio.types.OSPFNonPseudonode;

import com.google.common.testing.EqualsTester;

/**
 * Test for OSPFNonPseudonode Tlv.
 */
public class OspfNonPseudonodeTest {
    private final int value1 = 0x12121212;
    private final int value2 = 0x12121211;
    private final OSPFNonPseudonode tlv1 = OSPFNonPseudonode.of(value1);
    private final OSPFNonPseudonode sameAsTlv1 = OSPFNonPseudonode.of(value1);
    private final OSPFNonPseudonode tlv2 = OSPFNonPseudonode.of(value2);

    @Test
    public void basics() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
