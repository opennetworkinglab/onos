/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.LinkedList;

/**
 * Test case for TE Node Attribute tlv.
 */
public class TENodeAttributesTlvTest {

    private final NodeFlagBitsTlv nodeFlagBitsTlv1 = new NodeFlagBitsTlv((byte) 10);
    private final IPv4TERouterIdOfLocalNodeTlv iPv4TERouterIdOfLocalNodeTlv1 = new
            IPv4TERouterIdOfLocalNodeTlv(0x01010101);

    private final NodeFlagBitsTlv nodeFlagBitsTlv2 = new NodeFlagBitsTlv((byte) 20);
    private final IPv4TERouterIdOfLocalNodeTlv iPv4TERouterIdOfLocalNodeTlv2 = new
            IPv4TERouterIdOfLocalNodeTlv(0x02020202);

    private final LinkedList<PcepValueType> llNodeAttributesSubTLV1 = new LinkedList<>();
    private final boolean a = llNodeAttributesSubTLV1.add(nodeFlagBitsTlv1);
    private final boolean b = llNodeAttributesSubTLV1.add(iPv4TERouterIdOfLocalNodeTlv1);

    private final LinkedList<PcepValueType> llNodeAttributesSubTLV2 = new LinkedList<>();

    private final boolean c = llNodeAttributesSubTLV2.add(nodeFlagBitsTlv2);
    private final boolean d = llNodeAttributesSubTLV2.add(iPv4TERouterIdOfLocalNodeTlv2);

    private final TENodeAttributesTlv tlv1 = TENodeAttributesTlv.of(llNodeAttributesSubTLV1);
    private final TENodeAttributesTlv sameAsTlv1 = TENodeAttributesTlv.of(llNodeAttributesSubTLV1);
    private final TENodeAttributesTlv tlv2 = TENodeAttributesTlv.of(llNodeAttributesSubTLV2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, sameAsTlv1).addEqualityGroup(tlv2).testEquals();
    }

}
