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


import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Defines a table row abstraction to support sortable tables on the GUI.
 */
@Deprecated
public interface TableRow {

    // TODO: Define TableCell interface and return that, rather than String
    // The hope is that this will allow us to write a generic mechanism for
    // selecting a comparator based on the cell type for the column, to be
    // used for sorting the table rows.
    /**
     * Returns the value of the cell for the given column ID.
     *
     * @param key the column ID
     * @return the cell value
     */
    String get(String key);

    /**
     * Returns this table row in the form of a JSON object.
     *
     * @return the JSON node
     */
    ObjectNode toJsonNode();
}
