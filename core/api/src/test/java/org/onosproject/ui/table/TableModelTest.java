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
 */

package org.onosproject.ui.table;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link TableModel}.
 */
public class TableModelTest {

    private static final String FOO = "foo";
    private static final String BAR = "bar";
    private static final String BAZ = "baz";
    private static final String ZOO = "zoo";

    private static class TestFmtr implements CellFormatter {
        @Override
        public String format(Object value) {
            return "(" + value + ")";
        }
    }

    private TableModel tm;
    private TableRow[] rows;
    private CellFormatter fmt;

    @Test(expected = NullPointerException.class)
    public void guardAgainstNull() {
        tm = new TableModel(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void guardAgainstEmpty() {
        tm = new TableModel();
    }

    @Test(expected = IllegalArgumentException.class)
    public void guardAgainstDuplicateCols() {
        tm = new TableModel(FOO, BAR, FOO);
    }

    @Test
    public void basic() {
        tm = new TableModel(FOO, BAR);
        assertEquals("column count", 2, tm.columnCount());
        assertEquals("row count", 0, tm.rowCount());

        rows = tm.getTableRows();
        assertEquals("row count alt", 0, rows.length);
    }

    @Test
    public void defaultFormatter() {
        tm = new TableModel(FOO);
        fmt = tm.getFormatter(FOO);
        assertTrue("Wrong formatter", fmt instanceof DefaultCellFormatter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void formatterBadColumn() {
        tm = new TableModel(FOO);
        fmt = tm.getFormatter(BAR);
    }

    @Test
    public void altFormatter() {
        tm = new TableModel(FOO, BAR);
        tm.setFormatter(BAR, new TestFmtr());

        fmt = tm.getFormatter(FOO);
        assertTrue("Wrong formatter", fmt instanceof DefaultCellFormatter);
        assertEquals("Wrong result", "2", fmt.format(2));

        fmt = tm.getFormatter(BAR);
        assertTrue("Wrong formatter", fmt instanceof TestFmtr);
        assertEquals("Wrong result", "(2)", fmt.format(2));
    }

    @Test
    public void emptyRow() {
        tm = new TableModel(FOO, BAR);
        tm.addRow();
        assertEquals("bad row count", 1, tm.rowCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rowBadColumn() {
        tm = new TableModel(FOO, BAR);
        tm.addRow().cell(ZOO, 2);
    }

    @Test(expected = NullPointerException.class)
    public void rowNullValue() {
        tm = new TableModel(FOO, BAR);
        tm.addRow().cell(FOO, null);
    }

    @Test
    public void simpleRow() {
        tm = new TableModel(FOO, BAR);
        tm.addRow().cell(FOO, 3).cell(BAR, true);
        assertEquals("bad row count", 1, tm.rowCount());
        TableModel.Row r = tm.getRows()[0];
        assertEquals("bad cell", 3, r.get(FOO));
        assertEquals("bad cell", true, r.get(BAR));
    }
}
