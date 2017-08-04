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

import java.util.ArrayList;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Test for BGP prefix route tag attribute.
 */
public class BgpPrefixAttrRouteTagTest {
    ArrayList<Integer> maxUnResBandwidth = new ArrayList<Integer>();
    ArrayList<Integer> maxUnResBandwidth1 = new ArrayList<Integer>();

    private final BgpPrefixAttrRouteTag isisData = BgpPrefixAttrRouteTag
            .of(maxUnResBandwidth);
    private final BgpPrefixAttrRouteTag sameAsIsisData = BgpPrefixAttrRouteTag
            .of(maxUnResBandwidth);
    private final BgpPrefixAttrRouteTag isisDiff = BgpPrefixAttrRouteTag
            .of(maxUnResBandwidth1);

    @Test
    public void basics() {

        maxUnResBandwidth.add(new Integer(1));
        maxUnResBandwidth.add(new Integer(2));
        maxUnResBandwidth.add(new Integer(3));
        maxUnResBandwidth.add(new Integer(4));

        maxUnResBandwidth1.add(new Integer(1));
        maxUnResBandwidth1.add(new Integer(2));
        maxUnResBandwidth1.add(new Integer(3));
        maxUnResBandwidth1.add(new Integer(1));

        new EqualsTester().addEqualityGroup(isisData, sameAsIsisData)
                .addEqualityGroup(isisDiff).testEquals();
    }
}