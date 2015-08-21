/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.pcepio;

import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.onosproject.pcepio.types.IPv6TERouterIdofRemoteNodeTlv;

/**
 * Test of the IPv6TERouterIdofRemoteNodeTlv.
 */
public class IPv6TERouterIdofRemoteNodeTlvTest {

    private byte[] b1 = new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
    private byte[] b2 = new byte[] {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 };

    private final IPv6TERouterIdofRemoteNodeTlv tlv1 = IPv6TERouterIdofRemoteNodeTlv.of(b1);
    private final IPv6TERouterIdofRemoteNodeTlv sameAsTlv1 = IPv6TERouterIdofRemoteNodeTlv.of(b1);
    private final IPv6TERouterIdofRemoteNodeTlv tlv2 = IPv6TERouterIdofRemoteNodeTlv.of(b2);

    @Test
    public void basics() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
