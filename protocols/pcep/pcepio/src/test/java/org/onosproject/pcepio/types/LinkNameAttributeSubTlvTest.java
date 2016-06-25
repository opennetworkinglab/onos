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
 * Equality test for LinkNameAttributeSubTlv.
 */
public class LinkNameAttributeSubTlvTest {
    private final byte[] rawValue1 = new byte[] {0x01, 0x00};
    private final byte[] rawValue2 = new byte[] {0x02, 0x00};

    private final LinkNameAttributeSubTlv tlv1 = new LinkNameAttributeSubTlv(rawValue1, (short) rawValue1.length);
    private final LinkNameAttributeSubTlv sameAsTlv1 = LinkNameAttributeSubTlv.of(tlv1.getValue(), tlv1.getLength());
    private final LinkNameAttributeSubTlv tlv2 = new LinkNameAttributeSubTlv(rawValue2, (short) 0);

    @Test
    public void basics() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
