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

package org.onosproject.ui.topo;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Mod}.
 */
public class ModTest {

    private static final String AAA = "aaa";
    private static final String BBB = "bbb";

    private Mod mod1;
    private Mod mod2;

    @Test(expected = NullPointerException.class)
    public void nullId() {
        new Mod(null);
    }

    @Test
    public void basic() {
        mod1 = new Mod(AAA);
        assertEquals("wrong id", AAA, mod1.toString());
    }

    @Test
    public void equivalence() {
        mod1 = new Mod(AAA);
        mod2 = new Mod(AAA);
        assertNotSame("oops", mod1, mod2);
        assertEquals("not equivalent", mod1, mod2);
    }

    @Test
    public void comparable() {
        mod1 = new Mod(AAA);
        mod2 = new Mod(BBB);
        assertNotEquals("what?", mod1, mod2);
        assertTrue(mod1.compareTo(mod2) < 0);
        assertTrue(mod2.compareTo(mod1) > 0);
    }
}
