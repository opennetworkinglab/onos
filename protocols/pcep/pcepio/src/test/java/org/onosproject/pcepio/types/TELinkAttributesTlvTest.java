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
 * Test case for TE Link Attribute Tlv.
 */
public class TELinkAttributesTlvTest {

    private final AdministrativeGroupTlv administrativeGroupTlv1 = new AdministrativeGroupTlv(10);
    private final MaximumReservableLinkBandwidthTlv maximumReservableLinkBandwidthTlv1 =
            new MaximumReservableLinkBandwidthTlv(20);

    private final AdministrativeGroupTlv administrativeGroupTlv2 = new AdministrativeGroupTlv(20);
    private final MaximumReservableLinkBandwidthTlv maximumReservableLinkBandwidthTlv2 =
            new MaximumReservableLinkBandwidthTlv(30);

    private final LinkedList<PcepValueType> llLinkAttributesSubTLV1 = new LinkedList<>();
    private final boolean a = llLinkAttributesSubTLV1.add(administrativeGroupTlv1);
    private final boolean b = llLinkAttributesSubTLV1.add(maximumReservableLinkBandwidthTlv1);

    private final LinkedList<PcepValueType> llLinkAttributesSubTLV2 = new LinkedList<>();

    private final boolean c = llLinkAttributesSubTLV2.add(administrativeGroupTlv2);
    private final boolean d = llLinkAttributesSubTLV2.add(maximumReservableLinkBandwidthTlv2);

    private final TELinkAttributesTlv tlv1 = TELinkAttributesTlv.of(llLinkAttributesSubTLV1);
    private final TELinkAttributesTlv sameAsTlv1 = TELinkAttributesTlv.of(llLinkAttributesSubTLV1);
    private final TELinkAttributesTlv tlv2 = TELinkAttributesTlv.of(llLinkAttributesSubTLV2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, sameAsTlv1).addEqualityGroup(tlv2).testEquals();
    }

}
