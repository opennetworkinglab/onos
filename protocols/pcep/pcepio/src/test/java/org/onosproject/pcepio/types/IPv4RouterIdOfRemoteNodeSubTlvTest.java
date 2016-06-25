/*
 * Copyright 2016-present Open Networking Laboratory
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

/**
 * Test of the IPv4RouterIdOfRemoteNodeSubTlv.
 */
public class IPv4RouterIdOfRemoteNodeSubTlvTest {

    private final IPv4RouterIdOfRemoteNodeSubTlv tlv1 = IPv4RouterIdOfRemoteNodeSubTlv.of(2);
    private final IPv4RouterIdOfRemoteNodeSubTlv sameAsTlv1 = IPv4RouterIdOfRemoteNodeSubTlv.of(2);
    private final IPv4RouterIdOfRemoteNodeSubTlv tlv2 = IPv4RouterIdOfRemoteNodeSubTlv.of(3);

    @Test
    public void basics() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
