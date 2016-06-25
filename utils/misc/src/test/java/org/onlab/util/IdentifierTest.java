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

package org.onlab.util;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test of the base identifier.
 */
public class IdentifierTest {

    @Test
    public void basics() {
        FooId id = new FooId(123);
        assertEquals("incorrect value", 123, (int) id.id());
    }

    @Test
    public void equality() {
        FooId a = new FooId(1);
        FooId b = new FooId(1);
        FooId c = new FooId(2);
        new EqualsTester().addEqualityGroup(a, b).addEqualityGroup(c).testEquals();
    }

    static class FooId extends Identifier<Integer> {
        FooId(int id) {
            super(id);
        }
    }

}