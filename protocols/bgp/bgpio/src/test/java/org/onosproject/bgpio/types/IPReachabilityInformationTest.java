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
 * Test for IPReachabilityInformation Tlv.
 */
public class IPReachabilityInformationTest {
    private final byte[] value1 = new byte[] {(byte) 0xc0, (byte) 0xa8, 0x4d, 0x01};
    private final byte[] value2 = new byte[] {(byte) 0xc0};
    private final IPReachabilityInformationTlv tlv1 = IPReachabilityInformationTlv.of((byte) 0x17, value1, (short) 4);
    private final IPReachabilityInformationTlv sameAsTlv1 = IPReachabilityInformationTlv
                                                             .of((byte) 0x17, value1, (short) 4);
    private final IPReachabilityInformationTlv tlv2 = IPReachabilityInformationTlv.of((byte) 0x05, value2, (short) 1);

    @Test
    public void basics() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
