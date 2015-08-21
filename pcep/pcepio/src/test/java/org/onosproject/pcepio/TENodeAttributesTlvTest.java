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
package org.onosproject.pcepio;

import java.util.LinkedList;

import org.junit.Test;
import org.onosproject.pcepio.types.IPv4TERouterIdOfLocalNodeTlv;
import org.onosproject.pcepio.types.NodeFlagBitsTlv;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.TENodeAttributesTlv;

import com.google.common.testing.EqualsTester;

/**
 * Test case for TE Node Attribute tlv.
 */
public class TENodeAttributesTlvTest {

    NodeFlagBitsTlv nodeFlagBitsTlv1 = new NodeFlagBitsTlv((byte) 10);
    IPv4TERouterIdOfLocalNodeTlv iPv4TERouterIdOfLocalNodeTlv1 = new IPv4TERouterIdOfLocalNodeTlv((int) 0x01010101);;
    LinkedList<PcepValueType> llNodeAttributesSubTLVs1 = new LinkedList<PcepValueType>();
    LinkedList<PcepValueType> llNodeAttributesSubTLVs2 = new LinkedList<PcepValueType>();
    LinkedList<PcepValueType> llNodeAttributesSubTLVs3 = new LinkedList<PcepValueType>();

    boolean b = llNodeAttributesSubTLVs1.add(nodeFlagBitsTlv1);

    boolean c = llNodeAttributesSubTLVs2.add(nodeFlagBitsTlv1);
    boolean d = llNodeAttributesSubTLVs3.add(iPv4TERouterIdOfLocalNodeTlv1);

    final TENodeAttributesTlv tlv1 = TENodeAttributesTlv.of(llNodeAttributesSubTLVs1);
    final TENodeAttributesTlv tlv2 = TENodeAttributesTlv.of(llNodeAttributesSubTLVs2);
    final TENodeAttributesTlv tlv3 = TENodeAttributesTlv.of(llNodeAttributesSubTLVs3);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, tlv2).addEqualityGroup(tlv3).testEquals();
    }

}
