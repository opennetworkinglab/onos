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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Provides static utility methods for dealing with tables.
 */
public final class TableUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // non-instantiable
    private TableUtils() { }

    /**
     * Generates a JSON array node from the rows of the given table model.
     *
     * @param tm the table model
     * @return the array node representation of rows
     */
    public static ArrayNode generateRowArrayNode(TableModel tm) {
        ArrayNode array = MAPPER.createArrayNode();
        for (TableModel.Row r : tm.getRows()) {
            array.add(toJsonNode(r, tm));
        }
        return array;
    }

    /**
     * Generates a JSON object node from the annotations of the given table model.
     *
     * @param tm the table model
     * @return the object node representation of the annotations
     */
    public static ObjectNode generateAnnotObjectNode(TableModel tm) {
        ObjectNode node = MAPPER.createObjectNode();
        for (TableModel.Annot a : tm.getAnnotations()) {
            node.put(a.key(), a.valueAsString());
        }
        return node;
    }

    private static JsonNode toJsonNode(TableModel.Row row, TableModel tm) {
        ObjectNode result = MAPPER.createObjectNode();
        String[] keys = tm.getColumnIds();
        String[] cells = row.getAsFormattedStrings();
        int n = keys.length;
        for (int i = 0; i < n; i++) {
            result.put(keys[i], cells[i]);
        }
        return result;
    }
}
