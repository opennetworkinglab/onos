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
 * Test case for TE Link Attribute Tlv.
 */
public class LinkAttributesTlvTest {

    private final AdministrativeGroupSubTlv administrativeGroupTlv1 = new AdministrativeGroupSubTlv(10);
    private final MaximumReservableLinkBandwidthSubTlv maximumReservableLinkBandwidthTlv1 =
            new MaximumReservableLinkBandwidthSubTlv(20);

    private final AdministrativeGroupSubTlv administrativeGroupTlv2 = new AdministrativeGroupSubTlv(20);
    private final MaximumReservableLinkBandwidthSubTlv maximumReservableLinkBandwidthTlv2 =
            new MaximumReservableLinkBandwidthSubTlv(30);

    private final List<PcepValueType> llLinkAttributesSubTLV1 = new LinkedList<>();
    private final boolean a = llLinkAttributesSubTLV1.add(administrativeGroupTlv1);
    private final boolean b = llLinkAttributesSubTLV1.add(maximumReservableLinkBandwidthTlv1);

    private final List<PcepValueType> llLinkAttributesSubTLV2 = new LinkedList<>();

    private final boolean c = llLinkAttributesSubTLV2.add(administrativeGroupTlv2);
    private final boolean d = llLinkAttributesSubTLV2.add(maximumReservableLinkBandwidthTlv2);

    private final LinkAttributesTlv tlv1 = LinkAttributesTlv.of(llLinkAttributesSubTLV1);
    private final LinkAttributesTlv sameAsTlv1 = LinkAttributesTlv.of(llLinkAttributesSubTLV1);
    private final LinkAttributesTlv tlv2 = LinkAttributesTlv.of(llLinkAttributesSubTLV2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, sameAsTlv1).addEqualityGroup(tlv2).testEquals();
    }

}
