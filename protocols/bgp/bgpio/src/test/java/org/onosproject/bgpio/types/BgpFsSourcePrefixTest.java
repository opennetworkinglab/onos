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
package org.onosproject.bgpio.types;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import com.google.common.testing.EqualsTester;

/**
 * Test for source prefix flow specification component.
 */
public class BgpFsSourcePrefixTest {
    private final byte length = 4;

    private final IpPrefix prefix = IpPrefix.valueOf(IpAddress.valueOf("10.0.1.1"), 32);

    private final byte length2 = 4;
    private final IpPrefix prefix2 = IpPrefix.valueOf(IpAddress.valueOf("10.0.1.2"), 32);

    private final BgpFsSourcePrefix tlv1 = new BgpFsSourcePrefix(length,  prefix);
    private final BgpFsSourcePrefix sameAsTlv1 = new BgpFsSourcePrefix(length, prefix);
    private final BgpFsSourcePrefix tlv2 = new BgpFsSourcePrefix(length2,  prefix2);

    @Test
    public void testEquality() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
