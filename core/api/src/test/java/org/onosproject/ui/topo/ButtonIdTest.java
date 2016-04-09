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

package org.onosproject.ui.topo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ButtonId}.
 */
public class ButtonIdTest {

    private static final String ID1 = "id-1";
    private static final String ID2 = "id-2";

    private ButtonId b1, b2;


    @Test
    public void basic() {
        b1 = new ButtonId(ID1);
    }

    @Test
    public void same() {
        b1 = new ButtonId(ID1);
        b2 = new ButtonId(ID1);
        assertFalse("same ref?", b1 == b2);
        assertTrue("not equal?", b1.equals(b2));
    }

    @Test
    public void notSame() {
        b1 = new ButtonId(ID1);
        b2 = new ButtonId(ID2);
        assertFalse("same ref?", b1 == b2);
        assertFalse("equal?", b1.equals(b2));
    }
}
