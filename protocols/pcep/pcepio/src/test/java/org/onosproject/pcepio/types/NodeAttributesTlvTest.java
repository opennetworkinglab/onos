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

import java.util.LinkedList;
import java.util.List;

/**
 * Test case for TE Node Attribute tlv.
 */
public class NodeAttributesTlvTest {

    private final NodeFlagBitsSubTlv nodeFlagBitsTlv1 = new NodeFlagBitsSubTlv((byte) 10);
    private final IPv4RouterIdOfLocalNodeSubTlv iPv4TERouterIdOfLocalNodeTlv1 = new
            IPv4RouterIdOfLocalNodeSubTlv(0x01010101);

    private final NodeFlagBitsSubTlv nodeFlagBitsTlv2 = new NodeFlagBitsSubTlv((byte) 20);
    private final IPv4RouterIdOfLocalNodeSubTlv iPv4TERouterIdOfLocalNodeTlv2 = new
            IPv4RouterIdOfLocalNodeSubTlv(0x02020202);

    private final List<PcepValueType> llNodeAttributesSubTLV1 = new LinkedList<>();
    private final boolean a = llNodeAttributesSubTLV1.add(nodeFlagBitsTlv1);
    private final boolean b = llNodeAttributesSubTLV1.add(iPv4TERouterIdOfLocalNodeTlv1);

    private final List<PcepValueType> llNodeAttributesSubTLV2 = new LinkedList<>();

    private final boolean c = llNodeAttributesSubTLV2.add(nodeFlagBitsTlv2);
    private final boolean d = llNodeAttributesSubTLV2.add(iPv4TERouterIdOfLocalNodeTlv2);

    private final NodeAttributesTlv tlv1 = NodeAttributesTlv.of(llNodeAttributesSubTLV1);
    private final NodeAttributesTlv sameAsTlv1 = NodeAttributesTlv.of(llNodeAttributesSubTLV1);
    private final NodeAttributesTlv tlv2 = NodeAttributesTlv.of(llNodeAttributesSubTLV2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, sameAsTlv1).addEqualityGroup(tlv2).testEquals();
    }

}
