/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.drivers.microsemi.yang;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.onosproject.drivers.microsemi.yang.utils.CeVlanMapUtils;

public class CeVlanMapUtilsTest {

    @Test
    public void testGetVlanSet() {
        Short[] vlanArray = CeVlanMapUtils.getVlanSet("101:102");
        assertEquals(2, vlanArray.length);

        vlanArray = CeVlanMapUtils.getVlanSet("101:103,107,110:115");
        assertEquals(10, vlanArray.length);

        vlanArray = CeVlanMapUtils.getVlanSet("1:4095");
        assertEquals(4095, vlanArray.length);
    }

    @Test
    public void testVlanListAsString() {
        String ceVlanMap = CeVlanMapUtils.vlanListAsString(new Short[]{101, 102, 103});
        assertEquals("101:103", ceVlanMap);

        ceVlanMap = CeVlanMapUtils.vlanListAsString(new Short[]{0, 101, 104, 108});
        assertEquals("101,104,108", ceVlanMap);
    }

    @Test
    public void testAddtoCeVlanMap() {
        String ceVlanMap = CeVlanMapUtils.addtoCeVlanMap("101:102", (short) 103);
        assertEquals("101:103", ceVlanMap);

        ceVlanMap = CeVlanMapUtils.addtoCeVlanMap("101:102", (short) 0);
        assertEquals("101:102", ceVlanMap);

        ceVlanMap = CeVlanMapUtils.addtoCeVlanMap("101:102", (short) 104);
        assertEquals("101:102,104", ceVlanMap);
    }

    @Test
    public void testRemoveZeroIfPossible() {
        String ceVlanMap = CeVlanMapUtils.removeZeroIfPossible("101:102");
        assertEquals("101:102", ceVlanMap);

        ceVlanMap = CeVlanMapUtils.removeZeroIfPossible("0,101:102");
        assertEquals("101:102", ceVlanMap);

        ceVlanMap = CeVlanMapUtils.removeZeroIfPossible("0");
        assertEquals("0", ceVlanMap);
    }

    @Test
    public void testRemoveFromCeVlanMap() {
        String ceVlanMap = CeVlanMapUtils.removeFromCeVlanMap("101:102", (short) 102);
        assertEquals("101", ceVlanMap);

        ceVlanMap = CeVlanMapUtils.removeFromCeVlanMap("101:103", (short) 102);
        assertEquals("101,103", ceVlanMap);
    }

    @Test
    public void testCombineVlanSets() {
        assertEquals("101:104", CeVlanMapUtils.combineVlanSets("101:102", "103:104"));

        assertEquals("101:103", CeVlanMapUtils.combineVlanSets("101:102", "103"));

        assertEquals("101:102,104", CeVlanMapUtils.combineVlanSets("101:102", "104"));

        assertEquals("99,101:102", CeVlanMapUtils.combineVlanSets("101:102", "99"));

        assertEquals("101:102", CeVlanMapUtils.combineVlanSets("101:102", "0"));

    }
}
