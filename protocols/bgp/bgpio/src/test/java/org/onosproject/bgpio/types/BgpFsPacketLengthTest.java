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
package org.onosproject.bgpio.types;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Test for packet length flow specification component.
 */
public class BgpFsPacketLengthTest {
    List<BgpFsOperatorValue> operatorValue1 = new ArrayList<>();
    List<BgpFsOperatorValue> operatorValue2 = new ArrayList<>();

    @Test
    public void testEquality() {
        operatorValue1.add(new BgpFsOperatorValue((byte) 1, new byte[100]));
        operatorValue1.add(new BgpFsOperatorValue((byte) 1, new byte[100]));
        operatorValue2.add(new BgpFsOperatorValue((byte) 2, new byte[100]));
        operatorValue2.add(new BgpFsOperatorValue((byte) 1, new byte[100]));

        BgpFsPacketLength tlv1 = new BgpFsPacketLength(operatorValue1);
        BgpFsPacketLength sameAsTlv1 = new BgpFsPacketLength(operatorValue1);
        BgpFsPacketLength tlv2 = new BgpFsPacketLength(operatorValue2);

        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
