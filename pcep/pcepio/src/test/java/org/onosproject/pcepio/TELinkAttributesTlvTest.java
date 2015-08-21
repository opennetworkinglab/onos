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
import org.onosproject.pcepio.types.AdministrativeGroupTlv;
import org.onosproject.pcepio.types.MaximumReservableLinkBandwidthTlv;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.TELinkAttributesTlv;

import com.google.common.testing.EqualsTester;

/**
 * Test case for TE Link Attribute Tlv.
 */
public class TELinkAttributesTlvTest {

    AdministrativeGroupTlv administrativeGroupTlv1 = new AdministrativeGroupTlv(10);
    MaximumReservableLinkBandwidthTlv maximumReservableLinkBandwidthTlv1 = new MaximumReservableLinkBandwidthTlv(20);;
    LinkedList<PcepValueType> llLinkAttributesSubTLVs = new LinkedList<PcepValueType>();
    LinkedList<PcepValueType> llLinkAttributesSubTLVs2 = new LinkedList<PcepValueType>();
    LinkedList<PcepValueType> llLinkAttributesSubTLVs3 = new LinkedList<PcepValueType>();

    boolean b = llLinkAttributesSubTLVs.add(administrativeGroupTlv1);

    boolean c = llLinkAttributesSubTLVs2.add(administrativeGroupTlv1);
    boolean d = llLinkAttributesSubTLVs3.add(maximumReservableLinkBandwidthTlv1);

    final TELinkAttributesTlv tlv1 = TELinkAttributesTlv.of(llLinkAttributesSubTLVs);
    final TELinkAttributesTlv tlv2 = TELinkAttributesTlv.of(llLinkAttributesSubTLVs2);
    final TELinkAttributesTlv tlv3 = TELinkAttributesTlv.of(llLinkAttributesSubTLVs3);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, tlv2).addEqualityGroup(tlv3).testEquals();
    }

}
