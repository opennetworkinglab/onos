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

import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.onosproject.pcepio.types.PathSetupTypeTlv;

/**
 * Test of the PathSetupTypeTlv.
 */
public class PathSetupTypeTlvTest {
    final int rawValue1 = 0x0A;
    final int rawValue2 = 0x0A;

    final PathSetupTypeTlv tlv1 = new PathSetupTypeTlv(rawValue1);
    final PathSetupTypeTlv tlv2 = PathSetupTypeTlv.of(tlv1.getInt());

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, tlv2).testEquals();
    }
}
