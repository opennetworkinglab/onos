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

package org.onosproject.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;


/**
 * Provides a partial implementation of {@link TableRow}.
 */
public abstract class AbstractTableRow implements TableRow {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, String> data = new HashMap<>();

    @Override
    public String get(String key) {
        return data.get(key);
    }

    @Override
    public ObjectNode toJsonNode() {
        ObjectNode result = MAPPER.createObjectNode();
        for (String id : columnIds()) {
            result.put(id, data.get(id));
        }
        return result;
    }

    /**
     * Subclasses must provide the list of column IDs.
     *
     * @return array of column IDs
     */
    protected abstract String[] columnIds();

    /**
     * Add a column ID to value binding.
     *
     * @param id the column ID
     * @param value the cell value
     */
    protected void add(String id, String value) {
        data.put(id, value);
    }
}
