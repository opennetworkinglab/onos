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
 * Test case for Remote TE Node Descriptors tlv.
 */
public class RemoteNodeDescriptorsTlvTest {

    private final AutonomousSystemSubTlv autonomousSystemTlv1 = new AutonomousSystemSubTlv(10);
    private final BgpLsIdentifierSubTlv bGPLSidentifierTlv1 = new BgpLsIdentifierSubTlv(20);

    private final AutonomousSystemSubTlv autonomousSystemTlv2 = new AutonomousSystemSubTlv(20);
    private final BgpLsIdentifierSubTlv bGPLSidentifierTlv2 = new BgpLsIdentifierSubTlv(30);

    private final List<PcepValueType> llRemoteTENodeDescriptorSubTLV1 = new LinkedList<>();
    private final boolean a = llRemoteTENodeDescriptorSubTLV1.add(autonomousSystemTlv1);
    private final boolean b = llRemoteTENodeDescriptorSubTLV1.add(bGPLSidentifierTlv1);

    private final List<PcepValueType> llRemoteTENodeDescriptorSubTLV2 = new LinkedList<>();
    private final boolean c = llRemoteTENodeDescriptorSubTLV2.add(autonomousSystemTlv2);
    private final boolean d = llRemoteTENodeDescriptorSubTLV2.add(bGPLSidentifierTlv2);

    private final RemoteNodeDescriptorsTlv tlv1 = RemoteNodeDescriptorsTlv.of(llRemoteTENodeDescriptorSubTLV1);
    private final RemoteNodeDescriptorsTlv sameAsTlv1 =
            RemoteNodeDescriptorsTlv.of(llRemoteTENodeDescriptorSubTLV1);
    private final RemoteNodeDescriptorsTlv tlv2 = RemoteNodeDescriptorsTlv.of(llRemoteTENodeDescriptorSubTLV2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, sameAsTlv1).addEqualityGroup(tlv2).testEquals();
    }

}
