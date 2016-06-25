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
package org.onosproject.bgpio.types;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Test for Origin BGP Path Attribute.
 */
public class OriginTest {
    private final byte value1 = 0x01;
    private final byte value2 = 0x02;
    private final Origin attr1 = new Origin(value1);
    private final Origin sameAsAttr1 = new Origin(value1);
    private final Origin attr2 = new Origin(value2);

    @Test
    public void basics() {
        new EqualsTester()
        .addEqualityGroup(attr1, sameAsAttr1)
        .addEqualityGroup(attr2)
        .testEquals();
    }
}
