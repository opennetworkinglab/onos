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

package org.onosproject.ui.table.cell;

import org.junit.Test;
import org.onosproject.ui.table.CellFormatter;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DefaultCellFormatter}.
 */
public class DefaultCellFormatterTest {

    private static final String UNEX = "Unexpected result";
    private static final String SOME_STRING = "SoMeStRiNg";

    private static class TestClass {
        @Override
        public String toString() {
            return SOME_STRING;
        }
    }

    private CellFormatter fmt = DefaultCellFormatter.INSTANCE;

    @Test
    public void formatNull() {
        assertEquals(UNEX, "", fmt.format(null));
    }

    @Test
    public void formatInteger() {
        assertEquals(UNEX, "3", fmt.format(3));
    }

    @Test
    public void formatTrue() {
        assertEquals(UNEX, "true", fmt.format(true));
    }

    @Test
    public void formatFalse() {
        assertEquals(UNEX, "false", fmt.format(false));
    }

    @Test
    public void formatString() {
        assertEquals(UNEX, "FOO", fmt.format("FOO"));
    }

    @Test
    public void formatObject() {
        assertEquals(UNEX, SOME_STRING, fmt.format(new TestClass()));
    }
}
