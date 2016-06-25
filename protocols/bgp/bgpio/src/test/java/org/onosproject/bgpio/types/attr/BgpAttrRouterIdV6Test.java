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
package org.onosproject.bgpio.types.attr;

import org.junit.Test;
import org.onlab.packet.Ip6Address;

import com.google.common.testing.EqualsTester;

/**
 * Test for BGP attribute node router ID.
 */
public class BgpAttrRouterIdV6Test {

    private final short sType = 1;
    private final Ip6Address ip6RouterId = Ip6Address
            .valueOf("2001:0db8:0a0b:12f0:0000:0000:0000:0001");

    private final short sType1 = 2;
    private final Ip6Address ip6RouterId1 = Ip6Address
            .valueOf("2004:0db8:0a0b:12f0:0000:0000:0000:0004");

    private final BgpAttrRouterIdV6 data = BgpAttrRouterIdV6.of(ip6RouterId,
                                                                sType);
    private final BgpAttrRouterIdV6 sameAsData = BgpAttrRouterIdV6
            .of(ip6RouterId, sType);
    private final BgpAttrRouterIdV6 diffData = BgpAttrRouterIdV6
            .of(ip6RouterId1, sType1);

    @Test
    public void basics() {

        new EqualsTester().addEqualityGroup(data, sameAsData)
        .addEqualityGroup(diffData).testEquals();
    }
}
