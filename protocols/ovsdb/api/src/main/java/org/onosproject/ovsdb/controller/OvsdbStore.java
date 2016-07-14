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
package org.onosproject.ovsdb.controller;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 * The cache for local ovsdb database.
 */
public class OvsdbStore {

    private final ConcurrentMap<String, OvsdbTableStore> ovsdbStore = Maps.newConcurrentMap();

    /**
     * Gets the OvsdbTableStore.
     *
     * @param dbName ovsdb database name
     * @return tableStore OvsdbTableStore
     */
    public OvsdbTableStore getOvsdbTableStore(String dbName) {
        OvsdbTableStore tableStore = ovsdbStore.get(dbName);
        if (tableStore == null) {
            return null;
        }
        return tableStore;
    }

    /**
     * Create or Update a value to ovsdbStore.
     *
     * @param dbName ovsdb database name
     * @param tableStore a database tableStore.
     */
    public void createOrUpdateOvsdbStore(String dbName, OvsdbTableStore tableStore) {
        ovsdbStore.put(dbName, tableStore);
    }

    /**
     * Drops a value to rowStore.
     *
     * @param dbName ovsdb database name
     */
    public void dropOvsdbStore(String dbName) {
        ovsdbStore.remove(dbName);
    }

    /**
     * Gets the ovsdbStore.
     *
     * @return ovsdbStore
     */
    public ConcurrentMap<String, OvsdbTableStore> getOvsdbStore() {
        return ovsdbStore;
    }

}
