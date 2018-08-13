/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknode.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.genDpid;

/**
 * OpenstackNode util unit test.
 */
public final class OpenstackNodeUtilTest {

    /**
     * Tests the genDpid method.
     */
    @Test
    public void testGenDpid() {
        long one = 1;
        long ten = 10;
        long sixteen = 16;
        long seventeen = 17;
        long minus = -1;

        assertEquals("of:0000000000000001", genDpid(one));
        assertEquals("of:000000000000000a", genDpid(ten));
        assertEquals("of:0000000000000010", genDpid(sixteen));
        assertEquals("of:0000000000000011", genDpid(seventeen));
        assertNull(genDpid(minus));
    }
}
