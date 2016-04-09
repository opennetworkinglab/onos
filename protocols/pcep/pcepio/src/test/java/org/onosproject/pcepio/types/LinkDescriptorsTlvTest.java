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
 * Test case for TE link descriptors Tlv.
 */
public class LinkDescriptorsTlvTest {
    private final LinkLocalRemoteIdentifiersSubTlv linkLocalRemoteIdentifiersTlv1 = new
            LinkLocalRemoteIdentifiersSubTlv(10, 10);
    private final IPv4InterfaceAddressSubTlv iPv4InterfaceAddressTlv1 = new IPv4InterfaceAddressSubTlv(0x01010101);

    private final LinkLocalRemoteIdentifiersSubTlv linkLocalRemoteIdentifiersTlv2 = new
            LinkLocalRemoteIdentifiersSubTlv(20, 20);
    private final IPv4InterfaceAddressSubTlv iPv4InterfaceAddressTlv2 = new IPv4InterfaceAddressSubTlv(0x02020202);

    private final List<PcepValueType> llLinkDescriptorsSubTLVs1 = new LinkedList<>();
    private final boolean a = llLinkDescriptorsSubTLVs1.add(linkLocalRemoteIdentifiersTlv1);
    private final boolean b = llLinkDescriptorsSubTLVs1.add(iPv4InterfaceAddressTlv1);

    private final List<PcepValueType> llLinkDescriptorsSubTLVs2 = new LinkedList<>();
    private final boolean c = llLinkDescriptorsSubTLVs2.add(linkLocalRemoteIdentifiersTlv2);
    private final boolean d = llLinkDescriptorsSubTLVs2.add(iPv4InterfaceAddressTlv2);

    private final LinkDescriptorsTlv tlv1 = LinkDescriptorsTlv.of(llLinkDescriptorsSubTLVs1);
    private final LinkDescriptorsTlv sameAstlv1 = LinkDescriptorsTlv.of(llLinkDescriptorsSubTLVs1);
    private final LinkDescriptorsTlv tlv2 = LinkDescriptorsTlv.of(llLinkDescriptorsSubTLVs2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, sameAstlv1).addEqualityGroup(tlv2).testEquals();
    }

}
