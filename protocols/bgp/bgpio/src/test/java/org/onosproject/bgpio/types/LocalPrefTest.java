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

import com.google.common.testing.EqualsTester;

/**
 * Test for LocalPref BGP Path Attribute.
 */
public class LocalPrefTest {
    private final int value1 = 800;
    private final int value2 = 300;
    private final LocalPref attr1 = new LocalPref(value1);
    private final LocalPref sameAsAttr1 = new LocalPref(value1);
    private final LocalPref attr2 = new LocalPref(value2);

    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(attr1, sameAsAttr1).addEqualityGroup(attr2).testEquals();
    }
}
