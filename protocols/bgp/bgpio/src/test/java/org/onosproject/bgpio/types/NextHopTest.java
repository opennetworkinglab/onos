/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.bgpio.types;

import org.junit.Test;
import org.onlab.packet.Ip4Address;

import com.google.common.testing.EqualsTester;

/**
 * Test for NextHop BGP Path Attribute.
 */
public class NextHopTest {
    private final Ip4Address value1 = Ip4Address.valueOf("12.12.12.12");
    private final Ip4Address value2 = Ip4Address.valueOf("12.12.12.13");
    private final NextHop attr1 = new NextHop(value1);
    private final NextHop sameAsAttr1 = new NextHop(value1);
    private final NextHop attr2 = new NextHop(value2);

    @Test
    public void basics() {
        new EqualsTester()
        .addEqualityGroup(attr1, sameAsAttr1)
        .addEqualityGroup(attr2)
        .testEquals();
    }
}
