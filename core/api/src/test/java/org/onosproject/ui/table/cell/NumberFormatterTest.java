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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onosproject.ui.table.CellFormatter;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link NumberFormatter}.
 */
public class NumberFormatterTest {


    private CellFormatter f5dp = NumberFormatter.TO_5DP;
    private CellFormatter fInt = NumberFormatter.INTEGER;

    private static Locale systemLocale;

    @BeforeClass
    public static void classSetup() {
        systemLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterClass
    public static void classTeardown() {
        Locale.setDefault(systemLocale);
    }

    @Test
    public void defaultNullValue() {
        assertEquals("default null value", "", f5dp.format(null));
    }

    @Test
    public void defaultZero() {
        assertEquals("default zero", "0.00000", f5dp.format(0));
    }

    @Test
    public void defaultFifty() {
        assertEquals("default fifty", "50.00000", f5dp.format(50));
    }

    @Test
    public void default2G() {
        assertEquals("default 2G", "2,000.00000", f5dp.format(2000));
    }

    @Test
    public void integerNullValue() {
        assertEquals("integer null value", "", fInt.format(null));
    }

    @Test
    public void integerZero() {
        assertEquals("integer zero", "0", fInt.format(0));
    }

    @Test
    public void integerFifty() {
        assertEquals("integer fifty", "50", fInt.format(50));
    }

    @Test
    public void integer2G() {
        assertEquals("integer 2G", "2,000", fInt.format(2000));
    }

    @Test
    public void integer5M() {
        assertEquals("integer 5M", "5,000,042", fInt.format(5000042));
    }

}
