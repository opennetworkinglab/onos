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
import org.onosproject.pcepio.types.AutonomousSystemTlv;
import org.onosproject.pcepio.types.BGPLSidentifierTlv;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.RemoteTENodeDescriptorsTLV;

import com.google.common.testing.EqualsTester;

/**
 * Test case for Remote TE Node Descriptors tlv.
 */
public class RemoteTENodeDescriptorsTLVTest {

    AutonomousSystemTlv autonomousSystemTlv1 = new AutonomousSystemTlv(10);
    BGPLSidentifierTlv bGPLSidentifierTlv1 = new BGPLSidentifierTlv(20);;
    LinkedList<PcepValueType> llRemoteTENodeDescriptorSubTLVs1 = new LinkedList<PcepValueType>();
    LinkedList<PcepValueType> llRemoteTENodeDescriptorSubTLVs2 = new LinkedList<PcepValueType>();
    LinkedList<PcepValueType> llRemoteTENodeDescriptorSubTLVs3 = new LinkedList<PcepValueType>();

    boolean b = llRemoteTENodeDescriptorSubTLVs1.add(autonomousSystemTlv1);

    boolean c = llRemoteTENodeDescriptorSubTLVs2.add(autonomousSystemTlv1);
    boolean d = llRemoteTENodeDescriptorSubTLVs3.add(bGPLSidentifierTlv1);

    final RemoteTENodeDescriptorsTLV tlv1 = RemoteTENodeDescriptorsTLV.of(llRemoteTENodeDescriptorSubTLVs1);
    final RemoteTENodeDescriptorsTLV tlv2 = RemoteTENodeDescriptorsTLV.of(llRemoteTENodeDescriptorSubTLVs2);
    final RemoteTENodeDescriptorsTLV tlv3 = RemoteTENodeDescriptorsTLV.of(llRemoteTENodeDescriptorSubTLVs3);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, tlv2).addEqualityGroup(tlv3).testEquals();
    }

}
