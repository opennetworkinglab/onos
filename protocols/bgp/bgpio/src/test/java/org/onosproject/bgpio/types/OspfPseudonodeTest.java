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
import org.onlab.packet.Ip4Address;

import com.google.common.testing.EqualsTester;

/**
 * Test for OSPFPseudonode Tlv.
 */
public class OspfPseudonodeTest {
    private final int value1 = 0xc3223409;
    private final int value2 = 0xc3223406;
    private final Ip4Address drInterface1 = Ip4Address.valueOf(0xaf91e01);
    private final Ip4Address drInterface2 = Ip4Address.valueOf(0xaf91e02);
    private final OspfPseudonode tlv1 = OspfPseudonode.of(value1, drInterface1);
    private final OspfPseudonode sameAsTlv1 = OspfPseudonode.of(value1, drInterface1);
    private final OspfPseudonode tlv2 = OspfPseudonode.of(value2, drInterface2);

    @Test
    public void testEquality() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
