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
package org.onosproject.ovsdb.controller;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 * The class representing a database data.
 */
public class OvsdbTableStore {

    private final ConcurrentMap<String, OvsdbRowStore> tableStore = Maps.newConcurrentMap();

    /**
     * Gets the ovsdbRowStore.
     *
     * @param tableName a ovsdb table name
     * @return OvsdbRowStore the data of the table
     */
    public OvsdbRowStore getRows(String tableName) {
        return tableStore.get(tableName);
    }

    /**
     * Create or update a value to tableStore.
     *
     * @param tableName key of the tableName
     * @param rowStore a row of the table
     */
    public void createOrUpdateTable(String tableName, OvsdbRowStore rowStore) {
        tableStore.put(tableName, rowStore);
    }

    /**
     * Drops a value to table data.
     *
     * @param tableName key of the tableName
     */
    public void dropTable(String tableName) {
        tableStore.remove(tableName);
    }

    /**
     * Gets the tableStore.
     *
     * @return tableStore
     */
    public ConcurrentMap<String, OvsdbRowStore> getTableStore() {
        return tableStore;
    }

}
