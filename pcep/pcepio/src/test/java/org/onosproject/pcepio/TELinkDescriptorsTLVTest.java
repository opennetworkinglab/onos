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
import org.onosproject.pcepio.types.IPv4InterfaceAddressTlv;
import org.onosproject.pcepio.types.LinkLocalRemoteIdentifiersTlv;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.TELinkDescriptorsTLV;

import com.google.common.testing.EqualsTester;

/**
 * Test case for TE link descriptors Tlv.
 */
public class TELinkDescriptorsTLVTest {
    LinkLocalRemoteIdentifiersTlv linkLocalRemoteIdentifiersTlv1 = new LinkLocalRemoteIdentifiersTlv(10, 10);
    IPv4InterfaceAddressTlv iPv4InterfaceAddressTlv1 = new IPv4InterfaceAddressTlv((int) 0x01010101);
    LinkedList<PcepValueType> llLinkDescriptorsSubTLVs1 = new LinkedList<PcepValueType>();
    LinkedList<PcepValueType> llLinkDescriptorsSubTLVs2 = new LinkedList<PcepValueType>();
    LinkedList<PcepValueType> llLinkDescriptorsSubTLVs3 = new LinkedList<PcepValueType>();

    boolean b = llLinkDescriptorsSubTLVs1.add(linkLocalRemoteIdentifiersTlv1);
    boolean c = llLinkDescriptorsSubTLVs2.add(linkLocalRemoteIdentifiersTlv1);
    boolean d = llLinkDescriptorsSubTLVs3.add(iPv4InterfaceAddressTlv1);

    final TELinkDescriptorsTLV tlv1 = TELinkDescriptorsTLV.of(llLinkDescriptorsSubTLVs1);
    final TELinkDescriptorsTLV tlv2 = TELinkDescriptorsTLV.of(llLinkDescriptorsSubTLVs2);
    final TELinkDescriptorsTLV tlv3 = TELinkDescriptorsTLV.of(llLinkDescriptorsSubTLVs3);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, tlv2).addEqualityGroup(tlv3).testEquals();
    }

}
