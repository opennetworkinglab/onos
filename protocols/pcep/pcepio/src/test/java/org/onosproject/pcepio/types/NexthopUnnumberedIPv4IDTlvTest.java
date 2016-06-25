/*
 * Copyright 2015-present Open Networking Laboratory
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

/**
 * Equality test for NexthopUnnumberedIPv4IDTlv.
 */
public class NexthopUnnumberedIPv4IDTlvTest {

    private final NexthopUnnumberedIPv4IDTlv tlv1 = new NexthopUnnumberedIPv4IDTlv(0x0A, 0x0A);
    private final NexthopUnnumberedIPv4IDTlv sameAsTlv1 = new NexthopUnnumberedIPv4IDTlv(0x0A, 0x0A);
    private final NexthopUnnumberedIPv4IDTlv tlv2 = NexthopUnnumberedIPv4IDTlv.of(0x0B, 0x0B);

    @Test
    public void basics() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
