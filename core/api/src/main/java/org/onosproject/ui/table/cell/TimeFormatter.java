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
 *
 */

package org.onosproject.ui.table.cell;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onosproject.ui.table.CellFormatter;

/**
 * Formats time values using {@link DateTimeFormatter}.
 */
public class TimeFormatter extends AbstractCellFormatter {

    private static final DateTimeFormatter DTF = DateTimeFormat.longTime();

    @Override
    protected String nonNullFormat(Object value) {
        return DTF.print((DateTime) value);
    }

    /**
     * An instance of this class.
     */
    public static final CellFormatter INSTANCE = new TimeFormatter();
}
