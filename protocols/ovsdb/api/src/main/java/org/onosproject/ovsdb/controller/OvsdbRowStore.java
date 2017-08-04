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
package org.onosproject.ovsdb.controller;

import java.util.concurrent.ConcurrentMap;

import org.onosproject.ovsdb.rfc.notation.Row;

import com.google.common.collect.Maps;

/**
 * The class representing a table data.
 */
public class OvsdbRowStore {

    private final ConcurrentMap<String, Row> rowStore = Maps.newConcurrentMap();

    /**
     * Gets the row.
     *
     * @param uuid the key of the rowStore
     * @return row the row of the rowStore
     */
    public Row getRow(String uuid) {
        return rowStore.get(uuid);
    }

    /**
     * Inserts a row to rowStore.
     *
     * @param uuid key of the row
     * @param row a row of the table
     */
    public void insertRow(String uuid, Row row) {
        rowStore.put(uuid, row);
    }

    /**
     * Deletes a row to rowStore.
     *
     * @param uuid key of the row
     */
    public void deleteRow(String uuid) {
        rowStore.remove(uuid);
    }

    /**
     * Gets the rowStore.
     *
     * @return rowStore
     */
    public ConcurrentMap<String, Row> getRowStore() {
        return rowStore;
    }

}
