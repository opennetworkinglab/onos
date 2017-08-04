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
 * Test for IPv4Address Tlv.
 */
public class IPv4AddressTest {
    private final Ip4Address value1 = Ip4Address.valueOf("127.0.0.1");
    private final Ip4Address value2 = Ip4Address.valueOf("127.0.0.1");
    private final IPv4AddressTlv tlv1 = IPv4AddressTlv.of(value1, (short) 259);
    private final IPv4AddressTlv sameAsTlv1 = IPv4AddressTlv.of(value1, (short) 259);
    private final IPv4AddressTlv tlv2 = IPv4AddressTlv.of(value2, (short) 260);

    @Test
    public void basics() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
