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

package org.onosproject.ui.table.cell;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Formats number using the specified format string".
 */
public final class NumberFormatter extends AbstractCellFormatter {

    private final NumberFormat format;

    /**
     * Creates a formatter using a default decimal format.
     */
    public NumberFormatter() {
        this(new DecimalFormat("#,##0.00000"));
    }

    /**
     * Creates a formatter using the specified format.
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

}
