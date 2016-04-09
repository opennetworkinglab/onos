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

/**
 * Defines a formatter for cell values.
 */
public interface CellFormatter {

    /**
     * Formats the specified value into a string appropriate for displaying
     * in a table cell. Note that null values are acceptable, and will result
     * in the empty string.
     *
     * @param value the value
     * @return the formatted string
     */
    String format(Object value);
}
