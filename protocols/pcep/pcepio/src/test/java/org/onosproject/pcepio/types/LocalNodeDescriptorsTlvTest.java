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
 * Test of the LocalNodeDescriptorsTlv.
 */
public class LocalNodeDescriptorsTlvTest {

    private final AutonomousSystemSubTlv baAutoSysTlvRawValue1 = new AutonomousSystemSubTlv(1);
    private final BgpLsIdentifierSubTlv baBgplsIdRawValue1 = new BgpLsIdentifierSubTlv(1);

    private final AutonomousSystemSubTlv baAutoSysTlvRawValue2 = new AutonomousSystemSubTlv(2);
    private final BgpLsIdentifierSubTlv baBgplsIdRawValue2 = new BgpLsIdentifierSubTlv(2);

    private final List<PcepValueType> llNodeDescriptorSubTLVs1 = new LinkedList<PcepValueType>();
    private final boolean a = llNodeDescriptorSubTLVs1.add(baAutoSysTlvRawValue1);
    private final boolean b = llNodeDescriptorSubTLVs1.add(baBgplsIdRawValue1);

    private final List<PcepValueType> llNodeDescriptorSubTLVs2 = new LinkedList<PcepValueType>();
    private final boolean c = llNodeDescriptorSubTLVs2.add(baAutoSysTlvRawValue2);
    private final boolean d = llNodeDescriptorSubTLVs2.add(baBgplsIdRawValue2);

    private final LocalNodeDescriptorsTlv tlv1 = LocalNodeDescriptorsTlv.of(llNodeDescriptorSubTLVs1);
    private final LocalNodeDescriptorsTlv sameAstlv1 = LocalNodeDescriptorsTlv.of(llNodeDescriptorSubTLVs1);
    private final LocalNodeDescriptorsTlv tlv2 = LocalNodeDescriptorsTlv.of(llNodeDescriptorSubTLVs2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, sameAstlv1).addEqualityGroup(tlv2).testEquals();
    }
}
