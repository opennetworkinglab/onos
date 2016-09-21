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

/**
 * Formats integer values as hex strings with a "0x" prefix.
 */
public final class HexFormatter extends AbstractCellFormatter {

    // non-instantiable
    private HexFormatter() { }

    @Override
    protected String nonNullFormat(Object value) {
        return OX + Integer.toHexString((Integer) value);
    }

    /**
     * An instance of this class.
     */
    public static final CellFormatter INSTANCE = new HexFormatter();
}
