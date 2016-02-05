/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.bgpio.protocol.flowspec;

import org.junit.Test;

import org.onlab.packet.IpPrefix;

import com.google.common.testing.EqualsTester;

/**
 * Test for BgpFsDestinationPrefix flow specification component.
 */
public class BgpFlowSpecPrefixTest {
    private IpPrefix destinationPrefix1 = IpPrefix.valueOf("21.21.21.21/16");
    private IpPrefix sourcePrefix1 = IpPrefix.valueOf("11.11.11.11/16");
    private IpPrefix destinationPrefix2 = IpPrefix.valueOf("42.42.42.42/16");
    private IpPrefix sourcePrefix2 = IpPrefix.valueOf("32.32.32.32/16");

    private final BgpFlowSpecPrefix tlv1 = new BgpFlowSpecPrefix(destinationPrefix1, sourcePrefix1);
    private final BgpFlowSpecPrefix sameAsTlv1 = new BgpFlowSpecPrefix(destinationPrefix1, sourcePrefix1);
    private final BgpFlowSpecPrefix tlv2 = new BgpFlowSpecPrefix(destinationPrefix2, sourcePrefix2);

    @Test
    public void testEquality() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
