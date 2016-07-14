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
package org.onosproject.event;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Change}.
 */
public class ChangeTest {

    @Test
    public void getters() {
        Change<String> change = new Change<>("a", "b");
        assertEquals("a", change.oldValue());
        assertEquals("b", change.newValue());
    }

    @Test
    public void equality() {
        new EqualsTester()
        .addEqualityGroup(new Change<>("foo", "bar"),
                          new Change<>("foo", "bar"))
        .addEqualityGroup(new Change<>("bar", "car"))
        .testEquals();
    }
}
