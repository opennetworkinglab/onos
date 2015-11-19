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
 * Test case for TE link descriptors Tlv.
 */
public class TELinkDescriptorsTlvTest {
    private final LinkLocalRemoteIdentifiersTlv linkLocalRemoteIdentifiersTlv1 = new
            LinkLocalRemoteIdentifiersTlv(10, 10);
    private final IPv4InterfaceAddressTlv iPv4InterfaceAddressTlv1 = new IPv4InterfaceAddressTlv(0x01010101);

    private final LinkLocalRemoteIdentifiersTlv linkLocalRemoteIdentifiersTlv2 = new
            LinkLocalRemoteIdentifiersTlv(20, 20);
    private final IPv4InterfaceAddressTlv iPv4InterfaceAddressTlv2 = new IPv4InterfaceAddressTlv(0x02020202);

    private final LinkedList<PcepValueType> llLinkDescriptorsSubTLVs1 = new LinkedList<>();
    private final boolean a = llLinkDescriptorsSubTLVs1.add(linkLocalRemoteIdentifiersTlv1);
    private final boolean b = llLinkDescriptorsSubTLVs1.add(iPv4InterfaceAddressTlv1);

    private final LinkedList<PcepValueType> llLinkDescriptorsSubTLVs2 = new LinkedList<>();
    private final boolean c = llLinkDescriptorsSubTLVs2.add(linkLocalRemoteIdentifiersTlv2);
    private final boolean d = llLinkDescriptorsSubTLVs2.add(iPv4InterfaceAddressTlv2);

    private final TELinkDescriptorsTlv tlv1 = TELinkDescriptorsTlv.of(llLinkDescriptorsSubTLVs1);
    private final TELinkDescriptorsTlv sameAstlv1 = TELinkDescriptorsTlv.of(llLinkDescriptorsSubTLVs1);
    private final TELinkDescriptorsTlv tlv2 = TELinkDescriptorsTlv.of(llLinkDescriptorsSubTLVs2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, sameAstlv1).addEqualityGroup(tlv2).testEquals();
    }

}
