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

import static org.apache.commons.lang.WordUtils.capitalizeFully;

/**
 * Formats enum types to be readable strings.
 */
public final class EnumFormatter extends AbstractCellFormatter {

    // non-instantiable
    private EnumFormatter() { }

    @Override
    protected String nonNullFormat(Object value) {
        return capitalizeFully(value.toString().replace(UNDERSCORE, SPACE));
    }

    /**
     * An instance of this class.
     */
    public static final CellFormatter INSTANCE = new EnumFormatter();
}
