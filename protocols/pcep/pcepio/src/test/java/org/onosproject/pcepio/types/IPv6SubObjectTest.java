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
 * Test of the IPv6SubObject.
 */
public class IPv6SubObjectTest {

    private final byte[] b1 = new byte[] {(byte) 0xFE, (byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x02,
            (byte) 0xB3, (byte) 0xFF, (byte) 0xFE, 0x1E, (byte) 0x83, 0x29, 0x00, 0x02, 0x00, 0x00};
    private final byte[] b2 = new byte[] {(byte) 0xFE, (byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x02,
            (byte) 0xB3, (byte) 0xFF, (byte) 0xFE, 0x1E, (byte) 0x83, 0x30, 0x00, 0x02, 0x00, 0x00 };

    private final IPv6SubObject subObj1 = IPv6SubObject.of(b1);
    private final IPv6SubObject sameAsSubObj1 = IPv6SubObject.of(b1);
    private final IPv6SubObject subObj2 = IPv6SubObject.of(b2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(subObj1, sameAsSubObj1).addEqualityGroup(subObj2).testEquals();
    }
}
