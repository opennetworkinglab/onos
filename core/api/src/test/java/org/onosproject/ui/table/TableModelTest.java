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

package org.onosproject.ui.table;

import org.junit.Test;
import org.onosproject.ui.table.TableModel.SortDir;
import org.onosproject.ui.table.cell.DefaultCellFormatter;
import org.onosproject.ui.table.cell.HexFormatter;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link TableModel}.
 */
public class TableModelTest {

    private static final String UNEX_SORT = "unexpected sort: index ";

    private static final String FOO = "foo";
    private static final String BAR = "bar";
    private static final String ZOO = "zoo";
    private static final String ID = "id";
    private static final String ALPHA = "alpha";
    private static final String NUMBER = "number";

    private enum StarWars {
        LUKE_SKYWALKER, LEIA_ORGANA, HAN_SOLO, C3PO, R2D2, JABBA_THE_HUTT
    }

    private static class ParenFormatter implements CellFormatter {
        @Override
        public String format(Object value) {
            return "(" + value + ")";
        }
    }

    private TableModel tm;
    private TableModel.Row[] rows;
    private TableModel.Row row;
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
        tm.setFormatter(BAR, new ParenFormatter());

        fmt = tm.getFormatter(FOO);
        assertTrue("Wrong formatter", fmt instanceof DefaultCellFormatter);
        assertEquals("Wrong result", "2", fmt.format(2));

        fmt = tm.getFormatter(BAR);
        assertTrue("Wrong formatter", fmt instanceof ParenFormatter);
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

    @Test
    public void simpleRow() {
        tm = new TableModel(FOO, BAR);
        tm.addRow().cell(FOO, 3).cell(BAR, true);
        assertEquals("bad row count", 1, tm.rowCount());
        row = tm.getRows()[0];
        assertEquals("bad cell", 3, row.get(FOO));
        assertEquals("bad cell", true, row.get(BAR));
    }

    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String THREE = "three";
    private static final String FOUR = "four";
    private static final String ELEVEN = "eleven";
    private static final String TWELVE = "twelve";
    private static final String TWENTY = "twenty";
    private static final String THIRTY = "thirty";

    private static final String[] NAMES = {
            FOUR,
            THREE,
            TWO,
            ONE,
            ELEVEN,
            TWELVE,
            THIRTY,
            TWENTY,
    };
    private static final String[] SORTED_NAMES = {
            ELEVEN,
            FOUR,
            ONE,
            THIRTY,
            THREE,
            TWELVE,
            TWENTY,
            TWO,
    };

    private static final int[] NUMBERS = {
        4, 3, 2, 1, 11, 12, 30, 20
    };

    private static final int[] SORTED_NUMBERS = {
        1, 2, 3, 4, 11, 12, 20, 30
    };

    private static final String[] SORTED_HEX = {
        "0x1", "0x2", "0x3", "0x4", "0xb", "0xc", "0x14", "0x1e"
    };

    @Test
    public void verifyTestData() {
        // not a unit test per se, but will fail if we don't keep
        // the three test arrays in sync
        int nalen = NAMES.length;
        int snlen = SORTED_NAMES.length;
        int nulen = NUMBERS.length;

        if (nalen != snlen || nalen != nulen) {
            fail("test data array size discrepancy");
        }
    }

    private void initUnsortedTable() {
        tm = new TableModel(FOO, BAR);
        for (int i = 0; i < NAMES.length; i++) {
            tm.addRow().cell(FOO, NAMES[i]).cell(BAR, NUMBERS[i]);
        }
    }

    @Test
    public void tableStringSort() {
        initUnsortedTable();

        // sort by name
        tm.sort(FOO, SortDir.ASC, null, null);

        // verify results
        rows = tm.getRows();
        int nr = rows.length;
        assertEquals("row count", NAMES.length, nr);
        for (int i = 0; i < nr; i++) {
            assertEquals(UNEX_SORT + i, SORTED_NAMES[i], rows[i].get(FOO));
        }

        // now the other way
        tm.sort(FOO, SortDir.DESC, null, null);

        // verify results
        rows = tm.getRows();
        nr = rows.length;
        assertEquals("row count", NAMES.length, nr);
        for (int i = 0; i < nr; i++) {
            assertEquals(UNEX_SORT + i,
                         SORTED_NAMES[nr - 1 - i], rows[i].get(FOO));
        }
    }

    @Test
    public void tableNumberSort() {
        initUnsortedTable();

        // sort by number
        tm.sort(BAR, SortDir.ASC, null, null);

        // verify results
        rows = tm.getRows();
        int nr = rows.length;
        assertEquals("row count", NUMBERS.length, nr);
        for (int i = 0; i < nr; i++) {
            assertEquals(UNEX_SORT + i, SORTED_NUMBERS[i], rows[i].get(BAR));
        }

        // now the other way
        tm.sort(BAR, SortDir.DESC, null, null);

        // verify results
        rows = tm.getRows();
        nr = rows.length;
        assertEquals("row count", NUMBERS.length, nr);
        for (int i = 0; i < nr; i++) {
            assertEquals(UNEX_SORT + i,
                         SORTED_NUMBERS[nr - 1 - i], rows[i].get(BAR));
        }
    }

    @Test
    public void sortAndFormat() {
        initUnsortedTable();

        // set hex formatter
        tm.setFormatter(BAR, HexFormatter.INSTANCE);

        // sort by number
        tm.sort(BAR, SortDir.ASC, null, null);

        // verify results
        rows = tm.getRows();
        int nr = rows.length;
        assertEquals("row count", SORTED_HEX.length, nr);
        for (int i = 0; i < nr; i++) {
            assertEquals(UNEX_SORT + i, SORTED_HEX[i], rows[i].getAsString(BAR));
        }
    }

    private static final String[][] SORTED_NAMES_AND_HEX = {
            {ELEVEN, "0xb"},
            {FOUR, "0x4"},
            {ONE, "0x1"},
            {THIRTY, "0x1e"},
            {THREE, "0x3"},
            {TWELVE, "0xc"},
            {TWENTY, "0x14"},
            {TWO, "0x2"},
    };

    @Test
    public void sortAndFormatTwo() {
        initUnsortedTable();
        tm.setFormatter(BAR, HexFormatter.INSTANCE);
        tm.sort(FOO, SortDir.ASC, null, null);
        rows = tm.getRows();
        int nr = rows.length;
        for (int i = 0; i < nr; i++) {
            String[] exp = SORTED_NAMES_AND_HEX[i];
            String[] act = rows[i].getAsFormattedStrings();
            assertArrayEquals(UNEX_SORT + i, exp, act);
        }
    }

    private static final String[] FBZ = {FOO, BAR, ZOO};

    @Test
    public void getColumnIds() {
        tm = new TableModel(FOO, BAR, ZOO);
        assertArrayEquals("col IDs", FBZ, tm.getColumnIds());
    }

    @Test
    public void sortDirAsc() {
        assertEquals("asc sort dir", SortDir.ASC, TableModel.sortDir("asc"));
    }

    @Test
    public void sortDirDesc() {
        assertEquals("desc sort dir", SortDir.DESC, TableModel.sortDir("desc"));
    }

    @Test
    public void sortDirOther() {
        assertEquals("other sort dir", SortDir.ASC, TableModel.sortDir("other"));
    }

    @Test
    public void sortDirNull() {
        assertEquals("null sort dir", SortDir.ASC, TableModel.sortDir(null));
    }

    @Test
    public void enumSort() {
        tm = new TableModel(FOO);
        tm.addRow().cell(FOO, StarWars.HAN_SOLO);
        tm.addRow().cell(FOO, StarWars.C3PO);
        tm.addRow().cell(FOO, StarWars.JABBA_THE_HUTT);
        tm.addRow().cell(FOO, StarWars.LEIA_ORGANA);
        tm.addRow().cell(FOO, StarWars.R2D2);
        tm.addRow().cell(FOO, StarWars.LUKE_SKYWALKER);

        tm.sort(FOO, SortDir.ASC, null, null);

        // verify expected results
        StarWars[] ordered = StarWars.values();
        TableModel.Row[] rows = tm.getRows();
        assertEquals("wrong length?", ordered.length, rows.length);
        int nr = rows.length;
        for (int i = 0; i < nr; i++) {
            assertEquals(UNEX_SORT + i, ordered[i], rows[i].get(FOO));
        }
    }


    // ------------------------
    // Second sort column tests

    private static final String A1 = "a1";
    private static final String A2 = "a2";
    private static final String A3 = "a3";
    private static final String B1 = "b1";
    private static final String B2 = "b2";
    private static final String B3 = "b3";
    private static final String C1 = "c1";
    private static final String C2 = "c2";
    private static final String C3 = "c3";
    private static final String A = "A";
    private static final String B = "B";
    private static final String C = "C";

    private static final String[] UNSORTED_IDS = {
            A3, B2, A1, C2, A2, C3, B1, C1, B3
    };
    private static final String[] UNSORTED_ALPHAS = {
            A, B, A, C, A, C, B, C, B
    };
    private static final int[] UNSORTED_NUMBERS = {
            3, 2, 1, 2, 2, 3, 1, 1, 3
    };

    private static final String[] ROW_ORDER_AA_NA = {
            A1, A2, A3, B1, B2, B3, C1, C2, C3
    };
    private static final String[] ROW_ORDER_AD_NA = {
            C1, C2, C3, B1, B2, B3, A1, A2, A3
    };
    private static final String[] ROW_ORDER_AA_ND = {
            A3, A2, A1, B3, B2, B1, C3, C2, C1
    };
    private static final String[] ROW_ORDER_AD_ND = {
            C3, C2, C1, B3, B2, B1, A3, A2, A1
    };

    private void testAddRow(TableModel tm, int index) {
        tm.addRow().cell(ID, UNSORTED_IDS[index])
                .cell(ALPHA, UNSORTED_ALPHAS[index])
                .cell(NUMBER, UNSORTED_NUMBERS[index]);
    }

    private TableModel unsortedDoubleTableModel() {
        tm = new TableModel(ID, ALPHA, NUMBER);
        for (int i = 0; i < 9; i++) {
            testAddRow(tm, i);
        }
        return tm;
    }

    private void verifyRowOrder(String tag, TableModel tm, String[] rowOrder) {
        int i = 0;
        for (TableModel.Row row : tm.getRows()) {
            assertEquals(tag + ": unexpected row id", rowOrder[i++], row.get(ID));
        }
    }

    @Test
    public void sortAlphaAscNumberAsc() {
        tm = unsortedDoubleTableModel();
        verifyRowOrder("unsorted", tm, UNSORTED_IDS);
        tm.sort(ALPHA, SortDir.ASC, NUMBER, SortDir.ASC);
        verifyRowOrder("aana", tm, ROW_ORDER_AA_NA);
    }

    @Test
    public void sortAlphaDescNumberAsc() {
        tm = unsortedDoubleTableModel();
        verifyRowOrder("unsorted", tm, UNSORTED_IDS);
        tm.sort(ALPHA, SortDir.DESC, NUMBER, SortDir.ASC);
        verifyRowOrder("adna", tm, ROW_ORDER_AD_NA);
    }

    @Test
    public void sortAlphaAscNumberDesc() {
        tm = unsortedDoubleTableModel();
        verifyRowOrder("unsorted", tm, UNSORTED_IDS);
        tm.sort(ALPHA, SortDir.ASC, NUMBER, SortDir.DESC);
        verifyRowOrder("aand", tm, ROW_ORDER_AA_ND);
    }

    @Test
    public void sortAlphaDescNumberDesc() {
        tm = unsortedDoubleTableModel();
        verifyRowOrder("unsorted", tm, UNSORTED_IDS);
        tm.sort(ALPHA, SortDir.DESC, NUMBER, SortDir.DESC);
        verifyRowOrder("adnd", tm, ROW_ORDER_AD_ND);
    }

    // ----------------
    // Annotation tests

    @Test
    public void stringAnnotation() {
        tm = new TableModel(FOO);
        tm.addAnnotation(BAR, ZOO);
        Collection<TableModel.Annot> annots = tm.getAnnotations();
        assertEquals("wrong size", 1, annots.size());

        TableModel.Annot annot = annots.iterator().next();
        assertEquals("wrong key", BAR, annot.key());
        assertEquals("wrong value", ZOO, annot.value());
    }

    private static final String K_INT = "int";
    private static final String K_BOOL = "bool";
    private static final String K_FLOAT = "float";
    private static final String K_DOUBLE = "double";
    private static final String K_ENUM = "enum";

    private TableModel.Annot getAnnotation(Collection<TableModel.Annot> annots, String key) {
        final TableModel.Annot[] annot = {null};
        annots.forEach(a -> {
            if (a.key().equals(key)) {
                annot[0] = a;
            }
        });
        return annot[0];
    }

    private void verifyCollectionContains(Collection<TableModel.Annot> annots,
                                          String key, int i) {
        TableModel.Annot a = getAnnotation(annots, key);
        assertEquals("wrong int value", i, a.value());
    }

    private void verifyCollectionContains(Collection<TableModel.Annot> annots,
                                          String key, boolean b) {
        TableModel.Annot a = getAnnotation(annots, key);
        assertEquals("wrong boolean value", b, a.value());
    }

    private void verifyCollectionContains(Collection<TableModel.Annot> annots,
                                          String key, float f) {
        TableModel.Annot a = getAnnotation(annots, key);
        assertEquals("wrong float value", f, a.value());
    }

    private void verifyCollectionContains(Collection<TableModel.Annot> annots,
                                          String key, double d) {
        TableModel.Annot a = getAnnotation(annots, key);
        assertEquals("wrong double value", d, a.value());
    }

    private void verifyCollectionContains(Collection<TableModel.Annot> annots,
                                          String key, Enum<?> e) {
        TableModel.Annot a = getAnnotation(annots, key);
        assertEquals("wrong double value", e, a.value());
    }

    @Test
    public void primitivesAnnotation() {
        tm = new TableModel(FOO);
        tm.addAnnotation(K_INT, 1);
        tm.addAnnotation(K_BOOL, true);
        tm.addAnnotation(K_FLOAT, 3.14f);
        tm.addAnnotation(K_DOUBLE, 2.71828);
        tm.addAnnotation(K_ENUM, StarWars.LUKE_SKYWALKER);

        Collection<TableModel.Annot> annots = tm.getAnnotations();
        assertEquals("wrong size", 5, annots.size());

        verifyCollectionContains(annots, K_INT, 1);
        verifyCollectionContains(annots, K_BOOL, true);
        verifyCollectionContains(annots, K_FLOAT, 3.14f);
        verifyCollectionContains(annots, K_DOUBLE, 2.71828);
        verifyCollectionContains(annots, K_ENUM, StarWars.LUKE_SKYWALKER);
    }

    // TODO: add support for compound object value
}
