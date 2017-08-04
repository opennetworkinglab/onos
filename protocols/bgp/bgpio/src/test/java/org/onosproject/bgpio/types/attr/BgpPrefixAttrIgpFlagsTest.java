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

import com.google.common.testing.EqualsTester;

/**
 * Test for BGP prefix IGP Flag attribute.
 */
public class BgpPrefixAttrIgpFlagsTest {

    private final boolean bisisUpDownBit = true;
    private final boolean bOspfNoUnicastBit = true;
    private final boolean bOspfLclAddrBit = true;
    private final boolean bOspfNSSABit = true;

    private final boolean bisisUpDownBit1 = false;
    private final boolean bOspfNoUnicastBit1 = false;
    private final boolean bOspfLclAddrBit1 = false;
    private final boolean bOspfNSSABit1 = false;

    private final BgpPrefixAttrIgpFlags data = BgpPrefixAttrIgpFlags
            .of(bisisUpDownBit, bOspfNoUnicastBit, bOspfLclAddrBit,
                bOspfNSSABit);
    private final BgpPrefixAttrIgpFlags sameAsData = BgpPrefixAttrIgpFlags
            .of(bisisUpDownBit, bOspfNoUnicastBit, bOspfLclAddrBit,
                bOspfNSSABit);
    private final BgpPrefixAttrIgpFlags diffData = BgpPrefixAttrIgpFlags
            .of(bisisUpDownBit1, bOspfNoUnicastBit1, bOspfLclAddrBit1,
                bOspfNSSABit1);

    @Test
    public void basics() {

        new EqualsTester().addEqualityGroup(data, sameAsData)
        .addEqualityGroup(diffData).testEquals();
    }
}
