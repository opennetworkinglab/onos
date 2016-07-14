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

import com.google.common.testing.EqualsTester;

/**
 * Test for BGP attribute node flag.
 */
public class BgpAttrNodeFlagBitTlvTest {

    private final boolean bOverloadBit = true;
    private final boolean bAttachedBit = true;
    private final boolean bExternalBit = true;
    private final boolean bABRBit = true;

    private final boolean bOverloadBit1 = false;
    private final boolean bAttachedBit1 = false;
    private final boolean bExternalBit1 = false;
    private final boolean bABRBit1 = false;

    private final BgpAttrNodeFlagBitTlv data = BgpAttrNodeFlagBitTlv
            .of(bOverloadBit, bAttachedBit, bExternalBit, bABRBit);
    private final BgpAttrNodeFlagBitTlv sameAsData = BgpAttrNodeFlagBitTlv
            .of(bOverloadBit, bAttachedBit, bExternalBit, bABRBit);
    private final BgpAttrNodeFlagBitTlv diffData = BgpAttrNodeFlagBitTlv
            .of(bOverloadBit1, bAttachedBit1, bExternalBit1, bABRBit1);

    @Test
    public void basics() {

        new EqualsTester().addEqualityGroup(data, sameAsData)
        .addEqualityGroup(diffData).testEquals();
    }
}
