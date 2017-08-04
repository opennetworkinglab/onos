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
package org.onosproject.bgpio.types.attr;

import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;

import com.google.common.testing.EqualsTester;

/**
 * Test for BGP prefix metric attribute.
 */
public class BgpPrefixAttrOspfFwdAddrTest {

    private final short lsAttrLength = 4;
    private final Ip4Address ip4RouterId = Ip4Address.valueOf("192.168.1.1");
    private final Ip6Address ip6RouterId = Ip6Address
            .valueOf("2001:0db8:0a0b:12f0:0000:0000:0000:0001");

    private final short lsAttrLength1 = 16;
    private final Ip4Address ip4RouterId1 = Ip4Address.valueOf("192.168.1.2");
    private final Ip6Address ip6RouterId1 = Ip6Address
            .valueOf("1002:0db8:0a0b:12f0:0000:0000:0000:0002");

    private final BgpPrefixAttrOspfFwdAddr data = BgpPrefixAttrOspfFwdAddr
            .of(lsAttrLength, ip4RouterId, ip6RouterId);
    private final BgpPrefixAttrOspfFwdAddr sameAsData = BgpPrefixAttrOspfFwdAddr
            .of(lsAttrLength, ip4RouterId, ip6RouterId);
    private final BgpPrefixAttrOspfFwdAddr diffData = BgpPrefixAttrOspfFwdAddr
            .of(lsAttrLength1, ip4RouterId1, ip6RouterId1);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(data, sameAsData)
        .addEqualityGroup(diffData).testEquals();
    }
}
