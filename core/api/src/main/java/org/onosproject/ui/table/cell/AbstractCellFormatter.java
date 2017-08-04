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

import org.onosproject.ui.table.CellFormatter;

/**
 * Base implementation of a {@link CellFormatter}. This class takes care of
 * dealing with null inputs; subclasses should implement their format method
 * knowing that the input is guaranteed to be non-null.
 */
public abstract class AbstractCellFormatter implements CellFormatter {

    protected static final String EMPTY = "";
    protected static final String SLASH = "/";
    protected static final String QUERY = "?";
    protected static final String UNDERSCORE = "_";
    protected static final String SPACE = " ";
    protected static final String OX = "0x";


    @Override
    public String format(Object value) {
        return value == null ? EMPTY : nonNullFormat(value);
    }

    /**
     * Formats the specified value into a string appropriate for displaying
     * in a table cell. Note that value is guaranteed to be non-null.
     *
     * @param value the value
     * @return the formatted string
     */
    protected abstract String nonNullFormat(Object value);
}
