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
 * Test case for Symbolic path tlv.
 */
public class SymbolicPathNameTlvTest {

    private final byte[] value1 = {0x41 };
    private final Short length1 = 2;
    private final SymbolicPathNameTlv tlv1 = SymbolicPathNameTlv.of(value1, length1);

    private final byte[] value2 = {0x41 };
    private final Short length2 = 2;
    private final SymbolicPathNameTlv tlv2 = SymbolicPathNameTlv.of(value1, length2);

    private final byte[] value3 = {0x41, 0x43 };
    private final Short length3 = 3;
    private final SymbolicPathNameTlv tlv3 = SymbolicPathNameTlv.of(value3, length3);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, tlv2).addEqualityGroup(tlv3).testEquals();
    }

}
