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
 * Test for Protection Type attribute.
 */
public class BgpLinkAttrProtectionTypeTest {
    private final byte linkProtectionType1 = 0x04;
    private final byte linkProtectionType2 = 0x40;

    private final BgpLinkAttrProtectionType attr1 = BgpLinkAttrProtectionType.of(linkProtectionType1);
    private final BgpLinkAttrProtectionType sameAsAttr1 = BgpLinkAttrProtectionType.of(linkProtectionType1);
    private final BgpLinkAttrProtectionType attr2 = BgpLinkAttrProtectionType.of(linkProtectionType2);

    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(attr1, sameAsAttr1)
        .addEqualityGroup(attr2)
        .testEquals();
    }
}
