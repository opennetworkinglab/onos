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
 * Test case for Remote TE Node Descriptors tlv.
 */
public class RemoteTENodeDescriptorsTlvTest {

    private final AutonomousSystemTlv autonomousSystemTlv1 = new AutonomousSystemTlv(10);
    private final BGPLSidentifierTlv bGPLSidentifierTlv1 = new BGPLSidentifierTlv(20);

    private final AutonomousSystemTlv autonomousSystemTlv2 = new AutonomousSystemTlv(20);
    private final BGPLSidentifierTlv bGPLSidentifierTlv2 = new BGPLSidentifierTlv(30);

    private final LinkedList<PcepValueType> llRemoteTENodeDescriptorSubTLV1 = new LinkedList<>();
    private final boolean a = llRemoteTENodeDescriptorSubTLV1.add(autonomousSystemTlv1);
    private final boolean b = llRemoteTENodeDescriptorSubTLV1.add(bGPLSidentifierTlv1);

    private final LinkedList<PcepValueType> llRemoteTENodeDescriptorSubTLV2 = new LinkedList<>();
    private final boolean c = llRemoteTENodeDescriptorSubTLV2.add(autonomousSystemTlv2);
    private final boolean d = llRemoteTENodeDescriptorSubTLV2.add(bGPLSidentifierTlv2);

    private final RemoteTENodeDescriptorsTlv tlv1 = RemoteTENodeDescriptorsTlv.of(llRemoteTENodeDescriptorSubTLV1);
    private final RemoteTENodeDescriptorsTlv sameAsTlv1 =
            RemoteTENodeDescriptorsTlv.of(llRemoteTENodeDescriptorSubTLV1);
    private final RemoteTENodeDescriptorsTlv tlv2 = RemoteTENodeDescriptorsTlv.of(llRemoteTENodeDescriptorSubTLV2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, sameAsTlv1).addEqualityGroup(tlv2).testEquals();
    }

}
