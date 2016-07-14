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

import java.util.ArrayList;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Test for BGP unreserved bandwidth attribute.
 */
public class BgpLinkAttrUnRsrvdLinkBandwidthTest {
    ArrayList<Float> maxUnResBandwidth = new ArrayList<Float>();
    ArrayList<Float> maxUnResBandwidth1 = new ArrayList<Float>();
    short sType = 10;

    private final BgpLinkAttrUnRsrvdLinkBandwidth isisData = BgpLinkAttrUnRsrvdLinkBandwidth
            .of(maxUnResBandwidth, sType);
    private final BgpLinkAttrUnRsrvdLinkBandwidth sameAsIsisData = BgpLinkAttrUnRsrvdLinkBandwidth
            .of(maxUnResBandwidth, sType);
    private final BgpLinkAttrUnRsrvdLinkBandwidth isisDiff = BgpLinkAttrUnRsrvdLinkBandwidth
            .of(maxUnResBandwidth1, sType);

    @Test
    public void basics() {

        maxUnResBandwidth.add(new Float(1));
        maxUnResBandwidth.add(new Float(2));
        maxUnResBandwidth.add(new Float(3));
        maxUnResBandwidth.add(new Float(4));

        maxUnResBandwidth1.add(new Float(1));
        maxUnResBandwidth1.add(new Float(2));
        maxUnResBandwidth1.add(new Float(3));
        maxUnResBandwidth1.add(new Float(1));

        new EqualsTester().addEqualityGroup(isisData, sameAsIsisData)
        .addEqualityGroup(isisDiff).testEquals();

    }
}