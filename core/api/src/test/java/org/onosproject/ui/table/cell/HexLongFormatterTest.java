/*
 *  Copyright 2016-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.table.cell;

import org.junit.Test;
import org.onosproject.ui.table.CellFormatter;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link HexLongFormatter}.
 */
public class HexLongFormatterTest {

    private CellFormatter fmt = HexLongFormatter.INSTANCE;

    @Test
    public void nullValue() {
        assertEquals("null value", "", fmt.format(null));
    }

    @Test
    public void zero() {
        assertEquals("zero", "0x0", fmt.format(0L));
    }

    @Test
    public void one() {
        assertEquals("one", "0x1", fmt.format(1L));
    }

    @Test
    public void ten() {
        assertEquals("ten", "0xa", fmt.format(10L));
    }

    @Test
    public void twenty() {
        assertEquals("twenty", "0x14", fmt.format(20L));
    }

    @Test
    public void veryBig() {
        assertEquals("very big", "0x7048860f3a38", fmt.format(123456789101112L));
    }

    @Test
    public void maxLong() {
        assertEquals("max long", "0x7fffffffffffffff", fmt.format(Long.MAX_VALUE));
    }
}
