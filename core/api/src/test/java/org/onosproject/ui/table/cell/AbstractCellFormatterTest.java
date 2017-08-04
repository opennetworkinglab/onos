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

package org.onosproject.ui.table.cell;

import org.junit.Test;
import org.onosproject.ui.table.CellFormatter;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link AbstractCellFormatter}.
 */
public class AbstractCellFormatterTest {

    private static final String MOCK_OUTPUT = "Mock!!";

    private static class Concrete extends AbstractCellFormatter {
        @Override
        protected String nonNullFormat(Object value) {
            return MOCK_OUTPUT;
        }
    }

    private CellFormatter frm = new Concrete();

    @Test
    public void nullInput() {
        assertEquals("wrong result", "", frm.format(null));
    }

    // mock output, but check that our method was invoked...
    @Test
    public void nonNullInput() {
        assertEquals("what?", MOCK_OUTPUT, frm.format(1));
    }

}
