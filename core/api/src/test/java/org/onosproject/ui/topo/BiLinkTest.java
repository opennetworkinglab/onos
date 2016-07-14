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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link BiLink}.
 */
public class BiLinkTest extends BiLinkTestBase {

    private static final String EXP_ID_AB = "device-a/1-device-b/2";

    private BiLink blink;

    @Test
    public void basic() {
        blink = new ConcreteLink(KEY_AB, LINK_AB);
        assertEquals("wrong id", EXP_ID_AB, blink.linkId());
        assertEquals("wrong key", KEY_AB, blink.key());
        assertEquals("wrong link one", LINK_AB, blink.one());
        assertNull("what?", blink.two());

        blink.setOther(LINK_BA);
        assertEquals("wrong link two", LINK_BA, blink.two());
    }

    @Test(expected = NullPointerException.class)
    public void nullKey() {
        new ConcreteLink(null, LINK_AB);
    }

    @Test(expected = NullPointerException.class)
    public void nullLink() {
        new ConcreteLink(KEY_AB, null);
    }

    @Test(expected = NullPointerException.class)
    public void nullOther() {
        blink = new ConcreteLink(KEY_AB, LINK_AB);
        blink.setOther(null);
    }
}

