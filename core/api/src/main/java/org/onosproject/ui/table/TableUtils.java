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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.ui.JsonUtils;

/**
 * Provides static utility methods for dealing with tables.
 */
public final class TableUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // non-instantiable
    private TableUtils() { }

    /**
     * Produces a JSON array node from the specified table rows.
     *
     * @param rows table rows
     * @return JSON array
     */
    public static ArrayNode generateArrayNode(TableRow[] rows) {
        ArrayNode array = MAPPER.createArrayNode();
        for (TableRow r : rows) {
            array.add(r.toJsonNode());
        }
        return array;
    }

    /**
     * Creates a row comparator for the given request. The ID of the column
     * to sort on is the payload's "sortCol" property (defaults to "id").
     * The direction for the sort is the payload's "sortDir" property
     * (defaults to "asc").
     *
     * @param payload the event payload
     * @return a row comparator
     */
    public static RowComparator createRowComparator(ObjectNode payload) {
        return createRowComparator(payload, "id");
    }

    /**
     * Creates a row comparator for the given request. The ID of the column to
     * sort on is the payload's "sortCol" property (or the specified default).
     * The direction for the sort is the payload's "sortDir" property
     * (defaults to "asc").
     *
     * @param payload the event payload
     * @param defColId the default column ID
     * @return a row comparator
     */
    public static RowComparator createRowComparator(ObjectNode payload,
                                                    String defColId) {
        String sortCol = JsonUtils.string(payload, "sortCol", defColId);
        String sortDir = JsonUtils.string(payload, "sortDir", "asc");
        return new RowComparator(sortCol, RowComparator.direction(sortDir));
    }
}
