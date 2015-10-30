/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.bgp;

import org.junit.Test;
import org.onosproject.bgpio.types.BGPLSIdentifierTlv;

import com.google.common.testing.EqualsTester;

/**
 * Test for BGPLSIdentifier Tlv.
 */
public class BGPLSIdentifierTest {
    private final int value1 = 8738;
    private final int value2 = 13107;
    private final BGPLSIdentifierTlv tlv1 = BGPLSIdentifierTlv.of(value1);
    private final BGPLSIdentifierTlv sameAsTlv1 = new BGPLSIdentifierTlv(value1);
    private final BGPLSIdentifierTlv tlv2 = new BGPLSIdentifierTlv(value2);

    @Test
    public void basics() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}