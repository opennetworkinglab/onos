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

import org.onosproject.ui.table.CellFormatter;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Formats number using the specified format string.
 */
public final class NumberFormatter extends AbstractCellFormatter {

    private static final String FMT_INTEGER = "#,##0";
    private static final String FMT_5DP = "#,##0.00000";


    private final NumberFormat format;

    /**
     * Creates a formatter using the default format (no decimal places).
     * For example
     * <pre>
     *     12345 formatted as "12,345"
     * </pre>
     */
    public NumberFormatter() {
        this(FMT_INTEGER);
    }

    /**
     * Creates a formatter using a {@link DecimalFormat} configured with the
     * given format string.
     *
     * @param decimalFormat the format string
     */
    public NumberFormatter(String decimalFormat) {
        this(new DecimalFormat(decimalFormat));
    }

    /**
     * Creates a formatter using the specified {@link NumberFormat}.
     *
     * @param format number format
     */
    public NumberFormatter(NumberFormat format) {
        this.format = format;
    }

    @Override
    protected String nonNullFormat(Object value) {
        return format.format(value);
    }

    /**
     * An instance of this class that formats as integers (no decimal places).
     * For example
     * <pre>
     *     12345 formatted as "12,345"
     * </pre>
     */
    public static final CellFormatter INTEGER = new NumberFormatter();

    /**
     * An instance of this class that formats to 5 decimal places.
     * For example
     * <pre>
     *     12.3 formatted as "12.30000"
     *     1234 formatted as "1,234.00000"
     * </pre>
     */
    public static final CellFormatter TO_5DP = new NumberFormatter(FMT_5DP);

}
