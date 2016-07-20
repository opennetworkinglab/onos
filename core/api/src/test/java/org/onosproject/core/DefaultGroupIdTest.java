/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.core;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

/**
 * Test for DefaultGroupId.
 */
public class DefaultGroupIdTest {

    /**
     * Tests the equality of the instances.
     */
    @Test
    public void testEquality() {
        DefaultGroupId id1 = new DefaultGroupId((short) 1);
        DefaultGroupId id2 = new DefaultGroupId((short) 1);
        DefaultGroupId id3 = new DefaultGroupId((short) 2);

        new EqualsTester()
                .addEqualityGroup(id1, id2)
                .addEqualityGroup(id3)
                .testEquals();
    }

}
