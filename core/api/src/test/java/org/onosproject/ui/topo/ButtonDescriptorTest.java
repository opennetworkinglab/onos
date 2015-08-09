/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.topo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link ButtonDescriptor}.
 */
public class ButtonDescriptorTest {

    private static final String ID = "my-id";
    private static final String GID = "my-glyphId";
    private static final String TT = "my-tewltyp";

    private ButtonDescriptor bd;


    @Test
    public void basic() {
        bd = new ButtonDescriptor(ID, GID, TT);

        assertEquals("bad id", ID, bd.id());
        assertEquals("bad gid", GID, bd.glyphId());
        assertEquals("bad tt", TT, bd.tooltip());
    }

}
