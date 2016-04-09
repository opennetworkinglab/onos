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
package org.onosproject.pcepio.types;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

public class PcepNaiIpv6AdjacencyTest {
    private final byte[] localIpv6Addr1 = {(byte) 0x01010101 };
    private final byte[] remoteIpv6Addr1 = {(byte) 0x02020202 };
    private final byte[] localIpv6Addr2 = {(byte) 0x01010101 };
    private final byte[] remoteIpv6Addr2 = {(byte) 0x02020202 };
    private final byte[] localIpv6Addr3 = {(byte) 0x05050505 };
    private final byte[] remoteIpv6Addr3 = {(byte) 0x06060606 };

    private final PcepNaiIpv6Adjacency tlv1 = PcepNaiIpv6Adjacency.of(localIpv6Addr1, remoteIpv6Addr1);
    private final PcepNaiIpv6Adjacency tlv2 = PcepNaiIpv6Adjacency.of(localIpv6Addr1, remoteIpv6Addr1);
    private final PcepNaiIpv6Adjacency tlv3 = PcepNaiIpv6Adjacency.of(localIpv6Addr3, remoteIpv6Addr3);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, tlv2).addEqualityGroup(tlv3).testEquals();
    }
}
