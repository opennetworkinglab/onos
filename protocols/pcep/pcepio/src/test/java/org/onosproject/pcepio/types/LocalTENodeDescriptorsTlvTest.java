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
 * Test of the LocalTENodeDescriptorsTlv.
 */
public class LocalTENodeDescriptorsTlvTest {

    private final AutonomousSystemTlv baAutoSysTlvRawValue1 = new AutonomousSystemTlv(1);
    private final BGPLSidentifierTlv baBgplsIdRawValue1 = new BGPLSidentifierTlv(1);

    private final AutonomousSystemTlv baAutoSysTlvRawValue2 = new AutonomousSystemTlv(2);
    private final BGPLSidentifierTlv baBgplsIdRawValue2 = new BGPLSidentifierTlv(2);

    private final LinkedList<PcepValueType> llNodeDescriptorSubTLVs1 = new LinkedList<PcepValueType>();
    private final boolean a = llNodeDescriptorSubTLVs1.add(baAutoSysTlvRawValue1);
    private final boolean b = llNodeDescriptorSubTLVs1.add(baBgplsIdRawValue1);

    private final LinkedList<PcepValueType> llNodeDescriptorSubTLVs2 = new LinkedList<PcepValueType>();
    private final boolean c = llNodeDescriptorSubTLVs2.add(baAutoSysTlvRawValue2);
    private final boolean d = llNodeDescriptorSubTLVs2.add(baBgplsIdRawValue2);

    private final LocalTENodeDescriptorsTlv tlv1 = LocalTENodeDescriptorsTlv.of(llNodeDescriptorSubTLVs1);
    private final LocalTENodeDescriptorsTlv sameAstlv1 = LocalTENodeDescriptorsTlv.of(llNodeDescriptorSubTLVs1);
    private final LocalTENodeDescriptorsTlv tlv2 = LocalTENodeDescriptorsTlv.of(llNodeDescriptorSubTLVs2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, sameAstlv1).addEqualityGroup(tlv2).testEquals();
    }
}
